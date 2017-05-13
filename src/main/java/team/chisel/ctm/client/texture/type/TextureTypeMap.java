package team.chisel.ctm.client.texture.type;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextPosition;
import team.chisel.ctm.client.texture.render.TextureMap;
import team.chisel.ctm.client.texture.render.TextureMap.MapType;


@RequiredArgsConstructor
public class TextureTypeMap implements ITextureType {

    private final MapType type;
    
    @Override
    public TextureMap makeTexture(TextureInfo info) {
        return new TextureMap(this, info, type);
    }
    
    @Override
    public ITextureContext getBlockRenderContext(IBlockState state, IBlockAccess world, @Nonnull BlockPos pos, ICTMTexture<?> tex) {
        return type.getContext(pos, (TextureMap) tex);
    }
    
    @Override
    public ITextureContext getContextFromData(long data) {
        return new TextureContextPosition(BlockPos.fromLong(data));
    }
    
    @TextureType("R")
    public static final TextureTypeMap R = new TextureTypeMap(MapType.RANDOM);
    
    @TextureType("V")
    public static final TextureTypeMap V = new TextureTypeMap(MapType.PATTERNED);
}
