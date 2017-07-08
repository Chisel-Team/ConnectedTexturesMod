package team.chisel.ctm.client.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
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
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.IChiselFace;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;
import team.chisel.ctm.client.texture.render.TextureNormal;
import team.chisel.ctm.client.texture.type.TextureTypeNormal;
import team.chisel.ctm.client.util.ResourceUtil;

public class ModelCTM implements IModelCTM {
    
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(IMetadataSectionCTM.class, new IMetadataSectionCTM.Serializer()).create();

    private final ModelBlock modelinfo;
    private final IModel parentmodel;

    // Populated from overrides data during construction
    private final Int2ObjectMap<JsonElement> overrides;
    protected final Int2ObjectMap<IMetadataSectionCTM> metaOverrides = new Int2ObjectArrayMap<>();
    
    // Populated during bake with real texture data
    protected Int2ObjectMap<TextureAtlasSprite> spriteOverrides;
    protected Map<Pair<Integer, String>, ICTMTexture<?>> textureOverrides;

    private final Collection<ResourceLocation> textureDependencies;
    
    private transient byte layers;

    private Map<String, ICTMTexture<?>> textures = new HashMap<>();
    
    public ModelCTM(ModelBlock modelinfo, IModel parent, Int2ObjectMap<JsonElement> overrides) throws IOException {
        this.modelinfo = modelinfo;
        this.parentmodel = parent;
        this.overrides = overrides;
        
        this.textureDependencies = new HashSet<>();
        this.textureDependencies.addAll(parentmodel.getTextures());
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
                meta = GSON.fromJson(obj, IMetadataSectionCTM.class);
            }
            if (meta != null ) {
                metaOverrides.put(e.getKey(), meta);
                textureDependencies.addAll(Arrays.asList(meta.getAdditionalTextures()));
            }
        }
        
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
    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        return textureDependencies;
    }

    @Override
    @SuppressWarnings("null")
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        IBakedModel parent = parentmodel.bake(state, format, rl -> {
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
                    ICTMTexture<?> tex = e.getValue().makeTexture(spriteOverrides.get(e.getKey()), bakedTextureGetter);
                    textureOverrides.put(Pair.of(e.getKey(), texLoc.toString()), tex);
                }
            }
        }
        return new ModelBakedCTM(this, parent);
    }

    @Override
    public IModelState getDefaultState() {
        return TRSRTransformation.identity();
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
    @Deprecated
    public IChiselFace getFace(EnumFacing facing) {
        return null;
    }

    @Override
    @Deprecated
    public IChiselFace getDefaultFace() {
        return null;
    }
    
    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        // sign bit is used to signify that a layer-less (vanilla) texture is present
        return (layers < 0 && state.getBlock().getBlockLayer() == layer) || ((layers >> layer.ordinal()) & 1) == 1;
    }

    @Override
    public boolean ignoreStates() {
        return false;
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
}
