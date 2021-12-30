package team.chisel.ctm.client.texture.type;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextPillar;
import team.chisel.ctm.client.texture.render.TexturePillar;

@TextureType("ctmv")
@TextureType("pillar")
public class TextureTypePillar implements ITextureType {

    @Override
    public ICTMTexture<TextureTypePillar> makeTexture(TextureInfo info) {
        return new TexturePillar(this, info);
    }
    
    @Override
    public TextureContextPillar getBlockRenderContext(BlockState state, BlockGetter world, BlockPos pos, ICTMTexture<?> tex) {
        return new TextureContextPillar(world, pos);
    }
    
    @Override
    public int requiredTextures() {
        return 2;
    }

    @Override
    public ITextureContext getContextFromData(long data){
        return new TextureContextPillar(data);
    }
}