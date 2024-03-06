package team.chisel.ctm.client.newctm;

import java.util.Optional;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import team.chisel.ctm.Configurations;
import team.chisel.ctm.client.util.CTMLogic.StateComparisonCallback;

@Accessors(fluent = true, chain = true)
public class ConnectionCheck {

    public Optional<Boolean> disableObscuredFaceCheck = Optional.empty();
    
    @Getter
    @Setter
    protected boolean ignoreStates;
    
    @Getter
    @Setter
    protected StateComparisonCallback stateComparator = StateComparisonCallback.DEFAULT;

    /**
     * A simple check for if the given block can connect to the given direction on the given side.
     * 
     * @param world
     * @param current
     *            The position of your block.
     * @param connection
     *            The position of the block to check against.
     * @param dir
     *            The {@link Direction side} of the block to check for connection status. This is <i>not</i> the direction to check in.
     * @return True if the given block can connect to the given location on the given side.
     */
    public final boolean isConnected(BlockAndTintGetter world, BlockPos current, BlockPos connection, Direction dir) {

        BlockState state = getConnectionState(world, current, dir, connection);
        return isConnected(world, current, connection, dir, state);
    }

    /**
     * A simple check for if the given block can connect to the given direction on the given side.
     * 
     * @param world
     * @param current
     *            The position of your block.
     * @param connection
     *            The position of the block to check against.
     * @param dir
     *            The {@link Direction side} of the block to check for connection status. This is <i>not</i> the direction to check in.
     * @param state
     *            The state to check against for connection.
     * @return True if the given block can connect to the given location on the given side.
     */
    @SuppressWarnings({ "unused", "null" })
    public boolean isConnected(BlockAndTintGetter world, BlockPos current, BlockPos connection, Direction dir, BlockState state) {

//      if (CTMLib.chiselLoaded() && connectionBlocked(world, x, y, z, dir.ordinal())) {
//          return false;
//      }
      
        BlockPos obscuringPos = connection.relative(dir);

        boolean disableObscured = disableObscuredFaceCheck.orElseGet(Configurations::connectInsideCTM);

        BlockState con = getConnectionState(world, connection, dir, current);
        BlockState obscuring = disableObscured ? null : getConnectionState(world, obscuringPos, dir, current);

        // bad API user
        if (con == null) {
            throw new IllegalStateException("Error, received null blockstate as facade from block " + world.getBlockState(connection));
        }

        boolean ret = stateComparator(state, con, dir);

        // no block obscuring this face
        if (obscuring == null) {
            return ret;
        }

        // check that we aren't already connected outwards from this side
        ret &= !stateComparator(state, obscuring, dir);

        return ret;
    }
    
    public boolean stateComparator(BlockState from, BlockState to, Direction dir) {
        return stateComparator.connects(this, from, to, dir);
    }

//    private boolean connectionBlocked(IBlockReader world, int x, int y, int z, int side) {
//        Block block = world.getBlock(x, y, z);
//        if (block instanceof IConnectable) {
//            return !((IConnectable) block).canConnectCTM(world, x, y, z, side);
//        }
//        return false;
//    }

    public BlockState getConnectionState(BlockAndTintGetter world, BlockPos pos, @Nullable Direction side, BlockPos connection) {
        BlockState state = world.getBlockState(pos);
        if (side != null) {
            return state.getAppearance(world, pos, side, world.getBlockState(connection), connection);
        }
        return state;
    }

}
