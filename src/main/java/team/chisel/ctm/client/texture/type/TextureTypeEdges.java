package team.chisel.ctm.client.texture.type;

import javax.annotation.ParametersAreNonnullByDefault;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.render.TextureEdges;
import team.chisel.ctm.client.util.CTMLogic;
import team.chisel.ctm.client.util.Dir;

@TextureType("edges")
public class TextureTypeEdges extends TextureTypeCTM {

    @Override
    public ICTMTexture<? extends TextureTypeCTM> makeTexture(TextureInfo info) {
        return new TextureEdges(this, info);
    }
    
    @RequiredArgsConstructor
    @ParametersAreNonnullByDefault
    public static class CTMLogicEdges extends CTMLogic {
        
        @Setter
        @Getter
        private boolean obscured;
        
        @Override
        public boolean isConnected(IBlockReader world, BlockPos current, BlockPos connection, Direction dir, BlockState state) {
            if (isObscured()) {
                return false;
            }
            BlockState obscuring = getConnectionState(world, current.offset(dir), dir, current);
            if (stateComparator(state, obscuring, dir)) {
                setObscured(true);
                return false;
            }

            BlockState con = getConnectionState(world, connection, dir, current);
            BlockState obscuringcon = getConnectionState(world, connection.offset(dir), dir, current);
            
            if (stateComparator(state, con, dir) || stateComparator(state, obscuringcon, dir)) {
                Vector3d difference = Vector3d.func_237491_b_(connection.subtract(current));
                if (difference.lengthSquared() > 1) {
                    difference = difference.normalize();
                    if (dir.getAxis() == Axis.Z) {
                        difference = difference.rotateYaw((float) (-Math.PI / 2));
                    }
                    float ang = (float) Math.PI / 4;
                    Vector3d vA, vB;
                    if (dir.getAxis().isVertical()) {
                        vA = difference.rotateYaw(ang);
                        vB = difference.rotateYaw(-ang);
                    } else {
                        vA = difference.rotatePitch(ang);
                        vB = difference.rotatePitch(-ang);
                    }
                    BlockPos posA = new BlockPos(vA).add(current);
                    BlockPos posB = new BlockPos(vB).add(current);
                    return (getConnectionState(world, posA, dir, current) == state && !stateComparator(state, getConnectionState(world, posA.offset(dir), dir, current), dir))
                        || (getConnectionState(world, posB, dir, current) == state && !stateComparator(state, getConnectionState(world, posB.offset(dir), dir, current), dir));
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
    public TextureContextCTM getBlockRenderContext(BlockState state, IBlockReader world, BlockPos pos, ICTMTexture<?> tex) {
        return new TextureContextCTM(state, world, pos, (TextureEdges) tex) {
            
            @Override
            protected CTMLogic createCTM(BlockState state) {
                CTMLogic parent = super.createCTM(state);
                // FIXME
                CTMLogic ret = new CTMLogicEdges();
                ret.ignoreStates(parent.ignoreStates()).stateComparator(parent.stateComparator());
                ret.disableObscuredFaceCheck = parent.disableObscuredFaceCheck;
                return ret;
            }
        };
    }

    @Override
    public int requiredTextures() {
        return 3;
    }
}
