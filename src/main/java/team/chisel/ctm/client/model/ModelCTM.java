package team.chisel.ctm.client.model;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.val;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.animation.IClip;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;
import team.chisel.ctm.client.texture.render.TextureNormal;
import team.chisel.ctm.client.texture.type.TextureTypeNormal;
import team.chisel.ctm.client.util.ResourceUtil;

public class ModelCTM implements IModelCTM {
    
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(IMetadataSectionCTM.class, new IMetadataSectionCTM.Serializer()).create();

    private final ModelBlock modelinfo;
    private IModel vanillamodel;
    private Boolean uvlock;

    // Populated from overrides data during construction
    private final Int2ObjectMap<JsonElement> overrides;
    protected final Int2ObjectMap<IMetadataSectionCTM> metaOverrides = new Int2ObjectArrayMap<>();
    
    // Populated during bake with real texture data
    protected Int2ObjectMap<TextureAtlasSprite> spriteOverrides;
    protected Map<Pair<Integer, String>, ICTMTexture<?>> textureOverrides;

    private final Collection<ResourceLocation> textureDependencies;
    
    private transient byte layers;

    private Map<String, ICTMTexture<?>> textures = new HashMap<>();
    
    public ModelCTM(ModelBlock modelinfo, IModel vanillamodel, Int2ObjectMap<JsonElement> overrides) throws IOException {
        this.modelinfo = modelinfo;
        this.vanillamodel = vanillamodel;
        this.overrides = overrides;
        
        this.textureDependencies = new HashSet<>();
        this.textureDependencies.addAll(vanillamodel.getTextures());
        for (Entry<Integer, JsonElement> e : this.overrides.entrySet()) {
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
                if (obj.has("texture")) {
                    ResourceLocation rl = new ResourceLocation(obj.get("texture").getAsString());
                    textureDependencies.add(rl);
                }
                meta = GSON.fromJson(obj, IMetadataSectionCTM.class);
            }
            if (meta != null ) {
                metaOverrides.put(e.getKey(), meta);
                textureDependencies.addAll(Arrays.asList(meta.getAdditionalTextures()));
            }
        }
        
        this.textureDependencies.removeIf(rl -> rl.getResourcePath().startsWith("#"));
        
