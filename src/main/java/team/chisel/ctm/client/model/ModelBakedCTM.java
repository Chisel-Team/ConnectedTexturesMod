package team.chisel.ctm.client.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ObjectArrays;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.RenderContextList;
import team.chisel.ctm.client.util.BakedQuadRetextured;

@ParametersAreNonnullByDefault
public class ModelBakedCTM extends AbstractCTMBakedModel {
    
    public ModelBakedCTM(IModelCTM model, BakedModel parent, @Nullable RenderType layer) {
        super(model, parent, layer);
    }

    private static final Direction[] FACINGS = ObjectArrays.concat(Direction.values(), (Direction) null);

    @Override
    protected AbstractCTMBakedModel createModel(@Nullable BlockState state, IModelCTM model, BakedModel parent, @Nullable RenderContextList ctx, RandomSource rand, ModelData data, @Nullable RenderType layer) {
        while (parent instanceof ModelBakedCTM castParent) {
            parent = castParent.getParent(rand);
        }

        BakedModel finalParent = parent;
        //Calculate this lazily as there is a variety of cases where we just passthrough and we only want to calculate
        // this a single time, especially with the "duplicate" calls when checking the render type as an item render type
        Lazy<Boolean> layerMatches = Lazy.of(() -> {
            //No state or the parent's render types for the state contains the layer
            if (state == null || finalParent.getRenderTypes(state, rand, data).contains(layer)) {
                return true;
            }
            //Try to see if the render type is actually for the item variant of this block. This may be necessary if a mod is
            // getting the block's model directly and then trying to render it, such as from within an ItemStackBlockEntityRender
            ItemStack stack = new ItemStack(state.getBlock());
            if (!stack.isEmpty()) {
                //Some of these may be duplicate, but we need to check it as both fabulous and not as we don't have a display context
                // available in order to check if it is a gui or first person
                return finalParent.getRenderTypes(stack, false).contains(layer) ||
                       finalParent.getRenderTypes(stack, true).contains(layer);
            }
            return false;
        });
        AbstractCTMBakedModel ret = new ModelBakedCTM(model, parent, layer);
        for (Direction facing : FACINGS) {
            List<BakedQuad> parentQuads = parent.getQuads(state, facing, rand, data, null); // NOTE: We pass null here so that all quads are always returned, layer filtering is done below
            List<BakedQuad> quads;
            if (facing != null) {
                quads = ret.faceQuads.get(facing);
            } else {
                quads = ret.genQuads;
            }
            
            // Linked to maintain the order of quads
            Map<BakedQuad, ICTMTexture<?>> texturemap = new LinkedHashMap<>();
            // Gather all quads and map them to their textures
            // All quads should have an associated ICTMTexture, so ignore any that do not
            for (BakedQuad q : parentQuads) {
                ICTMTexture<?> tex = this.getOverrideTexture(rand, q.getTintIndex(), q.getSprite().contents().name());
                if (tex == null) {
                    tex = this.getTexture(rand, q.getSprite().contents().name());
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
                ICTMTexture<?> texture = e.getValue();
                // If the layer is null, this is a wrapped vanilla texture, so passthrough the layer check to the block
                if (layer == null || (texture.getLayer() != null && texture.getLayer().getRenderType() == layer) || (texture.getLayer() == null && layerMatches.get())) {
                    ITextureContext tcx = ctx == null ? null : ctx.getRenderContext(texture);
                    quads.addAll(texture.transformQuad(e.getKey(), tcx, quadGoal));
                }
            }
        }
        return ret;
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return wrapParticleIcon(super.getParticleIcon());
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        return wrapParticleIcon(super.getParticleIcon(data));
    }

    private @NotNull TextureAtlasSprite wrapParticleIcon(@NotNull TextureAtlasSprite particleIcon) {
        return Optional.ofNullable(getModel().getTexture(particleIcon.contents().name()))
              .map(ICTMTexture::getParticle)
              .orElse(particleIcon);
    }
}
