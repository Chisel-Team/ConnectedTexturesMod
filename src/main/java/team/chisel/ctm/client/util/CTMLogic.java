package team.chisel.ctm.client.util;

import static team.chisel.ctm.client.util.Dir.BOTTOM;
import static team.chisel.ctm.client.util.Dir.BOTTOM_LEFT;
import static team.chisel.ctm.client.util.Dir.BOTTOM_RIGHT;
import static team.chisel.ctm.client.util.Dir.LEFT;
import static team.chisel.ctm.client.util.Dir.RIGHT;
import static team.chisel.ctm.client.util.Dir.TOP;
import static team.chisel.ctm.client.util.Dir.TOP_LEFT;
import static team.chisel.ctm.client.util.Dir.TOP_RIGHT;

import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.client.newctm.CTMLogicBakery.OutputFace;
import team.chisel.ctm.client.newctm.ConnectionCheck;
import team.chisel.ctm.client.newctm.ICTMLogic;
import team.chisel.ctm.client.newctm.ILogicCache;
import team.chisel.ctm.client.newctm.LocalDirection;

// @formatter:off
/**
 * The CTM renderer will draw the block's FACE using by assembling 4 quadrants from the 5 available block
 * textures.  The normal Texture.png is the blocks "unconnected" texture, and is used when CTM is disabled or the block
 * has nothing to connect to.  This texture has all of the outside corner quadrants  The texture-ctm.png contains the
 * rest of the quadrants.
 * <pre>
 * ┌─────────────────┐ ┌────────────────────────────────┐
 * │ texture.png     │ │ texture-ctm.png                │
 * │ ╔══════╤══════╗ │ │  ──────┼────── ║ ─────┼───── ║ │
 * │ ║      │      ║ │ │ │      │      │║      │      ║ │
 * │ ║ 16   │ 17   ║ │ │ │ 0    │ 1    │║ 2    │ 3    ║ │
 * │ ╟──────┼──────╢ │ │ ┼──────┼──────┼╟──────┼──────╢ │
 * │ ║      │      ║ │ │ │      │      │║      │      ║ │
 * │ ║ 18   │ 19   ║ │ │ │ 4    │ 5    │║ 6    │ 7    ║ │
 * │ ╚══════╧══════╝ │ │  ──────┼────── ║ ─────┼───── ║ │
 * └─────────────────┘ │ ═══════╤═══════╝ ─────┼───── ╚ │
 *                     │ │      │      ││      │      │ │
 *                     │ │ 8    │ 9    ││ 10   │ 11   │ │
 *                     │ ┼──────┼──────┼┼──────┼──────┼ │
 *                     │ │      │      ││      │      │ │
 *                     │ │ 12   │ 13   ││ 14   │ 15   │ │
 *                     │ ═══════╧═══════╗ ─────┼───── ╔ │
 *                     └────────────────────────────────┘
 * </pre>
 * combining { 18, 13,  9, 16 }, we can generate a texture connected to the right!
 * <pre>
 * ╔══════╤═══════
 * ║      │      │
 * ║ 16   │ 9    │
 * ╟──────┼──────┼
 * ║      │      │
 * ║ 18   │ 13   │
 * ╚══════╧═══════
 * </pre>
 *
 * combining { 18, 13, 11,  2 }, we can generate a texture, in the shape of an L (connected to the right, and up
 * <pre>
 * ║ ─────┼───── ╚
 * ║      │      │
 * ║ 2    │ 11   │
 * ╟──────┼──────┼
 * ║      │      │
 * ║ 18   │ 13   │
 * ╚══════╧═══════
 * </pre>
 *
 * HAVE FUN!
 * -CptRageToaster-
 */
@ParametersAreNonnullByDefault
@Accessors(fluent = true, chain = true)
public class CTMLogic implements ICTMLogic, ILogicCache {
    
    public interface StateComparisonCallback {
        
        public static final StateComparisonCallback DEFAULT = 
                (ctm, from, to, dir) -> ctm.ignoreStates() ? from.getBlock() == to.getBlock() : from == to;
        
        boolean connects(ConnectionCheck instance, BlockState from, BlockState to, Direction dir);
    }
	
