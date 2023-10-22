package team.chisel.ctm.client.newctm;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.client.newctm.CTMLogicBakery.OutputFace;

public interface ICTMLogic {
    
    int[] getSubmapIds(BlockAndTintGetter world, BlockPos pos, Direction side);
    
    OutputFace[] getSubmaps(BlockAndTintGetter world, BlockPos pos, Direction side);
    
    ILogicCache cached();
    
    List<ISubmap> outputSubmaps();
    
    int requiredTextures();

}
