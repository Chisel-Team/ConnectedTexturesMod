package team.chisel.ctm.client.texture.type;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.render.TextureCTM;
import team.chisel.ctm.client.texture.render.TextureEdges;
import team.chisel.ctm.client.texture.render.TextureEdgesFull;
import team.chisel.ctm.client.util.CTMLogic;

@TextureType("EDGES_FULL")
public class TextureTypeEdgesFull extends TextureTypeEdges {
    
    @Override
    public ICTMTexture<? extends TextureTypeCTM> makeTexture(TextureInfo info) {
        return new TextureEdgesFull(this, info);
    }
    
    public static class CTMLogicFull extends CTMLogicEdges {
        
        public CTMLogicFull(TextureEdges tex) {
            super(tex);
        }

        @Override
        public boolean isConnected(IBlockAccess world, BlockPos current, BlockPos connection, EnumFacing dir, IBlockState state) {
            if (isObscured()) {
                return false;
            }
            IBlockState obscuring = world.getBlockState(current.offset(dir));
            if (tex.getConnectTo().contains(obscuring.getBlock())) {
                setObscured(true);
                return false;
            }
            IBlockState obscuringcon = world.getBlockState(connection.offset(dir));
            if (tex.getConnectTo().contains(obscuringcon.getBlock())) {
                return true;
            }
            IBlockState con = world.getBlockState(connection);
            if (tex.getConnectTo().contains(con.getBlock())) {
                Vec3d difference = new Vec3d(connection.subtract(current));
                if (difference.lengthSquared() > 1) {
                    difference = difference.normalize();
                    BlockPos posA = new BlockPos(difference.rotateYaw((float) Math.PI /  4)).add(current);
                    BlockPos posB = new BlockPos(difference.rotateYaw((float) Math.PI / -4)).add(current);
                    return world.getBlockState(posA) == state || world.getBlockState(posB) == state;
                } else {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public long serialized() {
            return isObscured() ? (super.serialized() | (1 << 8)) : super.serialized();
        }
    }

    @Override
    public TextureContextCTM getBlockRenderContext(IBlockState state, IBlockAccess world, BlockPos pos, ICTMTexture<?> tex) {
        return new TextureContextCTM(state, world, pos, (TextureCTM<?>) tex) {
            @Override
            protected CTMLogic createCTM(IBlockState state) {
                return new CTMLogicFull((TextureEdges) tex);
            }
        };
    }
    
    @Override
    public int getQuadsPerSide() {
        return 1;
    }
}
