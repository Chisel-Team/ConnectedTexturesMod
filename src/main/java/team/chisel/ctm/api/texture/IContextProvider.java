package team.chisel.ctm.api.texture;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

@ParametersAreNonnullByDefault
public interface IContextProvider {

    /**
     * Called to create a context for an upcoming render. This context will then be passed to
     * {@link ICTMTexture#transformQuad(net.minecraft.client.renderer.block.model.BakedQuad, ITextureContext, int)}.
     * 
     * @param state
     *            The state of the block being rendered.
     * @param world
     *            The current rendering world.
     * @param pos
     *            The position of the block being rendered.
     * @param tex
     *            The current {@link ICTMTexture} being rendered.
     * @return A context which can be used to manipulate quads later in the pipeline.
     */
    ITextureContext getBlockRenderContext(BlockState state, BlockAndTintGetter world, BlockPos pos, ICTMTexture<?> tex);

    /**
     * Recreates a render context compressed data.
     * <br><br>
     * As of yet, this method is unused.
     * 
     * @param data
     *            The compressed data, will match what is produced by {@link ITextureContext#getCompressedData()}.
     */
    @Deprecated
    ITextureContext getContextFromData(long data);
}
