package team.chisel.ctm.client.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public interface LocalDirection {

    /**
     * Finds if this block is connected for the given side in this Dir.
     *
     * @param ctm
     *            The CTM instance to use for logic.
     * @param world
     *            The world the block is in.
     * @param pos
     *            The position of your block.
     * @param side
     *            The side of the current face.
     * @return True if the block is connected in the given Dir, false otherwise.
     */
    boolean isConnected(ConnectionCheck ctm, BlockGetter world, BlockPos pos, Direction side);

    /**
     * Finds if this block is connected for the given side in this Dir.
     *
     * @param ctm
     *            The CTM instance to use for logic.
     * @param world
     *            The world the block is in.
     * @param pos
     *            The position of your block.
     * @param side
     *            The side of the current face.
     * @param state
     *            The state to check for connection with.
     * @return True if the block is connected in the given Dir, false otherwise.
     */
    boolean isConnected(ConnectionCheck ctm, BlockGetter world, BlockPos pos, Direction side, BlockState state);

    LocalDirection relativize(Direction normal);

    BlockPos getOffset(Direction normal);

    int ordinal();

}
