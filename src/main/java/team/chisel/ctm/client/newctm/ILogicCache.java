package team.chisel.ctm.client.newctm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import team.chisel.ctm.client.newctm.CTMLogicBakery.OutputFace;

public interface ILogicCache {

    OutputFace[] getCachedSubmaps();

    long serialized();

    /**
     * Builds the connection map and stores it in this CTM instance.
     */
    void buildConnectionMap(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction side);
}
