package team.chisel.ctm.client.util;

import com.google.common.annotations.VisibleForTesting;

import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import team.chisel.ctm.api.texture.ISubmap;

@RequiredArgsConstructor
public class NewCTMLogic implements ICTMLogic {
    
    @VisibleForTesting
    public final int[][] lookups;
    private final ISubmap[] tiles;
    private final LocalDirection[] directions;
    private final ConnectionCheck connectionCheck;
    
    private int[] cachedSubmapIds;
    private ISubmap[] cachedSubmaps;
    
    public ISubmap[] getCachedSubmaps() {
        return this.cachedSubmaps;
    }
    
    public ISubmap[] getSubmaps(BlockGetter world, BlockPos pos, Direction side) {
        int key = 0;
        for (int i = 0; i < directions.length; i++) {
            BlockPos conPos = pos.offset(directions[i].getOffset(side));
            // TODO decide endian-ness properly, my original spreadsheet started at the most significant bit, hence this hacky transformation of the bitshift value
            key |= (connectionCheck.isConnected(world, pos, conPos, side) ? 1 : 0) << (directions.length - i - 1);
        }
        if (key >= lookups.length) {
            throw new IllegalStateException("Input state found that is not in lookup table: " + Integer.toBinaryString(key));
        }
        // TODO reduce allocation by encapsulating multi-submap results
        int[] tileIds = lookups[key];
        ISubmap[] ret = new ISubmap[tileIds.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = tiles[tileIds[i]];
        }
        this.cachedSubmapIds = tileIds;
        this.cachedSubmaps = ret;
        return ret;
    }

    @Override
    public long serialized() {
        int stride = directions.length;
        int len = cachedSubmapIds.length;
        if (len * stride > 64) {
            throw new IllegalStateException("Too many submaps to serialize");
        }
        long ret = 0L;
        for (int i = 0; i < cachedSubmapIds.length; i++) {
            ret |= cachedSubmapIds[i] << (i * stride);
        }
        return ret;
    }

    @Override
    public void buildConnectionMap(BlockGetter world, BlockPos pos, Direction side) {
        getSubmaps(world, pos, side);
    }

    @Override
    public void buildConnectionMap(long data, Direction side) {
    }

    @Override
    public boolean connected(Dir dir) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean connectedAnd(Dir... dirs) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean connectedOr(Dir... dirs) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean connectedNone(Dir... dirs) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean connectedOnly(Dir... dirs) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int numConnections() {
        // TODO Auto-generated method stub
        return 0;
    }
}
