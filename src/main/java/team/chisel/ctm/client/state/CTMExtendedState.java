package team.chisel.ctm.client.state;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;

import lombok.Getter;
import lombok.experimental.Delegate;
import net.minecraft.block.state.BlockStateBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.state.IProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.util.RenderContextList;
import team.chisel.ctm.client.util.ProfileUtil;

@ParametersAreNonnullByDefault
public class CTMExtendedState extends BlockStateBase implements IExtendedBlockState {

    interface Exclusions {
        public <T extends Comparable<T>> T getValue(IProperty<T> property);
        
        public <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property, V value);
        
        public <T extends Comparable<T>> IBlockState cycleProperty(IProperty<T> property);
    }
    
    @Delegate(excludes = Exclusions.class)
    private final IBlockState wrapped;
    private final IBlockState clean;
    
    private final boolean extended;
    private final @Nullable IExtendedBlockState extState;
    
    @Getter
    private final IBlockReader world;
    @Getter
    private final BlockPos pos;
    
    private @Nullable RenderContextList ctxCache;
    
    @SuppressWarnings("null")
    public CTMExtendedState(IBlockState state, IBlockReader world, BlockPos pos) {
        ProfileUtil.start("ctm_extended_state");
        this.wrapped = state;
        this.world = world;
        this.pos = pos;
        
        this.extended = wrapped instanceof IExtendedBlockState;
        if (extended) {
            extState = (IExtendedBlockState) wrapped;
            clean = extState.getClean();
        } else {
            extState = null;
            clean = wrapped;
        }
        ProfileUtil.end();
    }

    public CTMExtendedState(IBlockState state, CTMExtendedState parent) {
        this(state, parent.world, parent.pos);
    }
    
    public RenderContextList getContextList(IBlockState state, IModelCTM model) {
        if (ctxCache == null) {
            ctxCache = new RenderContextList(state, model.getCTMTextures(), world, pos);
        }
        return ctxCache;
    }

    @Override
    public @Nullable Collection<IUnlistedProperty<?>> getUnlistedNames() {
        return extended ? extState.getUnlistedNames() : Collections.emptyList();
    }

    @Override
    public @Nullable <V> V getValue(@Nullable IUnlistedProperty<V> property) {
        return extended ? extState.getValue(property) : null;
    }

    @Override
    public <V> IExtendedBlockState withProperty(@Nullable IUnlistedProperty<V> property, @Nullable V value) {
        return extended ? new CTMExtendedState(extState.withProperty(property, value), this) : this;
    }

    @Override
    public @Nullable ImmutableMap<IUnlistedProperty<?>, Optional<?>> getUnlistedProperties() {
        return extended ? extState.getUnlistedProperties() : ImmutableMap.of();
    }

    @Override
    public IBlockState getClean() {
        return clean;
    }
    
    // Lombok chokes on these for some reason

    @Override
    public <T extends Comparable<T>> T getValue(IProperty<T> property) {
        return wrapped.getValue(property);
    }

    @Override
    public <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property, V value) {
        return new CTMExtendedState(wrapped.withProperty(property, value), this);
    }

    @Override
    public <T extends Comparable<T>> IBlockState cycleProperty(IProperty<T> property) {
        return new CTMExtendedState(wrapped.cycleProperty(property), this);
    }
}
