package team.chisel.ctm.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.jetbrains.annotations.NotNull;

public interface IOffsetProvider {
    
    @NotNull
    BlockPos getOffset(@NotNull BlockAndTintGetter world, @NotNull BlockPos pos);

}
