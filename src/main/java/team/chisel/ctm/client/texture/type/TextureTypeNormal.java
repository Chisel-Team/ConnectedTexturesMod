package team.chisel.ctm.client.texture.type;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
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
    
    @TextureType("normal")
    INSTANCE;
    
    @Nonnull
    private static final ITextureContext EMPTY_CONTEXT = () -> 0L;

    @Override
    public ICTMTexture<TextureTypeNormal> makeTexture(TextureInfo info){
        return new TextureNormal(this, info);
    }

    @Override
    public ITextureContext getBlockRenderContext(BlockState state, BlockAndTintGetter world, BlockPos pos, ICTMTexture<?> tex){
        return EMPTY_CONTEXT;
    }

    @Override
    public ITextureContext getContextFromData(long data){
        return EMPTY_CONTEXT;
    }
}
