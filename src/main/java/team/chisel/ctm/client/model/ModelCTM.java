package team.chisel.ctm.client.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;
import team.chisel.ctm.client.texture.render.TextureNormal;
import team.chisel.ctm.client.texture.type.TextureTypeNormal;
import team.chisel.ctm.client.util.BlockRenderLayer;
import team.chisel.ctm.client.util.ResourceUtil;

public class ModelCTM implements IModelCTM {
    
    private final UnbakedModel vanillamodel;
    private final @Nullable BlockModel modelinfo;

    // Populated from overrides data during construction
    private final Int2ObjectMap<JsonElement> overrides;
    protected final Int2ObjectMap<IMetadataSectionCTM> metaOverrides = new Int2ObjectArrayMap<>();
    
    // Populated during bake with real texture data
    protected Int2ObjectMap<TextureAtlasSprite> spriteOverrides;
    protected Map<Pair<Integer, ResourceLocation>, ICTMTexture<?>> textureOverrides;

    private final Collection<ResourceLocation> textureDependencies;

    private final Set<RenderType> extraLayers = new HashSet<>();
    private final Set<RenderType> extraLayersView = Collections.unmodifiableSet(extraLayers);

    private Map<ResourceLocation, ICTMTexture<?>> textures = new HashMap<>();
    
    public ModelCTM(UnbakedModel modelinfo) {
        this.vanillamodel = modelinfo;
        this.modelinfo = null;
        this.overrides = new Int2ObjectOpenHashMap<>();
        this.textureDependencies = new HashSet<>();
    }
    
