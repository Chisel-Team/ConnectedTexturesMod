package team.chisel.ctm.client.texture.type;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.render.TextureCTM;
import team.chisel.ctm.client.texture.render.TextureEdges;
import team.chisel.ctm.client.texture.render.TextureEdgesFull;
import team.chisel.ctm.client.util.CTMLogic;

@TextureType("EDGES_FULL")
public class TextureTypeEdgesFull extends TextureTypeEdges {
    
    @Override
    public ICTMTexture<? extends TextureTypeCTM> makeTexture(TextureInfo info) {
        return new TextureEdgesFull(this, info);
    }

    @Override
    public TextureContextCTM getBlockRenderContext(IBlockState state, IBlockAccess world, BlockPos pos, ICTMTexture<?> tex) {
        return new TextureContextCTM(state, world, pos, (TextureCTM<?>) tex) {
            @Override
            protected CTMLogic createCTM(IBlockState state) {
                return new CTMLogicEdges((TextureEdges) tex);
            }
        };
    }
    
    @Override
    public int requiredTextures() {
        return 2;
    }

    @Override
    public int getQuadsPerSide() {
        return 1;
    }
}
