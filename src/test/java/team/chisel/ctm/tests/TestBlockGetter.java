package team.chisel.ctm.tests;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class TestBlockGetter implements BlockAndTintGetter {
    
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private final LevelLightEngine lightEngine;

    public TestBlockGetter() {
        this.lightEngine = new LevelLightEngine(new LightChunkGetter() {
            @Nullable
            @Override
            public LightChunk getChunkForLighting(int chunkX, int chunkZ) {
                return null;
            }

            @Override
            public BlockGetter getLevel() {
                return TestBlockGetter.this;
            }
        }, false, false);
    }
    
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

    @Override
    public float getShade(Direction direction, boolean shade) {
        return 0;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return lightEngine;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        return 0;
    }
}
