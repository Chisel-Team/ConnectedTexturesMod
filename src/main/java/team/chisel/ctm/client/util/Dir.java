package team.chisel.ctm.client.util;

import static net.minecraft.util.EnumFacing.DOWN;
import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.NORTH;
import static net.minecraft.util.EnumFacing.SOUTH;
import static net.minecraft.util.EnumFacing.UP;
import static net.minecraft.util.EnumFacing.WEST;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
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
	private static final EnumFacing NORMAL = SOUTH;
	
	static {
	    // Run after static init
	    for (Dir dir : Dir.VALUES) {
	        dir.buildCaches();
	    }
	}

	private @NonnullType EnumFacing[] dirs;
	
	private @NonnullType BlockPos[] offsets = new BlockPos[6];

	private Dir(EnumFacing... dirs) {
		this.dirs = dirs;
    }
	
    private void buildCaches() {
        // Fill normalized dirs
        for (EnumFacing normal : EnumFacing.values()) {
            @NonnullType EnumFacing[] normalized;
            if (normal == NORMAL) {
                normalized = dirs;
            } else if (normal == NORMAL.getOpposite()) {
                // If this is the opposite direction of the default normal, we
                // need to mirror the dirs
                // A mirror version does not affect y+ and y- so we ignore those
                EnumFacing[] ret = new EnumFacing[dirs.length];
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = dirs[i].getYOffset() != 0 ? dirs[i] : dirs[i].getOpposite();
                }
                normalized = ret;
            } else {
                EnumFacing axis;
                // Next, we need different a different rotation axis depending
                // on if this is up/down or not
                if (normal.getYOffset() == 0) {
                    // If it is not up/down, pick either the left or right-hand
                    // rotation
                    axis = normal == NORMAL.rotateY() ? UP : DOWN;
                } else {
                    // If it is up/down, pick either the up or down rotation.
                    axis = normal == UP ? NORMAL.rotateYCCW() : NORMAL.rotateY();
                }
                EnumFacing[] ret = new EnumFacing[dirs.length];
                // Finally apply all the rotations
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = rotate(dirs[i], axis);
                }
                normalized = ret;
            }
            BlockPos ret = BlockPos.ORIGIN;
            for (EnumFacing dir : normalized) {
                ret = ret.offset(dir);
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
    public boolean isConnected(CTMLogic ctm, IBlockReader world, BlockPos pos, EnumFacing side) {
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
    public boolean isConnected(CTMLogic ctm, IBlockReader world, BlockPos pos, EnumFacing side, IBlockState state) {
        return ctm.isConnected(world, pos, applyConnection(pos, side), side, state);
    }

    /**
     * Apply this Dir to the given BlockPos for the given EnumFacing normal direction.
     * 
     * @return The offset BlockPos
     */
    @SuppressWarnings("null")
    @Nonnull
    public BlockPos applyConnection(BlockPos pos, EnumFacing side) {
        return pos.add(getOffset(side));
    }

    public Dir relativize(EnumFacing normal) {
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
    public BlockPos getOffset(EnumFacing normal) {
        return offsets[normal.ordinal()];
    }
	
	public @Nullable Dir getDirFor(EnumFacing[] dirs) {
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

	private EnumFacing rotate(EnumFacing facing, EnumFacing axisFacing) {
        Axis axis = axisFacing.getAxis();
        AxisDirection axisDir = axisFacing.getAxisDirection();

        if (axisDir == AxisDirection.POSITIVE) {
            return facing.rotateAround(axis);
        }

        if (facing.getAxis() != axis) {
            switch (axis) {
            case X:
                // Inverted results from EnumFacing#rotateX
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
                return facing.rotateYCCW();
            case Z:
                // Inverted results from EnumFacing#rotateZ
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