    public ModelCTM(BlockModel modelinfo, Int2ObjectMap<JsonElement> overrides) throws IOException {
    	this.vanillamodel = modelinfo;
    	this.modelinfo = modelinfo;
    	this.overrides = overrides;
        this.textureDependencies = new HashSet<>();
        for (Int2ObjectMap.Entry<JsonElement> e : this.overrides.int2ObjectEntrySet()) {
            IMetadataSectionCTM meta = null;
            if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isString()) {
                ResourceLocation rl = new ResourceLocation(e.getValue().getAsString());
                meta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(rl)).orElse(null); // TODO lazy null
                textureDependencies.add(rl);
            } else if (e.getValue().isJsonObject()) {
                JsonObject obj = e.getValue().getAsJsonObject();
                if (!obj.has("ctm_version")) {
                    // This model can only be version 1, TODO improve this
                    obj.addProperty("ctm_version", 1);
                }
                meta = new IMetadataSectionCTM.Serializer().fromJson(obj);
            }
            if (meta != null ) {
                metaOverrides.put(e.getIntKey(), meta);
                textureDependencies.addAll(Arrays.asList(meta.getAdditionalTextures()));
            }
        }
        
        this.textureDependencies.removeIf(rl -> rl.getPath().startsWith("#"));
	}
	
	@Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
		return bake(bakery, spriteGetter, modelState, modelLocation);
	}

	private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();

	public BakedModel bake(ModelBaker bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ResourceLocation modelLocation) {
        BakedModel parent;
        if (modelinfo != null && modelinfo.getRootModel() == ModelBakery.GENERATION_MARKER) { // Apply same special case that ModelBakery does
            return ITEM_MODEL_GENERATOR.generateBlockModel(spriteGetter, modelinfo).bake(bakery, modelinfo, spriteGetter, modelTransform, modelLocation, false);
        } else {
            initializeOverrides(spriteGetter);
            this.textureDependencies.forEach(t -> initializeTexture(new Material(TextureAtlas.LOCATION_BLOCKS, t), spriteGetter));
            parent = vanillamodel.bake(bakery, mat -> initializeTexture(mat, spriteGetter), modelTransform, modelLocation);
            if (!isInitialized()) {
                this.spriteOverrides = new Int2ObjectOpenHashMap<>();
                this.textureOverrides = new HashMap<>();
            }
        }
        return new ModelBakedCTM(this, parent, null);
    }
	
	public TextureAtlasSprite initializeTexture(Material m, Function<Material, TextureAtlasSprite> spriteGetter) {
	    TextureAtlasSprite sprite = spriteGetter.apply(m);
	    Optional<IMetadataSectionCTM> chiselmeta = Optional.empty();
	    try {
	        chiselmeta = ResourceUtil.getMetadata(sprite);
	    } catch (IOException e) {}
	    final Optional<IMetadataSectionCTM> meta = chiselmeta;
	    textures.computeIfAbsent(sprite.contents().name(), s -> {
	        ICTMTexture<?> tex;
	        if (meta.isEmpty()) {
	            tex = new TextureNormal(TextureTypeNormal.INSTANCE, new TextureInfo(new TextureAtlasSprite[] { sprite }, Optional.empty(), null, false));
	        } else {
	            tex = meta.get().makeTexture(sprite, spriteGetter);
	        }
            BlockRenderLayer renderLayer = tex.getLayer();
            if (renderLayer != null) {
                extraLayers.add(renderLayer.getRenderType());
            }
	        return tex;
	    });
        return sprite;
	}
	
	private void initializeOverrides(Function<Material, TextureAtlasSprite> spriteGetter) {
        if (spriteOverrides == null) {
            spriteOverrides = new Int2ObjectOpenHashMap<>();
            // Convert all primitive values into sprites
            for (Int2ObjectMap.Entry<JsonElement> e : overrides.int2ObjectEntrySet()) {
                if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isString()) {
                    TextureAtlasSprite override = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation(e.getValue().getAsString())));
                    spriteOverrides.put(e.getIntKey(), override);
                }
            }
        }
        if (textureOverrides == null) {
            textureOverrides = new HashMap<>();
            for (Int2ObjectMap.Entry<IMetadataSectionCTM> e : metaOverrides.int2ObjectEntrySet()) {
                List<BlockElementFace> matches = modelinfo.getElements().stream().flatMap(b -> b.faces.values().stream()).filter(b -> b.tintIndex == e.getIntKey()).toList();
                Multimap<Material, BlockElementFace> bySprite = HashMultimap.create();
                // TODO 1.15 this isn't right
                matches.forEach(part -> bySprite.put(modelinfo.textureMap.getOrDefault(part.texture.substring(1), Either.right(part.texture)).left().get(), part));
                for (var e2 : bySprite.asMap().entrySet()) {
                    ResourceLocation texLoc = e2.getKey().sprite().contents().name();
                    TextureAtlasSprite override = getOverrideSprite(e.getIntKey());
                    if (override == null) {
                        override = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, texLoc));
                    }
                    ICTMTexture<?> tex = e.getValue().makeTexture(override, spriteGetter);
                    BlockRenderLayer renderLayer = tex.getLayer();
                    if (renderLayer != null) {
                        extraLayers.add(renderLayer.getRenderType());
                    }
                    textureOverrides.put(Pair.of(e.getIntKey(), texLoc), tex);
                }
            }
        }
	}

	public boolean isInitialized() {
	    return spriteOverrides != null && textureOverrides != null && !textures.isEmpty();
	}

    @Override
    public void load() {}

    @Override
    public Collection<ICTMTexture<?>> getCTMTextures() {
        return ImmutableList.<ICTMTexture<?>>builder().addAll(textures.values()).addAll(textureOverrides.values()).build();
    }
    
    @Override
    public ICTMTexture<?> getTexture(ResourceLocation iconName) {
        return textures.get(iconName);
    }

    @Override
    public Set<RenderType> getExtraLayers(BlockState state) {
        return extraLayersView;
    }

    @Override
    @Nullable
    public TextureAtlasSprite getOverrideSprite(int tintIndex) {
        return spriteOverrides.get(tintIndex);
    }
    
    @Override
    @Nullable
    public ICTMTexture<?> getOverrideTexture(int tintIndex, ResourceLocation sprite) {
        return textureOverrides.get(Pair.of(tintIndex, sprite));
    }

    @Override
    public void resolveParents(@NotNull Function<ResourceLocation, UnbakedModel> modelGetter, @NotNull IGeometryBakingContext context) {
        vanillamodel.resolveParents(modelGetter);
    }
}
