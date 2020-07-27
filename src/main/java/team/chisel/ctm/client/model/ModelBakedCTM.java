package team.chisel.ctm.client.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ObjectArrays;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.RenderContextList;
import team.chisel.ctm.client.util.BakedQuadRetextured;
import team.chisel.ctm.client.util.CTMPackReloadListener;

@ParametersAreNonnullByDefault
public class ModelBakedCTM extends AbstractCTMBakedModel {
    
    public ModelBakedCTM(IModelCTM model, IBakedModel parent) {
        super(model, parent);
    }

    private static final Direction[] FACINGS = ObjectArrays.concat(Direction.values(), (Direction) null);

    @Override
    protected AbstractCTMBakedModel createModel(@Nullable BlockState state, IModelCTM model, IBakedModel parent, @Nullable RenderContextList ctx, Random rand) {
        while (parent instanceof ModelBakedCTM) {
            parent = ((AbstractCTMBakedModel)parent).getParent(rand);
        }

        AbstractCTMBakedModel ret = new ModelBakedCTM(model, parent);
        for (RenderType layer : LAYERS) {
            for (Direction facing : FACINGS) {
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
                    ICTMTexture<?> tex = this.getOverrideTexture(rand, q.getTintIndex(), q.func_187508_a().getName());
                    if (tex == null) {
                        tex = this.getTexture(rand, q.func_187508_a().getName());
                    }

                    if (tex != null) {
                        TextureAtlasSprite spriteReplacement = this.getOverrideSprite(rand, q.getTintIndex());
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
                    if ((e.getValue().getLayer() != null && e.getValue().getLayer().getRenderType() == layer) || (e.getValue().getLayer() == null && (state == null || CTMPackReloadListener.canRenderInLayerFallback(state, layer)))) {
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
        return Optional.ofNullable(getModel().getTexture(getParent().getParticleTexture().getName()))
                .map(ICTMTexture::getParticle)
                .orElse(getParent().getParticleTexture());
    }
    
    @Override
    public IBakedModel handlePerspective(TransformType cameraTransformType, MatrixStack ms) {
    	// FIXME this won't work if parent returns a different model (shouldn't happen for vanilla)
    	getParent().handlePerspective(cameraTransformType, ms);
    	return this;
    }
}
