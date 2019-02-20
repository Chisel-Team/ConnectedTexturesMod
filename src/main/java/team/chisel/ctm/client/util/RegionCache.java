package team.chisel.ctm.client.util;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

/**
 * Used by render state creation to avoid unnecessary block lookups through the world.
 */
@ParametersAreNonnullByDefault
public class RegionCache implements IBlockReader {

    /*
     * XXX
     * 
     * These are required for future use, in case there is ever a need to have this region cache only store a certain area of the world.
     * 
     * Currently, this class is only used by CTM, which is limited to a very small subsection of the world, 
     * and thus the overhead of distance checking is unnecessary.
     */
    @SuppressWarnings("unused")
    private final BlockPos center;
    @SuppressWarnings("unused")
    private final int radius;
    
    private WeakReference<IBlockReader> passthrough;
    private final Long2ObjectMap<IBlockState> stateCache = new Long2ObjectOpenHashMap<>();

    public RegionCache(BlockPos center, int radius, @Nullable IBlockReader passthrough) {
        this.center = center;
        this.radius = radius;
        this.passthrough = new WeakReference<>(passthrough);
    }
    
    private IBlockReader getPassthrough() {
        IBlockReader ret = passthrough.get();
        Preconditions.checkNotNull(ret);
        return ret;
    }
    
    public @Nonnull RegionCache updateWorld(IBlockReader passthrough) {
        // We do NOT use getPassthrough() here so as to skip the null-validation - it's obviously valid to be null here
        if (this.passthrough.get() != passthrough) {
            stateCache.clear();
        }
        this.passthrough = new WeakReference<>(passthrough);
        return this;
    }

    @Override
    @Nullable
    public TileEntity getTileEntity(BlockPos pos) {
        return getPassthrough().getTileEntity(pos);
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        // In cases with direct passthroughs, these are never used by our code.
        // But in case something out there does use them, this will work
        return getPassthrough().getCombinedLight(pos, lightValue);
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        long address = pos.toLong();
        IBlockState ret = stateCache.get(address);
        if (ret == null) {
            stateCache.put(address, ret = getPassthrough().getBlockState(pos));
        }
        return ret;
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        IBlockState state = getBlockState(pos);
        return state.getBlock().isAir(state, this, pos);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return getPassthrough().getBiome(pos);
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return getPassthrough().getStrongPower(pos, direction);
    }

    @Override
    public WorldType getWorldType() {
        return getPassthrough().getWorldType();
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        return getPassthrough().isSideSolid(pos, side, _default);
    }
}
