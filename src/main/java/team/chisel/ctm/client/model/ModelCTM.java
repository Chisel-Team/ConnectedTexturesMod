package team.chisel.ctm.client.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.IModelConfiguration;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.BlockModelExtension;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;
import team.chisel.ctm.client.texture.render.TextureNormal;
import team.chisel.ctm.client.texture.type.TextureTypeNormal;
import team.chisel.ctm.client.util.BlockRenderLayer;
import team.chisel.ctm.client.util.CTMPackReloadListener;
import team.chisel.ctm.client.util.ResourceUtil;

public class ModelCTM implements IModelCTM {
    
    private final UnbakedModel vanillamodel;
    private final @Nullable BlockModel modelinfo;

    // Populated from overrides data during construction
    private final Int2ObjectMap<JsonElement> overrides;
    protected Int2ObjectMap<IMetadataSectionCTM> metaOverrides = new Int2ObjectArrayMap<>();
    
    // Populated during bake with real texture data
    protected Int2ObjectMap<TextureAtlasSprite> spriteOverrides;
    protected Map<Pair<Integer, ResourceLocation>, ICTMTexture<?>> textureOverrides;

    private final Collection<String> textureDependencies;
    
    private transient byte layers;

    private Map<ResourceLocation, ICTMTexture<?>> textures = new HashMap<>();
    
    public ModelCTM(UnbakedModel modelinfo) {
        this.vanillamodel = modelinfo;
        this.overrides = new Int2ObjectOpenHashMap<>();
        this.textureDependencies = new HashSet<>();
        BlockModel temp = null;

        if (modelinfo instanceof BlockModel blockmodel && blockmodel instanceof BlockModelExtension extension) {
            temp = blockmodel;
            metaOverrides = extension.getMetaOverrides();
            for (IMetadataSectionCTM meta: metaOverrides.values()) {
                textureDependencies.addAll(Arrays.asList(meta.getAdditionalTextures()));
            }
            this.textureDependencies.removeIf(rl -> rl.contains("#"));
        }
        this.modelinfo = temp;
    }
    
