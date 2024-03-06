package team.chisel.ctm.client.texture.type;

import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
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
    public ITextureContext getBlockRenderContext(BlockState state, BlockAndTintGetter world, @NotNull BlockPos pos, ICTMTexture<?> tex) {
        return type.getContext(pos, (TextureMap) tex);
    }
    
    @Override
    public ITextureContext getContextFromData(long data) {
        return new TextureContextPosition(BlockPos.of(data));
    }
    
    @TextureType("r")
    @TextureType("random")
    public static final TextureTypeMap R = new TextureTypeMap(MapType.RANDOM);
    
    @TextureType("v")
    @TextureType("pattern")
    public static final TextureTypeMap V = new TextureTypeMap(MapType.PATTERNED);
}
