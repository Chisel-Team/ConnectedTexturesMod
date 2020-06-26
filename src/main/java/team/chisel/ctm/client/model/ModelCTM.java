package team.chisel.ctm.client.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.val;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.BlockPartFace;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IModelTransform;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelConfiguration;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;
import team.chisel.ctm.client.texture.render.TextureNormal;
import team.chisel.ctm.client.texture.type.TextureTypeNormal;
import team.chisel.ctm.client.util.BlockRenderLayer;
import team.chisel.ctm.client.util.CTMPackReloadListener;
import team.chisel.ctm.client.util.ResourceUtil;

public class ModelCTM implements IModelCTM {
    
    private final IUnbakedModel vanillamodel;
    private final @Nullable BlockModel modelinfo;

    // Populated from overrides data during construction
    private final Int2ObjectMap<JsonElement> overrides;
    protected final Int2ObjectMap<IMetadataSectionCTM> metaOverrides = new Int2ObjectArrayMap<>();
    
    // Populated during bake with real texture data
    protected Int2ObjectMap<TextureAtlasSprite> spriteOverrides;
    protected Map<Pair<Integer, String>, ICTMTexture<?>> textureOverrides;

    private final Collection<ResourceLocation> textureDependencies;
    
    private transient byte layers;

    private Map<ResourceLocation, ICTMTexture<?>> textures = new HashMap<>();
    
    public ModelCTM(IUnbakedModel modelinfo) {
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
                meta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(rl));
                textureDependencies.add(rl);
            } else if (e.getValue().isJsonObject()) {
                JsonObject obj = e.getValue().getAsJsonObject();
                if (!obj.has("ctm_version")) {
                    // This model can only be version 1, TODO improve this
                    obj.add("ctm_version", new JsonPrimitive(1));
                }
                meta = new IMetadataSectionCTM.Serializer().deserialize(obj);
            }
            if (meta != null ) {
                metaOverrides.put(e.getIntKey(), meta);
                textureDependencies.addAll(Arrays.asList(meta.getAdditionalTextures()));
            }
        }
        
        this.textureDependencies.removeIf(rl -> rl.getPath().startsWith("#"));
	}

	@Override
	public Collection<RenderMaterial> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
		List<RenderMaterial> ret = textureDependencies.stream()
				.map(rl -> new RenderMaterial(AtlasTexture.LOCATION_BLOCKS_TEXTURE, rl))
    			.collect(Collectors.toList());
    	ret.addAll(vanillamodel.getTextures(modelGetter, missingTextureErrors));
        // Validate all texture metadata
    	for (RenderMaterial tex : ret) {
            IMetadataSectionCTM meta;
			try {
				meta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(tex.getTextureLocation()));
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
	public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ItemOverrideList itemOverrides, ResourceLocation modelLocation) {
		return bake(bakery, spriteGetter, modelTransform, modelLocation);
	}
	
	public IBakedModel bake(ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ResourceLocation modelLocation) {
        IBakedModel parent = vanillamodel.bakeModel(bakery, spriteGetter, modelTransform, modelLocation);
        initializeTextures(bakery, spriteGetter);
        return new ModelBakedCTM(this, parent);
    }
	
	public void initializeTextures(ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter) {
		for (RenderMaterial m : getTextures(null, bakery::getUnbakedModel, new HashSet<>())) {
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
        if (spriteOverrides == null) {
            spriteOverrides = new Int2ObjectArrayMap<>();
            // Convert all primitive values into sprites
            for (Int2ObjectMap.Entry<JsonElement> e : overrides.int2ObjectEntrySet()) {
                if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isString()) {
                    TextureAtlasSprite sprite = spriteGetter.apply(new RenderMaterial(AtlasTexture.LOCATION_BLOCKS_TEXTURE, new ResourceLocation(e.getValue().getAsString())));
                    spriteOverrides.put(e.getIntKey(), sprite);
                }
            }
        }
        if (textureOverrides == null) {
            textureOverrides = new HashMap<>();
            for (Int2ObjectMap.Entry<IMetadataSectionCTM> e : metaOverrides.int2ObjectEntrySet()) {
                List<BlockPartFace> matches = modelinfo.getElements().stream().flatMap(b -> b.mapFaces.values().stream()).filter(b -> b.tintIndex == e.getIntKey()).collect(Collectors.toList());
                Multimap<RenderMaterial, BlockPartFace> bySprite = HashMultimap.create();
                // TODO 1.15 this isn't right
                matches.forEach(part -> bySprite.put(modelinfo.textures.getOrDefault(part.texture.substring(1), Either.right(part.texture)).left().get(), part));
                for (val e2 : bySprite.asMap().entrySet()) {
                    ResourceLocation texLoc = e2.getKey().getTextureLocation();
                    TextureAtlasSprite sprite = getOverrideSprite(e.getIntKey());
                    if (sprite == null) {
                    	sprite = spriteGetter.apply(new RenderMaterial(AtlasTexture.LOCATION_BLOCKS_TEXTURE, texLoc));
                    }
                    ICTMTexture<?> tex = e.getValue().makeTexture(sprite, spriteGetter);
                    layers |= 1 << (tex.getLayer() == null ? 7 : tex.getLayer().ordinal());
                    textureOverrides.put(Pair.of(e.getIntKey(), texLoc.toString()), tex);
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
