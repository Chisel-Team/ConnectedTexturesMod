package team.chisel.ctm.client.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.base.Function;
import com.google.common.collect.ObjectArrays;

import net.minecraft.block.state.IBlockState;
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
import team.chisel.ctm.client.texture.MetadataSectionCTM;
import team.chisel.ctm.client.util.ResourceUtil;

public class ModelCTM implements IModelCTM {

    private final ModelBlock modelinfo;
    private final IModel parentmodel;

    private final Map<String, String[]> textureLists;
    
    private Collection<ResourceLocation> textureDependencies;
    
    private transient byte layers;

    private Map<String, ICTMTexture<?>> textures = new HashMap<>();
    private boolean hasVanillaTextures;
    
    public ModelCTM(ModelBlock modelinfo, IModel parent, Map<String, String[]> textureLists) {
        this.modelinfo = modelinfo;
        this.parentmodel = parent;
        this.textureLists = textureLists;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        if (textureDependencies != null) {
            return textureDependencies;
        }
        textureDependencies = new HashSet<>();
        Map<ResourceLocation, String[]> resolvedTextureLists = new HashMap<>();
        if (modelinfo != null) {
            for (Entry<String, String[]> e : textureLists.entrySet()) {
                if (modelinfo.isTexturePresent(e.getKey())) {
                    resolvedTextureLists.put(new ResourceLocation(modelinfo.textures.get(e.getKey())), e.getValue());
                } else {
                    resolvedTextureLists.put(new ResourceLocation(e.getKey()), e.getValue());
                }
            }
        }
        for (ResourceLocation rl : parentmodel.getTextures()) {
            if (resolvedTextureLists.containsKey(rl)) {
                for (String s : resolvedTextureLists.get(rl)) {
                    textureDependencies.add(new ResourceLocation(s));
                }
            } else {
                textureDependencies.add(rl);
            }
        }
        return getTextures();
    }

    @Override
    @SuppressWarnings("null")
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        IBakedModel parent = parentmodel.bake(state, format, rl -> {
            TextureAtlasSprite sprite = bakedTextureGetter.apply(rl);
            MetadataSectionCTM chiselmeta = null;
            try {
                chiselmeta = ResourceUtil.getMetadata(sprite);
            } catch (IOException e) {}
            if (chiselmeta != null) {
                final MetadataSectionCTM meta = chiselmeta;
                textures.computeIfAbsent(sprite.getIconName(), s -> {
                    // TODO VERY TEMPORARY
                    ICTMTexture<?> tex = meta.getType().makeTexture(new TextureInfo(
                            Arrays.stream(ObjectArrays.concat(new ResourceLocation(sprite.getIconName()), meta.getAdditionalTextures())).map(bakedTextureGetter::apply).toArray(TextureAtlasSprite[]::new), 
                            Optional.of(meta.getExtraData()), 
                            meta.getLayer()
                    )); 
                    layers |= 1 << tex.getLayer().ordinal();
                    return tex;
                });
            } else {
                hasVanillaTextures = true;
            }
            return sprite;
        });
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
        return textures.values();
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
        return (hasVanillaTextures && state.getBlock().getBlockLayer() == layer) || ((layers >> layer.ordinal()) & 1) == 1;
    }

    @Override
    public boolean ignoreStates() {
        return false;
    }
}
