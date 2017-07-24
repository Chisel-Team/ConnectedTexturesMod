package team.chisel.ctm.client.texture.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.render.TextureEdges;
import team.chisel.ctm.client.util.CTMLogic;
import team.chisel.ctm.client.util.Dir;

@TextureType("EDGES")
public class TextureTypeEdges extends TextureTypeCTM {

    @Override
    public ICTMTexture<? extends TextureTypeCTM> makeTexture(TextureInfo info) {
        return new TextureEdges(this, info);
    }
    
    @RequiredArgsConstructor
    public static class CTMLogicEdges extends CTMLogic {
        
        protected final TextureEdges tex;
        
        @Setter
        @Getter
        private boolean obscured;
        
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

            IBlockState con = world.getBlockState(connection);
            IBlockState obscuringcon = world.getBlockState(connection.offset(dir));
            
            if (tex.getConnectTo().contains(con.getBlock()) || tex.getConnectTo().contains(obscuringcon.getBlock())) {
                Vec3d difference = new Vec3d(connection.subtract(current));
                if (difference.lengthSquared() > 1) {
                    difference = difference.normalize();
                    BlockPos posA = new BlockPos(difference.rotateYaw((float) Math.PI /  4)).add(current);
                    BlockPos posB = new BlockPos(difference.rotateYaw((float) Math.PI / -4)).add(current);
                    return (world.getBlockState(posA) == state && !tex.getConnectTo().contains(world.getBlockState(posA.offset(dir)).getBlock())) || (world.getBlockState(posB) == state && !tex.getConnectTo().contains(world.getBlockState(posB.offset(dir)).getBlock()));
                } else {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        protected void fillSubmaps(int idx) {
            Dir[] dirs = submapMap[idx];
            if (!connectedOr(dirs[0], dirs[1]) && connected(dirs[2])) {
                submapCache[idx] = submapOffsets[idx];
            } else {
                super.fillSubmaps(idx);
            }
        }
        
        @Override
        public long serialized() {
            return isObscured() ? (super.serialized() | (1 << 8)) : super.serialized();
        }
    }
    
    @Override
    public TextureContextCTM getBlockRenderContext(IBlockState state, IBlockAccess world, BlockPos pos, ICTMTexture<?> tex) {
        return new TextureContextCTM(state, world, pos, (TextureEdges) tex) {
            
            @Override
            protected CTMLogic createCTM(IBlockState state) {
                return new CTMLogicEdges((TextureEdges) tex);
            }
        };
    }

    @Override
    public int requiredTextures() {
        return 3;
    }
}
