package team.chisel.ctm.client.newctm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
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
     * @param state
     *            The state of your block.
     * @param side
     *            The side of the current face.
     * @return True if the block is connected in the given Dir, false otherwise.
     */
    boolean isConnected(ConnectionCheck ctm, BlockAndTintGetter world, BlockPos pos, BlockState state, Direction side);

    /**
     * Finds if this block is connected for the given side in this Dir.
     *
     * @param ctm
     *            The CTM instance to use for logic.
     * @param world
     *            The world the block is in.
     * @param pos
     *            The position of your block.
     * @param state
     *            The state of your block.
     * @param side
     *            The side of the current face.
     * @param connectionState
     *            The state to check for connection with.
     * @return True if the block is connected in the given Dir, false otherwise.
     */
    boolean isConnected(ConnectionCheck ctm, BlockAndTintGetter world, BlockPos pos, BlockState state, Direction side, BlockState connectionState);

    LocalDirection relativize(Direction normal);

    BlockPos getOffset(Direction normal);

    int ordinal();
    
    String name();

    String asJson();
}
