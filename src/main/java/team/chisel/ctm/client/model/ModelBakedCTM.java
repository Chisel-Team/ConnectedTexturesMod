package team.chisel.ctm.client.model;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ObjectArrays;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BakedQuadRetextured;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.common.model.TRSRTransformation;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.util.RenderContextList;
import team.chisel.ctm.client.state.ChiselExtendedState;
import team.chisel.ctm.client.util.Quad;

@ParametersAreNonnullByDefault
public class ModelBakedCTM extends AbstractCTMBakedModel {
    
    public ModelBakedCTM(IModelCTM model, IBakedModel parent) {
        super(model, parent);
    }

    private static final EnumFacing[] FACINGS = ObjectArrays.concat(EnumFacing.VALUES, (EnumFacing) null);

    @Override
    protected AbstractCTMBakedModel createModel(@Nullable IBlockState state, IModelCTM model, @Nullable RenderContextList ctx, long rand) {
        IBakedModel parent = getParent(rand);
        AbstractCTMBakedModel ret = new ModelBakedCTM(model, parent);
        for (BlockRenderLayer layer : LAYERS) {
            for (EnumFacing facing : FACINGS) {
                List<BakedQuad> parentQuads = parent.getQuads(state, facing, rand);
                List<BakedQuad> quads;
                if (facing != null) {
                    ret.faceQuads.put(layer, facing, quads = new ArrayList<>());
                } else {
                    quads = ret.genQuads.get(layer);
                }
                for (BakedQuad q : parentQuads) {
                    ICTMTexture<?> tex = this.getModel().getOverrideTexture(q.getTintIndex(), q.getSprite().getIconName());
                    if (tex == null) {
                        tex = this.getModel().getTexture(q.getSprite().getIconName());
                    }
                    TextureAtlasSprite spriteReplacement = getModel().getOverrideSprite(q.getTintIndex());
                    if (spriteReplacement != null) {
                        q = new BakedQuadRetextured(q, spriteReplacement);
                    }

                    if (!(state instanceof ChiselExtendedState) || (tex == null && layer == state.getBlock().getBlockLayer())) {
                        quads.add(q);
                    } else if (tex != null && layer == tex.getLayer()) {
                        ITextureType type = tex.getType();

                        ITextureContext brc = ctx == null ? null : ctx.getRenderContext(tex.getType());
                        quads.addAll(tex.transformQuad(q, brc, type.getQuadsPerSide()));
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public @Nonnull TextureAtlasSprite getParticleTexture() {
        return getParent().getParticleTexture();
    }
    
    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
        if (getParent() instanceof IPerspectiveAwareModel) {
            return ((IPerspectiveAwareModel) getParent()).handlePerspective(cameraTransformType);
        } else {
            return Pair.of(this, new TRSRTransformation(getParent().getItemCameraTransforms().getTransform(cameraTransformType)).getMatrix());
        }
    }
}
