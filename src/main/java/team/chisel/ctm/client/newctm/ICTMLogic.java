package team.chisel.ctm.client.newctm;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import org.jetbrains.annotations.Nullable;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.client.newctm.CTMLogicBakery.OutputFace;
import team.chisel.ctm.client.util.Submap;

public interface ICTMLogic {
    
    int[] getSubmapIds(BlockAndTintGetter world, BlockPos pos, Direction side);
    
    OutputFace[] getSubmaps(BlockAndTintGetter world, BlockPos pos, Direction side);
    
    ILogicCache cached(@Nullable ConnectionCheck connectionCheck);
    
    List<ISubmap> outputSubmaps();

    default ISubmap getFallbackUvs() {
        return Submap.X1;
    }
    
    int requiredTextures();

}
