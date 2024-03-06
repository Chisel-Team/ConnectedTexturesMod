package team.chisel.ctm.client.util;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import team.chisel.ctm.client.newctm.LocalDirection;

/**
 * Represents all the different spot for connection locations for a ctm block
 */
@ParametersAreNonnullByDefault
public enum ConnectionLocations {

    UP(Dir.TOP),
    DOWN(Dir.BOTTOM),
    NORTH(Direction.EAST, Dir.RIGHT),
    SOUTH(Direction.EAST, Dir.LEFT),
    EAST(Dir.RIGHT),
    WEST(Dir.LEFT),
    
    NORTH_EAST(Direction.UP, Dir.TOP_RIGHT),
    NORTH_WEST(Direction.UP, Dir.TOP_LEFT),
    SOUTH_EAST(Direction.UP, Dir.BOTTOM_RIGHT),
    SOUTH_WEST(Direction.UP, Dir.BOTTOM_LEFT),
    
    NORTH_UP(Direction.EAST, Dir.TOP_RIGHT),
    NORTH_DOWN(Direction.EAST, Dir.BOTTOM_RIGHT),
    SOUTH_UP(Direction.EAST, Dir.TOP_LEFT),
    SOUTH_DOWN(Direction.EAST, Dir.BOTTOM_LEFT),
    
    EAST_UP(Dir.TOP_RIGHT),
    EAST_DOWN(Dir.BOTTOM_RIGHT),
    WEST_UP(Dir.TOP_LEFT),
    WEST_DOWN(Dir.BOTTOM_LEFT),
    
    NORTH_EAST_UP(Direction.EAST, Dir.TOP_RIGHT, true),
    NORTH_EAST_DOWN(Direction.EAST, Dir.BOTTOM_RIGHT, true),
    
    SOUTH_EAST_UP(Direction.EAST, Dir.TOP_LEFT, true),
    SOUTH_EAST_DOWN(Direction.EAST, Dir.BOTTOM_LEFT, true),
    
    SOUTH_WEST_UP(Direction.WEST, Dir.TOP_LEFT, true),
    SOUTH_WEST_DOWN(Direction.WEST, Dir.BOTTOM_LEFT, true),
    
    NORTH_WEST_UP(Direction.WEST, Dir.TOP_RIGHT, true),
    NORTH_WEST_DOWN(Direction.WEST, Dir.BOTTOM_RIGHT, true),
    
    UP_UP(Direction.UP, null, true),
    DOWN_DOWN(Direction.DOWN, null, true),
    NORTH_NORTH(Direction.NORTH, null, true),
    SOUTH_SOUTH(Direction.SOUTH, null, true),
    EAST_EAST(Direction.EAST, null, true),
    WEST_WEST(Direction.WEST, null, true),
    
    ;
    
    public static final ConnectionLocations[] VALUES = values();
    
    /**
     * The enum facing directions needed to get to this connection location
     */
    private final Direction normal;
    private final @Nullable LocalDirection dir;
    private boolean offset;

    ConnectionLocations(@Nullable LocalDirection dir) {
        this(Direction.SOUTH, dir);
    }
    
    ConnectionLocations(Direction normal, @Nullable LocalDirection dir){
        this(normal, dir, false);
    }
    
    ConnectionLocations(Direction normal, @Nullable LocalDirection dir, boolean offset) {
        this.normal = normal;
        this.dir = dir;
        this.offset = offset;
    }

    public @Nullable LocalDirection getDirForSide(Direction facing){
        return dir == null ? null : dir.relativize(facing);
    }

    public @Nullable Direction clipOrDestroy(Direction direction) {
        throw new UnsupportedOperationException();
//        Direction[] dirs = dir == null ? new Direction[] {normal, normal} : dir.getNormalizedDirs(direction);
//        if (dirs[0] == direction) {
//            return dirs.length > 1 ? dirs[1] : null;
//        } else if (dirs.length > 1 && dirs[1] == direction) {
//            return dirs[0];
//        } else {
//            return null;
//        }
    }

    @SuppressWarnings("null")
    public BlockPos transform(BlockPos pos) {
        if (dir != null) {
            pos = pos.offset(dir.getOffset(normal));
        } else {
            pos = pos.relative(normal);
        }

        if (offset) {
            pos = pos.relative(normal);
        }
        return pos;
    }

    public static ConnectionLocations fromFacing(Direction facing){
        switch (facing){
            case NORTH: return NORTH;
            case SOUTH: return SOUTH;
            case EAST: return EAST;
            case WEST: return WEST;
            case UP: return UP;
            case DOWN: return DOWN;
            default: return NORTH;
        }
    }

    public static Direction toFacing(ConnectionLocations loc){
        switch (loc){
            case NORTH: return Direction.NORTH;
            case SOUTH: return Direction.SOUTH;
            case EAST: return Direction.EAST;
            case WEST: return Direction.WEST;
            case UP: return Direction.UP;
            case DOWN: return Direction.DOWN;
            default: return Direction.NORTH;
        }
    }

    public static List<ConnectionLocations> decode(long data) {
        List<ConnectionLocations> list = new ArrayList<>();
        for (ConnectionLocations loc : values()) {
            if ((1 & (data >> loc.ordinal())) != 0) {
                list.add(loc);
            }
        }
        return list;
    }

    public long getMask() {
        return 1 << ordinal();
    }
}