package team.chisel.ctm.client.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ObjectArrays;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.RenderContextList;
import team.chisel.ctm.client.util.BakedQuadRetextured;

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
                
                // Linked to maintain the order of quads
                Map<BakedQuad, ICTMTexture<?>> texturemap = new LinkedHashMap<>();
                // Gather all quads and map them to their textures
                // All quads should have an associated ICTMTexture, so ignore any that do not
                for (BakedQuad q : parentQuads) {
                    ICTMTexture<?> tex = this.getModel().getOverrideTexture(q.getTintIndex(), q.getSprite().getIconName());
                    if (tex == null) {
                        tex = this.getModel().getTexture(q.getSprite().getIconName());
                    }

                    if (tex != null) {
                        TextureAtlasSprite spriteReplacement = getModel().getOverrideSprite(q.getTintIndex());
                        if (spriteReplacement != null) {
                            q = new BakedQuadRetextured(q, spriteReplacement);
                        }

                        texturemap.put(q, tex);
                    }
                }

                // Compute the quad goal for a given facing
                // TODO this means that non-culling (null facing) quads will *all* share the same quad goal, which is excessive
                // Explore optimizations to quad goal (detecting overlaps??)
                int quadGoal = ctx == null ? 1 : texturemap.values().stream().mapToInt(tex -> tex.getType().getQuadsPerSide()).max().orElse(1);
                for (Entry<BakedQuad, ICTMTexture<?>> e : texturemap.entrySet()) {
                    // If the layer is null, this is a wrapped vanilla texture, so passthrough the layer check to the block
                    if (e.getValue().getLayer() == layer || (e.getValue().getLayer() == null && (state == null || state.getBlock().canRenderInLayer(state, layer)))) {
                        ITextureContext tcx = ctx == null ? null : ctx.getRenderContext(e.getValue());
                        quads.addAll(e.getValue().transformQuad(e.getKey(), tcx, quadGoal));
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
    	// FIXME this won't work if parent returns a different model (shouldn't happen for vanilla)
    	return Pair.of(this, getParent().handlePerspective(cameraTransformType).getRight());
    }
}
