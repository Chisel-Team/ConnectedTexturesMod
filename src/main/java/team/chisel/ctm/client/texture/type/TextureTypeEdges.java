package team.chisel.ctm.client.texture.type;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.newctm.ConnectionCheck;
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
    
    @ParametersAreNonnullByDefault
    public static class CTMLogicEdges extends CTMLogic {
        
        public CTMLogicEdges() {
            this.connectionCheck = new ConnectionCheckEdges();
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

        public boolean isObscured() {
            return ((ConnectionCheckEdges)connectionCheck).isObscured();
        }
    }
    
    public static class ConnectionCheckEdges extends ConnectionCheck {
        
        @Setter
        @Getter
        private boolean obscured;
        
        @Override
        public boolean isConnected(BlockAndTintGetter world, BlockPos current, BlockState currentState, BlockPos connection, Direction dir, BlockState state) {
            if (isObscured()) {
                return false;
            }
            BlockState obscuring = getConnectionState(world, current.relative(dir), dir, current, currentState);
            if (stateComparator(state, obscuring, dir)) {
                setObscured(true);
                return false;
            }

            BlockState con = getConnectionState(world, connection, dir, current, currentState);
            BlockState obscuringcon = getConnectionState(world, connection.relative(dir), dir, current, currentState);
            
            if (stateComparator(state, con, dir) || stateComparator(state, obscuringcon, dir)) {
                Vec3 difference = Vec3.atLowerCornerOf(connection.subtract(current));
                if (difference.lengthSqr() > 1) {
                    difference = difference.normalize();
                    if (dir.getAxis() == Direction.Axis.Z) {
                        difference = difference.yRot((float) (-Math.PI / 2));
                    }
                    float ang = (float) Math.PI / 4;
                    Vec3 vA, vB;
                    if (dir.getAxis().isVertical()) {
                        vA = difference.yRot(ang);
                        vB = difference.yRot(-ang);
                    } else {
                        vA = difference.xRot(ang);
                        vB = difference.xRot(-ang);
                    }
                    BlockPos posA = BlockPos.containing(vA).offset(current);
                    BlockPos posB = BlockPos.containing(vB).offset(current);
                    return (getConnectionState(world, posA, dir, current, currentState) == state && !stateComparator(state, getConnectionState(world, posA.relative(dir), dir, current, currentState), dir))
                        || (getConnectionState(world, posB, dir, current, currentState) == state && !stateComparator(state, getConnectionState(world, posB.relative(dir), dir, current, currentState), dir));
                } else {
                    return true;
                }
            }
            return false;
        }
    }
    
    @Override
    public TextureContextCTM getBlockRenderContext(BlockState state, BlockAndTintGetter world, BlockPos pos, ICTMTexture<?> tex) {
        return new TextureContextCTM(state, world, pos, (TextureEdges) tex) {
            
            @Override
            protected CTMLogic createCTM(BlockState state) {
                CTMLogic parent = super.createCTM(state);
                // FIXME
                CTMLogic ret = new CTMLogicEdges();
                ret.connectionCheck.ignoreStates(parent.connectionCheck.ignoreStates()).stateComparator(parent.connectionCheck.stateComparator());
                ret.connectionCheck.disableObscuredFaceCheck = parent.connectionCheck.disableObscuredFaceCheck;
                return ret;
            }
        };
    }

    @Override
    public int requiredTextures() {
        return 3;
    }
}
