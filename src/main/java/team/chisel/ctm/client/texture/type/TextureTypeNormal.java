package team.chisel.ctm.client.texture.type;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.render.TextureNormal;

/**
 * Normal Block Render Type
 */
public enum TextureTypeNormal implements ITextureType {
    
    @TextureType("NORMAL")
    INSTANCE;

    @Override
    public ICTMTexture<TextureTypeNormal> makeTexture(TextureInfo info){
        return new TextureNormal(this, info);
    }

    @Override
    public ITextureContext getBlockRenderContext(IBlockState state, IBlockAccess world, BlockPos pos, ICTMTexture<?> tex){
        return null;
    }

    @Override
    public ITextureContext getContextFromData(long data){
        return null;
    }
}
