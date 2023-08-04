package team.chisel.ctm.client.newctm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import team.chisel.ctm.client.newctm.CTMLogicBakery.OutputFace;
import team.chisel.ctm.client.util.Dir;

public interface ILogicCache {

    OutputFace[] getCachedSubmaps();

    long serialized();

    /**
     * Builds the connection map and stores it in this CTM instance.
     */
    void buildConnectionMap(BlockGetter world, BlockPos pos, Direction side);
}
