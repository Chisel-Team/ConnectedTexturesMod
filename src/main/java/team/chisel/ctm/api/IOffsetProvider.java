package team.chisel.ctm.api;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IOffsetProvider {
    
    @Nonnull
    BlockPos getOffset(@Nonnull Level world, @Nonnull BlockPos pos);

}
