package team.chisel.ctm.client.texture.type;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTMV;
import team.chisel.ctm.client.texture.render.TextureCTMV;

@TextureType("CTMV")
public class TextureTypeCTMV implements ITextureType {

    @Override
    public ICTMTexture<TextureTypeCTMV> makeTexture(TextureInfo info) {
        return new TextureCTMV(this, info);
    }
    
    @Override
    public TextureContextCTMV getBlockRenderContext(IBlockState state, IBlockAccess world, BlockPos pos, ICTMTexture<?> tex) {
        return new TextureContextCTMV(world, pos);
    }
    
    @Override
    public int requiredTextures() {
        return 2;
    }

    @Override
    public ITextureContext getContextFromData(long data){
        return new TextureContextCTMV(data);
    }
}