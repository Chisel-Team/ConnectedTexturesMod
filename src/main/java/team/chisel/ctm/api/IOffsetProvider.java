package team.chisel.ctm.api;

import javax.annotation.Nonnull;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public interface IOffsetProvider {
    
    @Nonnull
    public BlockPos getOffset(@Nonnull World world, @Nonnull BlockPos pos);

}
