package team.chisel.ctm.tests;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class TestBlockGetter implements BlockGetter {
    
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    
    void addBlock(BlockPos pos, BlockState state) {
        blocks.put(pos, state);
    }
    
    void removeBlock(BlockPos pos) {
        this.blocks.remove(pos);
    }

    @Override
    public int getHeight() {
        return 8;
    }

    @Override
    public int getMinBuildHeight() {
        return 0;
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return Fluids.EMPTY.defaultFluidState(); 
    }

}
