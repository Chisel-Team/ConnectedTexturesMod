package team.chisel.ctm.client.util;

import static net.minecraft.core.Direction.DOWN;
import static net.minecraft.core.Direction.EAST;
import static net.minecraft.core.Direction.NORTH;
import static net.minecraft.core.Direction.SOUTH;
import static net.minecraft.core.Direction.UP;
import static net.minecraft.core.Direction.WEST;
import static net.minecraft.core.Direction.AxisDirection;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import team.chisel.ctm.api.util.NonnullType;

/**
 * Think of this class as a "Two dimensional ForgeDirection, with diagonals".
 * <p>
 * It represents the eight different directions a face of a block can connect with CTM, and contains the logic for determining if a block is indeed connected in that direction.
 * <p>
 * Note that, for example, {@link #TOP_RIGHT} does not mean connected to the {@link #TOP} and {@link #RIGHT}, but connected in the diagonal direction represented by {@link #TOP_RIGHT}. This is used
 * for inner corner rendering.
 */
@ParametersAreNonnullByDefault
public enum Dir {
	// @formatter:off
    TOP(UP), 
    TOP_RIGHT(UP, EAST),
    RIGHT(EAST), 
    BOTTOM_RIGHT(DOWN, EAST), 
    BOTTOM(DOWN), 
    BOTTOM_LEFT(DOWN, WEST), 
    LEFT(WEST), 
    TOP_LEFT(UP, WEST);
    // @formatter:on

	/**
	 * All values of this enum, used to prevent unnecessary allocation via {@link #values()}.
	 */
	public static final Dir[] VALUES = values();
	private static final Direction NORMAL = SOUTH;
	
	static {
	    // Run after static init
	    for (Dir dir : Dir.VALUES) {
	        dir.buildCaches();
	    }
	}

	private @NonnullType Direction[] dirs;
	
	private @NonnullType BlockPos[] offsets = new BlockPos[6];

	Dir(Direction... dirs) {
		this.dirs = dirs;
    }
	
    private void buildCaches() {
        // Fill normalized dirs
        for (Direction normal : Direction.values()) {
            @NonnullType Direction[] normalized;
            if (normal == NORMAL) {
                normalized = dirs;
            } else if (normal == NORMAL.getOpposite()) {
                // If this is the opposite direction of the default normal, we
                // need to mirror the dirs
                // A mirror version does not affect y+ and y- so we ignore those
                Direction[] ret = new Direction[dirs.length];
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = dirs[i].getStepY() != 0 ? dirs[i] : dirs[i].getOpposite();
                }
                normalized = ret;
            } else {
                Direction axis;
                // Next, we need different a different rotation axis depending
                // on if this is up/down or not
                if (normal.getStepY() == 0) {
                    // If it is not up/down, pick either the left or right-hand
                    // rotation
                    axis = normal == NORMAL.getClockWise() ? UP : DOWN;
                } else {
                    // If it is up/down, pick either the up or down rotation.
                    axis = normal == UP ? NORMAL.getCounterClockWise() : NORMAL.getClockWise();
                }
                Direction[] ret = new Direction[dirs.length];
                // Finally apply all the rotations
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = rotate(dirs[i], axis);
                }
                normalized = ret;
            }
            BlockPos ret = BlockPos.ZERO;
            for (Direction dir : normalized) {
                ret = ret.relative(dir);
            }
            offsets[normal.ordinal()] = ret;
        }
	}

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
    public boolean isConnected(CTMLogic ctm, BlockGetter world, BlockPos pos, Direction side) {
        return ctm.isConnected(world, pos, applyConnection(pos, side), side);
    }

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
    public boolean isConnected(CTMLogic ctm, BlockGetter world, BlockPos pos, Direction side, BlockState state) {
        return ctm.isConnected(world, pos, applyConnection(pos, side), side, state);
    }

    /**
     * Apply this Dir to the given BlockPos for the given Direction normal direction.
     * 
     * @return The offset BlockPos
     */
    @SuppressWarnings("null")
    @Nonnull
    public BlockPos applyConnection(BlockPos pos, Direction side) {
        return pos.offset(getOffset(side));
    }

    public Dir relativize(Direction normal) {
        /*
        if (normal == NORMAL) {
            return this;
        } else if (normal == NORMAL.getOpposite()) {
            return getDirFor(getNormalizedDirs(normal));
        } else {
            if (dirs.length == 1) {
                if (normal.getAxis() == dirs[0].getAxis()) {
                    return null;
                } else {
                    return this;
                }
            }
        }
        */
        throw new UnsupportedOperationException("Yell at tterrag to finish deserialization");
    }
    
    @Nonnull
    public BlockPos getOffset(Direction normal) {
        return offsets[normal.ordinal()];
    }
	
	public @Nullable Dir getDirFor(Direction[] dirs) {
	    if (dirs == this.dirs) { // Short circuit for identical return from getNormalizedDirs
	        return this; 
	    }
	    
	    for (Dir dir : VALUES) {
	        if (Arrays.equals(dir.dirs, dirs)) {
	            return dir;
	        }
	    }
	    return null;
	}

	private Direction rotate(Direction facing, Direction axisFacing) {
        Direction.Axis axis = axisFacing.getAxis();
        AxisDirection axisDir = axisFacing.getAxisDirection();

        if (axisDir == AxisDirection.POSITIVE) {
            return DirectionHelper.rotateAround(facing, axis);
        }

        if (facing.getAxis() != axis) {
            switch (axis) {
            case X:
                // Inverted results from Direction#rotateX
                switch (facing) {
                case NORTH:
                    return UP;
                case DOWN:
                    return NORTH;
                case SOUTH:
                    return DOWN;
                case UP:
                    return SOUTH;
                default:
                    return facing; // Invalid but ignored
                }
            case Y:
                return facing.getCounterClockWise();
            case Z:
                // Inverted results from Direction#rotateZ
                switch (facing) {
                case EAST:
                    return EAST;
                case WEST:
                    return WEST;
                case UP:
                    return DOWN;
                case DOWN:
                    return UP;
                default:
                    return facing; // invalid but ignored
                }
            }
        }

        return facing;
	}
}