    /**
     * The Uvs for the specific "magic number" value
     */
    public static final ISubmap[] uvs = new ISubmap[]{
            //Ctm texture
            Submap.fromPixelScale(4, 4, 0, 0),   // 0
            Submap.fromPixelScale(4, 4, 4, 0),   // 1
            Submap.fromPixelScale(4, 4, 8, 0),   // 2
            Submap.fromPixelScale(4, 4, 12, 0),  // 3
            Submap.fromPixelScale(4, 4, 0, 4),   // 4
            Submap.fromPixelScale(4, 4, 4, 4),   // 5
            Submap.fromPixelScale(4, 4, 8, 4),   // 6
            Submap.fromPixelScale(4, 4, 12, 4),  // 7
            Submap.fromPixelScale(4, 4, 0, 8),   // 8
            Submap.fromPixelScale(4, 4, 4, 8),   // 9
            Submap.fromPixelScale(4, 4, 8, 8),   // 10
            Submap.fromPixelScale(4, 4, 12, 8),  // 11
            Submap.fromPixelScale(4, 4, 0, 12),  // 12
            Submap.fromPixelScale(4, 4, 4, 12),  // 13
            Submap.fromPixelScale(4, 4, 8, 12),  // 14
            Submap.fromPixelScale(4, 4, 12, 12), // 15
            // Default texture
            Submap.fromPixelScale(8, 8, 0, 0),   // 16
            Submap.fromPixelScale(8, 8, 8, 0),   // 17
            Submap.fromPixelScale(8, 8, 0, 8),   // 18
            Submap.fromPixelScale(8, 8, 8, 8)    // 19
    };
    
    public static final ISubmap FULL_TEXTURE = Submap.X1;
    
    // @formatter:on

	/** Some hardcoded offset values for the different corner indeces */
	protected static int[] submapOffsets = { 4, 5, 1, 0 };

	// TODO encapsulate
	public ConnectionCheck connectionCheck = new ConnectionCheck();
	
    // Mapping the different corner indeces to their respective dirs
	protected static final Dir[][] submapMap = new Dir[][] {
	    { BOTTOM, LEFT, BOTTOM_LEFT },
	    { BOTTOM, RIGHT, BOTTOM_RIGHT },
	    { TOP, RIGHT, TOP_RIGHT },
	    { TOP, LEFT, TOP_LEFT }
	};
	
	protected byte connectionMap;
	protected int[] submapCache = new int[] { 18, 19, 17, 16 };


	public static CTMLogic getInstance() {
		return new CTMLogic();
	}

	/**
	 * @return The indeces of the typical 4x4 submap to use for the given face at the given location.
	 * 
	 *         Indeces are in counter-clockwise order starting at bottom left.
	 */
	@Override
    public int[] getSubmapIds(@Nullable BlockAndTintGetter world, BlockPos pos, Direction side) {
		if (world == null) {
            return submapCache;
        }

		buildConnectionMap(world, pos, side);

		// Map connections to submap indeces
		for (int i = 0; i < 4; i++) {
			fillSubmaps(i);
		}

		return submapCache;
	}

	public int[] createSubmapIndices(long data, Direction side){
		submapCache = new int[] { 18, 19, 17, 16 };

		buildConnectionMap(data, side);

		// Map connections to submap indeces
		for (int i = 0; i < 4; i++) {
			fillSubmaps(i);
		}

		return submapCache;
	}
    
    public int[] getSubmapIndices() {
        return submapCache;
    }
    
    @Override
    public long serialized() {
        return Byte.toUnsignedLong(connectionMap);
    }
	
    public static boolean isDefaultTexture(int id) {
        return (id == 16 || id == 17 || id == 18 || id == 19);
    }
    
    protected void setConnectedState(LocalDirection dir, boolean connected) {
        connectionMap = setConnectedState(connectionMap, dir, connected);
    }
    
    private static byte setConnectedState(byte map, LocalDirection dir, boolean connected) {
        if (connected) {
            return (byte) (map | (1 << dir.ordinal()));
        } else {
            return (byte) (map & ~(1 << dir.ordinal()));
        }
    }

