package team.chisel.ctm.client.util;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FaceOffset {
    public static BlockPos getBlockPosOffsetFromFaceOffset(EnumFacing facing, int xOffset, int yOffset) {
        switch (facing) {
            default: // UP
                return new BlockPos(xOffset, 0, -yOffset);
            case DOWN:
                return new BlockPos(xOffset, 0, yOffset);
            case NORTH:
                return new BlockPos(-xOffset, yOffset, 0);
            case SOUTH:
                return new BlockPos(xOffset, yOffset, 0);
            case WEST:
                return new BlockPos(0, yOffset, xOffset);
            case EAST:
                return new BlockPos(0, yOffset, -xOffset);
        }
    }
}