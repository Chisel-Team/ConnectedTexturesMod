package team.chisel.ctm.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public interface IOffsetProvider {
    
    @NotNull
    BlockPos getOffset(@NotNull Level world, @NotNull BlockPos pos);

}