    /**
     * Builds the connection map and stores it in this CTM instance. The {@link #connected(Dir)}, {@link #connectedAnd(Dir...)}, and {@link #connectedOr(Dir...)} methods can be used to access it.
     */
    @Override
    public void buildConnectionMap(BlockAndTintGetter world, BlockPos pos, Direction side) {
        //BlockState state = connectionCheck.getConnectionState(world, pos, side, pos);
        // TODO this naive check doesn't work for models that have unculled faces.
        // Perhaps a smarter optimization could be done eventually?
//        if (state.shouldSideBeRendered(world, pos, side)) {
            for (Dir dir : Dir.VALUES) {
                //Note: We can't cache the state that we are checking about connection for as we want to ensure that
                // we can take into account the side of the block we want to know the "state" of as if the block is
                // a facade of some sort it might return different results based on where it is being queried from
                setConnectedState(dir, dir.isConnected(connectionCheck, world, pos, side));
            }
//        }
    }

    public void buildConnectionMap(long data, Direction side) {
        connectionMap = 0; // Clear all connections
        List<ConnectionLocations> connections = ConnectionLocations.decode(data);
        for (ConnectionLocations loc : connections) {
            if (loc.getDirForSide(side) != null) {
                LocalDirection dir = loc.getDirForSide(side);
                if (dir != null) {
                    setConnectedState(dir, true);
                }
            }
        }
    }

	@SuppressWarnings("null")
    protected void fillSubmaps(int idx) {
		Dir[] dirs = submapMap[idx];
		if (connectedOr(dirs[0], dirs[1])) {
			if (connectedAnd(dirs)) {
				// If all dirs are connected, we use the fully connected face,
				// the base offset value.
			    submapCache[idx] = submapOffsets[idx];
			} else {
				// This is a bit magic-y, but basically the array is ordered so
				// the first dir requires an offset of 2, and the second dir
				// requires an offset of 8, plus the initial offset for the
				// corner.
			    submapCache[idx] = submapOffsets[idx] + (connected(dirs[0]) ? 2 : 0) + (connected(dirs[1]) ? 8 : 0);
			}
		}
	}

	/**
	 * @param dir
	 *            The direction to check connection in.
	 * @return True if the cached connectionMap holds a connection in this {@link Dir direction}.
	 */
    public boolean connected(Dir dir) {
		return ((connectionMap >> dir.ordinal()) & 1) == 1;
	}

	/**
	 * @param dirs
	 *            The directions to check connection in.
	 * @return True if the cached connectionMap holds a connection in <i><b>all</b></i> the given {@link Dir directions}.
	 */
    @SuppressWarnings("null")
    public boolean connectedAnd(Dir... dirs) {
		for (Dir dir : dirs) {
			if (!connected(dir)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param dirs
	 *            The directions to check connection in.
	 * @return True if the cached connectionMap holds a connection in <i><b>one of</b></i> the given {@link Dir directions}.
	 */
    @SuppressWarnings("null")
    public boolean connectedOr(Dir... dirs) {
		for (Dir dir : dirs) {
			if (connected(dir)) {
				return true;
			}
		}
		return false;
    }
	
    public boolean connectedNone(Dir... dirs) {
	    for (Dir dir : dirs) {
	        if (connected(dir)) {
	            return false;
	        }
	    }
	    return true;
	}
	
    public boolean connectedOnly(Dir... dirs) {
	    byte map = 0;
	    for (Dir dir : dirs) {
	        map = setConnectedState(map, dir, true);
	    }
	    return map == this.connectionMap;
	}
	
    public int numConnections() {
	    return Integer.bitCount(connectionMap);
	}

    @Override
    @Deprecated
    public OutputFace[] getCachedSubmaps() {
        return new OutputFace[0];
    }

    @Override
    @Deprecated
    public OutputFace[] getSubmaps(BlockAndTintGetter world, BlockPos pos, Direction side) {
        return new OutputFace[0];
    }
    
    @Override
    @Deprecated
    public ILogicCache cached(@Nullable ConnectionCheck connectionCheck) {
        return this;
    }
    
    @Override
    public List<ISubmap> outputSubmaps() {
        return Arrays.stream(Submap.X2).flatMap(Arrays::stream).toList();
    }
    
    @Override
    public int requiredTextures() {
        return 2;
    }
}