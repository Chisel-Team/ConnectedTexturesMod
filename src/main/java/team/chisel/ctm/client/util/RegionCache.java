package team.chisel.ctm.client.util;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

/**
 * Used by render state creation to avoid unnecessary block lookups through the world.
 */
@ParametersAreNonnullByDefault
public class RegionCache implements IBlockAccess {

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

    //Wrapped in a WeakReference so this can actually be garbage collected
    // otherwise the WeakHashMap holding RegionCache instances will never clear 
    // as there is always a non weak reference to the world object.
    private final WeakReference<IBlockAccess> passthrough;
    private final Function<BlockPos, IBlockState> lookupFunc;
    private final Map<BlockPos, IBlockState> stateCache = new HashMap<>();

    public RegionCache(BlockPos center, int radius, IBlockAccess passthrough) {
        this.center = center;
        this.radius = radius;
        this.passthrough = new WeakReference<>(passthrough);
        //This is unsafe, but clean.
        this.lookupFunc = pos -> this.passthrough.get().getBlockState(pos);
    }

    @Override
    @Nullable
    public TileEntity getTileEntity(BlockPos pos) {
        IBlockAccess passthrough = this.passthrough.get();
        return passthrough != null ? passthrough.getTileEntity(pos) : null;
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        IBlockAccess passthrough = this.passthrough.get();
        // In cases with direct passthroughs, these are never used by our code.
        // But in case something out there does use them, this will work
        return passthrough != null ? passthrough.getCombinedLight(pos, lightValue) : 0;
    }

    @SuppressWarnings("null")
    @Override
    public IBlockState getBlockState(BlockPos pos) {
        IBlockAccess passthrough = this.passthrough.get();
        return passthrough != null ? stateCache.computeIfAbsent(pos, lookupFunc) : Blocks.AIR.getDefaultState();
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        IBlockAccess passthrough = this.passthrough.get();
        IBlockState state = getBlockState(pos);
        return passthrough == null || state.getBlock().isAir(state, this, pos);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        IBlockAccess passthrough = this.passthrough.get();
        return passthrough != null ? passthrough.getBiome(pos) : Biomes.PLAINS;
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        IBlockAccess passthrough = this.passthrough.get();
        return passthrough != null ? passthrough.getStrongPower(pos, direction) : 0;
    }

    @Override
    public WorldType getWorldType() {
        IBlockAccess passthrough = this.passthrough.get();
        return passthrough != null ? passthrough.getWorldType() : WorldType.DEFAULT;
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        IBlockAccess passthrough = this.passthrough.get();
        return passthrough != null && passthrough.isSideSolid(pos, side, _default);
    }
}