    public ModelCTM(BlockModel modelinfo, Int2ObjectMap<JsonElement> overrides) throws IOException {
    	this.vanillamodel = modelinfo;
    	this.modelinfo = modelinfo;
    	this.overrides = overrides;
        this.textureDependencies = new HashSet<>();
        for (Int2ObjectMap.Entry<JsonElement> e : this.overrides.int2ObjectEntrySet()) {
            IMetadataSectionCTM meta = null;
            if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isString()) { //TODO is this still a thing?
                ResourceLocation rl = new ResourceLocation(e.getValue().getAsString());
                meta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(rl));
                textureDependencies.add(rl.toString());
            } else if (e.getValue().isJsonObject()) {
                JsonObject obj = e.getValue().getAsJsonObject();
                if (!obj.has("ctm_version")) {
                    // This model can only be version 1, TODO improve this
                    obj.add("ctm_version", new JsonPrimitive(1));
                }
                meta = new IMetadataSectionCTM.Serializer().fromJson(obj);
            }
            if (meta != null ) {
                metaOverrides.put(e.getIntKey(), meta);
                textureDependencies.addAll(Arrays.asList(meta.getAdditionalTextures()));
            }
        }
        
        this.textureDependencies.removeIf(rl -> rl.contains("#"));
	}

	@Override
	public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
		List<Material> ret = textureDependencies.stream()
				.map(rl -> new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation(rl)))
    			.collect(Collectors.toList());
        ret.addAll(vanillamodel.getMaterials(modelGetter, missingTextureErrors));
        // Validate all texture metadata
    	for (Material tex : ret) {
            IMetadataSectionCTM meta;
			try {
				meta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(tex.texture()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
            if (meta != null) {
                if (meta.getType().requiredTextures() != meta.getAdditionalTextures().length + 1) {
                    throw new IllegalArgumentException(String.format("Texture type %s requires exactly %d textures. %d were provided.", meta.getType(), meta.getType().requiredTextures(), meta.getAdditionalTextures().length + 1));
                }
            }
    	}
    	return ret;
    }
	
	@Override
	public BakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides itemOverrides, ResourceLocation modelLocation) {
       return bake(bakery, spriteGetter, modelTransform, modelLocation);
	}
	
	private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();

	public BakedModel bake(ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ResourceLocation modelLocation) {
        BakedModel parent;
        if (modelinfo != null && modelinfo.getRootModel() == ModelBakery.GENERATION_MARKER) { // Apply same special case that ModelBakery does
            return ITEM_MODEL_GENERATOR.generateBlockModel(spriteGetter, modelinfo).bake(bakery, modelinfo, spriteGetter, modelTransform, modelLocation, false);
        } else {
            parent = vanillamodel.bake(bakery, spriteGetter, modelTransform, modelLocation);
        }
        initializeTextures(bakery, spriteGetter);
        return new ModelBakedCTM(this, parent);
    }
	
	public void initializeTextures(ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter) {
		for (Material m : getTextures(null, bakery::getModel, new HashSet<>())) {
		    TextureAtlasSprite sprite = spriteGetter.apply(m);
		    IMetadataSectionCTM chiselmeta = null;
		    try {
		        chiselmeta = ResourceUtil.getMetadata(sprite);
		    } catch (IOException e) {}
		    final IMetadataSectionCTM meta = chiselmeta;
		    textures.computeIfAbsent(sprite.getName(), s -> {
		        ICTMTexture<?> tex;
		        if (meta == null) {
		            tex = new TextureNormal(TextureTypeNormal.INSTANCE, new TextureInfo(new TextureAtlasSprite[] { sprite }, Optional.empty(), null));
		        } else {
		            tex = meta.makeTexture(sprite, spriteGetter);
		        }
		        layers |= 1 << (tex.getLayer() == null ? 7 : tex.getLayer().ordinal());
		        return tex;
		    });
		}
        if (spriteOverrides == null) { //TODO is this a thing?
            spriteOverrides = new Int2ObjectArrayMap<>();
            // Convert all primitive values into sprites
            for (Int2ObjectMap.Entry<JsonElement> e : overrides.int2ObjectEntrySet()) {
                if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isString()) {
                    TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation(e.getValue().getAsString())));
                    spriteOverrides.put(e.getIntKey(), sprite);
                }
            }
        }
        if (textureOverrides == null) {
            textureOverrides = new HashMap<>();
            for (Int2ObjectMap.Entry<IMetadataSectionCTM> e : metaOverrides.int2ObjectEntrySet()) {
                List<BlockElementFace> matches = modelinfo.getElements().stream().flatMap(b -> b.faces.values().stream()).filter(b -> b.tintIndex == e.getIntKey()).toList();
                Multimap<Material, BlockElementFace> bySprite = HashMultimap.create();
                // TODO 1.15 this isn't right
                matches.forEach(part -> bySprite.put(modelinfo.getMaterial(part.texture), part));
                for (var e2 : bySprite.asMap().entrySet()) {
                    ResourceLocation texLoc = e2.getKey().texture();
                    TextureAtlasSprite sprite = getOverrideSprite(e.getIntKey());
                    if (sprite == null) {
                    	sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, texLoc));
                    }
                    ICTMTexture<?> tex = e.getValue().makeTexture(sprite, spriteGetter);
                    layers |= 1 << (tex.getLayer() == null ? 7 : tex.getLayer().ordinal());
                    for (String s: e.getValue().getAdditionalTextures()) {
                        TextureAtlasSprite atlasSprite = spriteGetter.apply(modelinfo.getMaterial(s));
                        tex.addSprite(atlasSprite);
                    }
                    textureOverrides.put(Pair.of(e.getIntKey(), texLoc), tex);
                }
            }
        }
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
    public boolean canRenderInLayer(BlockState state, RenderType layer) {
        // sign bit is used to signify that a layer-less (vanilla) texture is present
        return (layers < 0 && CTMPackReloadListener.canRenderInLayerFallback(state, layer)) || ((layers >> BlockRenderLayer.fromType(layer).ordinal()) & 1) == 1;
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
}
