package team.chisel.ctm.client.texture.ctx;

import java.util.EnumMap;

import javax.annotation.Nonnull;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;
import team.chisel.ctm.client.util.CTMLogic;
import team.chisel.ctm.client.util.RegionCache;

public class TextureContextCTM implements ITextureContext {
    
    private EnumMap<EnumFacing, CTMLogic> ctmData = new EnumMap<>(EnumFacing.class);

    private long data;

    public TextureContextCTM(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
        world = new RegionCache(pos, 1, world);
        for (EnumFacing face : EnumFacing.VALUES) {
            CTMLogic ctm = createCTM(state);
            ctm.createSubmapIndices(world, pos, face);
            ctmData.put(face, ctm);
            this.data |= ctm.serialized() << (face.ordinal() * 8);
        }
    }

    public TextureContextCTM(long data){
        this.data = data;
        for(EnumFacing face : EnumFacing.VALUES){
            CTMLogic ctm = createCTM(null); // FIXME
            ctm.createSubmapIndices(data, face);
            ctmData.put(face, ctm);
        }
    }
    
    protected CTMLogic createCTM(@Nonnull IBlockState state) {
        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);
        if (model instanceof AbstractCTMBakedModel) {
            return CTMLogic.getInstance().ignoreStates(((AbstractCTMBakedModel)model).getModel().ignoreStates());
        }
        return CTMLogic.getInstance();
    }

    public CTMLogic getCTM(EnumFacing face) {
        return ctmData.get(face);
    }

    @Override
    public long getCompressedData(){
        return this.data;
    }
}