        // Validate all texture metadata
        for (ResourceLocation res : getTextures()) {
            IMetadataSectionCTM meta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(res));
            if (meta != null) {
                if (meta.getType().requiredTextures() != meta.getAdditionalTextures().length + 1) {
                    throw new IOException(String.format("Texture type %s requires exactly %d textures. %d were provided.", meta.getType(), meta.getType().requiredTextures(), meta.getAdditionalTextures().length + 1));
                }
            }
        }
    }
    
    @Override
    public IModel getVanillaParent() {
        return vanillamodel;
    }
    
    // TODO remove this reflection
    private static final MethodHandle _asVanillaModel; static {
        MethodHandle mh;
        try {
            mh = MethodHandles.lookup().unreflect(IModel.class.getMethod("asVanillaModel"));
        } catch (IllegalAccessException | NoSuchMethodException | SecurityException e) {
            mh = null;
        }
        _asVanillaModel = mh;
    }
    
    // @Override Soft override
    @SuppressWarnings("unchecked")
    public Optional<ModelBlock> asVanillaModel() {
        return Optional.ofNullable(_asVanillaModel)
                .<Optional<ModelBlock>>map(mh -> {
                    try {
                        return (Optional<ModelBlock>) mh.invokeExact(getVanillaParent());
                    } catch (Throwable e1) {
                        return Optional.empty();
                    }
                })
                .filter(Optional::isPresent)
                .orElse(Optional.ofNullable(modelinfo));
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        return textureDependencies;
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        IBakedModel parent = vanillamodel.bake(state, format, rl -> {
            TextureAtlasSprite sprite = bakedTextureGetter.apply(rl);
            IMetadataSectionCTM chiselmeta = null;
            try {
                chiselmeta = ResourceUtil.getMetadata(sprite);
            } catch (IOException e) {}
            final IMetadataSectionCTM meta = chiselmeta;
            textures.computeIfAbsent(sprite.getIconName(), s -> {
                ICTMTexture<?> tex;
                if (meta == null) {
                    tex = new TextureNormal(TextureTypeNormal.INSTANCE, new TextureInfo(new TextureAtlasSprite[] { sprite }, Optional.empty(), null));
                } else {
                    tex = meta.makeTexture(sprite, bakedTextureGetter);
                }
                layers |= 1 << (tex.getLayer() == null ? 7 : tex.getLayer().ordinal());
                return tex;
            });
            return sprite;
        });
        if (spriteOverrides == null) {
            spriteOverrides = new Int2ObjectArrayMap<>();
            // Convert all primitive values into sprites
            for (Entry<Integer, JsonElement> e : overrides.entrySet()) {
                if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isString()) {
                    TextureAtlasSprite sprite = bakedTextureGetter.apply(new ResourceLocation(e.getValue().getAsString()));
                    spriteOverrides.put(e.getKey(), sprite);
                } else if (e.getValue().isJsonObject()) {
                    JsonElement texture = e.getValue().getAsJsonObject().get("texture");
                    if (texture != null && texture.isJsonPrimitive()) {
                        spriteOverrides.put(e.getKey(), bakedTextureGetter.apply(new ResourceLocation(texture.getAsString())));
                    }
                }
            }
        }
        if (textureOverrides == null) {
            textureOverrides = new HashMap<>();
            for (Entry<Integer, IMetadataSectionCTM> e : metaOverrides.entrySet()) {
                List<BlockPartFace> matches = modelinfo.getElements().stream().flatMap(b -> b.mapFaces.values().stream()).filter(b -> b.tintIndex == e.getKey()).collect(Collectors.toList());
                Multimap<String, BlockPartFace> bySprite = HashMultimap.create();
                matches.forEach(part -> bySprite.put(modelinfo.textures.getOrDefault(part.texture.substring(1), part.texture), part));
                for (val e2 : bySprite.asMap().entrySet()) {
                    ResourceLocation texLoc = new ResourceLocation(e2.getKey());
                    TextureAtlasSprite sprite = getOverrideSprite(e.getKey());
                    if (sprite == null) {
                    	sprite = bakedTextureGetter.apply(texLoc);
                    }
                    ICTMTexture<?> tex = e.getValue().makeTexture(sprite, bakedTextureGetter);
                    layers |= 1 << (tex.getLayer() == null ? 7 : tex.getLayer().ordinal());
                    textureOverrides.put(Pair.of(e.getKey(), texLoc.toString()), tex);
                }
            }
        }
        return new ModelBakedCTM(this, parent);
    }

    @Override
    public IModelState getDefaultState() {
        return getVanillaParent().getDefaultState();
    }
    
    public Optional<? extends IClip> getClip(String name) {
        return getVanillaParent().getClip(name);
    }

    @Override
    public void load() {}
    
    @Override
    public Collection<ICTMTexture<?>> getChiselTextures() {
        return ImmutableList.<ICTMTexture<?>>builder().addAll(textures.values()).addAll(textureOverrides.values()).build();
    }
    
    @Override
    public ICTMTexture<?> getTexture(String iconName) {
        return textures.get(iconName);
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        // sign bit is used to signify that a layer-less (vanilla) texture is present
        return (layers < 0 && state.getBlock().getBlockLayer() == layer) || ((layers >> layer.ordinal()) & 1) == 1;
    }

    @Override
    @Nullable
    public TextureAtlasSprite getOverrideSprite(int tintIndex) {
        return spriteOverrides.get(tintIndex);
    }
    
    @Override
    @Nullable
    public ICTMTexture<?> getOverrideTexture(int tintIndex, String sprite) {
        return textureOverrides.get(Pair.of(tintIndex, sprite));
    }

    @Override
    public IModel retexture(ImmutableMap<String, String> textures) {
        try {
            ModelCTM ret = deepCopy(getVanillaParent().retexture(textures), null, null);

            ret.modelinfo.textures.putAll(textures);
            for (Entry<Integer, IMetadataSectionCTM> e : ret.metaOverrides.entrySet()) {
                ResourceLocation[] additionals = e.getValue().getAdditionalTextures();
                for (int i = 0; i < additionals.length; i++) {
                    ResourceLocation res = additionals[i];
                    if (res.getResourcePath().startsWith("#")) {
                        String newTexture = textures.get(res.getResourcePath().substring(1));
                        if (newTexture != null) {
                            additionals[i] = new ResourceLocation(newTexture);
                            ret.textureDependencies.add(additionals[i]);
                        }
                    }
                }
            }
            for (int i : ret.overrides.keySet()) {
                ret.overrides.compute(i, (idx, ele) -> {
                    if (ele.isJsonPrimitive() && ele.getAsJsonPrimitive().isString()) {
                        String newTexture = textures.get(ele.getAsString().substring(1));
                        if (newTexture != null) {
                            ele = new JsonPrimitive(newTexture);
                            ret.textureDependencies.add(new ResourceLocation(ele.getAsString()));
                        }
                    }
                    return ele;
                });
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
            return ModelLoaderRegistry.getMissingModel();
        }
    }
    
    @Override
    public IModel uvlock(boolean value) {
        if (uvlock == null || uvlock.booleanValue() != value) {
            IModel newParent = getVanillaParent().uvlock(value);
            if (newParent != getVanillaParent()) {
                IModel ret = deepCopyOrMissing(newParent, null, null);
                if (ret instanceof ModelCTM) {
                    ((ModelCTM) ret).uvlock = value;
                }
                return ret;
            }
        }
        return this;
    }

    /**
     * Allows the model to process custom data from the variant definition.
     * If unknown data is encountered it should be skipped.
     * @return a new model, with data applied.
     */
    public IModel process(ImmutableMap<String, String> customData) {
        return deepCopyOrMissing(getVanillaParent().process(customData), null, null);
    }

    public IModel smoothLighting(boolean value) {
        if (modelinfo.isAmbientOcclusion() != value) {
            return deepCopyOrMissing(getVanillaParent().smoothLighting(value), value, null);
        }
        return this;
    }

    public IModel gui3d(boolean value) {
        if (modelinfo.isGui3d() != value) {
            return deepCopyOrMissing(getVanillaParent().gui3d(value), null, value);
        }
        return this;
    }

    private IModel deepCopyOrMissing(IModel newParent, Boolean ao, Boolean gui3d) {
        try {
            return deepCopy(newParent, ao, gui3d);
        } catch (IOException e) {
            e.printStackTrace();
            return ModelLoaderRegistry.getMissingModel();
        }
    }

    private ModelCTM deepCopy(IModel newParent, Boolean ao, Boolean gui3d) throws IOException {
        // Deep copy logic taken from ModelLoader$VanillaModelWrapper
        List<BlockPart> parts = new ArrayList<>();
        for (BlockPart part : modelinfo.getElements()) {
        	parts.add(new BlockPart(part.positionFrom, part.positionTo, Maps.newHashMap(part.mapFaces), part.partRotation, part.shade));
        }
        
        ModelBlock newModel = new ModelBlock(modelinfo.getParentLocation(), parts,
                Maps.newHashMap(modelinfo.textures), ao == null ? modelinfo.isAmbientOcclusion() : ao, gui3d == null ? modelinfo.isGui3d() : gui3d,
                modelinfo.getAllTransforms(), Lists.newArrayList(modelinfo.getOverrides()));
        
        newModel.name = modelinfo.name;
        newModel.parent = modelinfo.parent;
        return new ModelCTM(newModel, newParent, new Int2ObjectArrayMap<>(overrides));
    }
}
