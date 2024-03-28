package team.chisel.ctm.client.util;

import java.lang.ref.WeakReference;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelDataManager;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used by render state creation to avoid unnecessary block lookups through the world.
 */
@ParametersAreNonnullByDefault
public class RegionCache implements BlockAndTintGetter {

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
    
    private WeakReference<BlockAndTintGetter> passthrough;
    private final Long2ObjectMap<BlockState> stateCache = new Long2ObjectOpenHashMap<>();

    public RegionCache(BlockPos center, int radius, @Nullable BlockAndTintGetter passthrough) {
        this.center = center;
        this.radius = radius;
        this.passthrough = new WeakReference<>(passthrough);
    }
    
    private BlockAndTintGetter getPassthrough() {
        BlockAndTintGetter ret = passthrough.get();
        Preconditions.checkNotNull(ret);
        return ret;
    }
    
    public @NotNull RegionCache updateWorld(BlockAndTintGetter passthrough) {
        // We do NOT use getPassthrough() here so as to skip the null-validation - it's obviously valid to be null here
        if (this.passthrough.get() != passthrough) {
            stateCache.clear();
            this.passthrough = new WeakReference<>(passthrough);
        }
        return this;
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        return getPassthrough().getBlockEntity(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        long address = pos.asLong();
        var state = stateCache.get(address);

        if (state == null) {
            state = getPassthrough().getBlockState(pos);
            stateCache.put(address, state);
        }

        return state;
    }

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getPassthrough().getFluidState(pos);
	}

    @Override
    public int getHeight() {
        return getPassthrough().getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return getPassthrough().getMinBuildHeight();
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return getPassthrough().getShade(direction, shade);
    }

    @Override
    public float getShade(float normalX, float normalY, float normalZ, boolean shade) {
        return getPassthrough().getShade(normalX, normalY, normalZ, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return getPassthrough().getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        return getPassthrough().getBlockTint(pos, colorResolver);
    }

    @Nullable
    @Override
    public ModelDataManager getModelDataManager() {
        //Note: While we don't have to override this method for purposes of compilation of the interface implementation
        // we need to make sure to provide the model data manager so that mods that may query it from IForgeBlock#getAppearance
        // can get their block's model data appropriately
        return getPassthrough().getModelDataManager();
    }

    @Nullable
    @Override
    public AuxiliaryLightManager getAuxLightManager(ChunkPos pos) {
        return getPassthrough().getAuxLightManager(pos);
    }
}
