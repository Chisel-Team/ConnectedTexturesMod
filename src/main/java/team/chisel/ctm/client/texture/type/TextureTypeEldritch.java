package team.chisel.ctm.client.texture.type;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextPosition;
import team.chisel.ctm.client.texture.render.TextureEldritch;

@ParametersAreNonnullByDefault
@TextureType("eldritch")
public class TextureTypeEldritch implements ITextureType {

    public static class Context extends TextureContextPosition {

        private final BlockPos wrappedpos;

        public Context(BlockPos pos) {
            super(pos);
            wrappedpos = new BlockPos(pos.getX() & 7, pos.getY() & 7, pos.getZ() & 7);
        }

        @Override
        public BlockPos getPosition() {
            return wrappedpos;
        }

        @Override
        public long getCompressedData() {
            return getPosition().toLong();
        }
    }

    @Override
    public ITextureContext getBlockRenderContext(IBlockState state, IBlockAccess world, BlockPos pos, ICTMTexture<?> tex) {
        return new Context(pos);
    }

    @Override
    public ITextureContext getContextFromData(long data) {
        return new Context(BlockPos.fromLong(data));
    }

    @Override
    public ICTMTexture<TextureTypeEldritch> makeTexture(TextureInfo info) {
        return new TextureEldritch(this, info);
    }
}
