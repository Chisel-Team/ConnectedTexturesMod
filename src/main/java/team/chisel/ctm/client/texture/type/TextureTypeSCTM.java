package team.chisel.ctm.client.texture.type;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.render.TextureCTM;
import team.chisel.ctm.client.texture.render.TextureSCTM;
import team.chisel.ctm.client.util.CTMLogic;

import java.util.Optional;

@TextureType("sctm")
@TextureType("ctm_simple")
public class TextureTypeSCTM extends TextureTypeCTM {

    @Override
    public ICTMTexture<TextureTypeSCTM> makeTexture(TextureInfo info) {
        return new TextureSCTM(this, info);
    }
    
    @Override
    public TextureContextCTM getBlockRenderContext(final BlockState state, final BlockGetter world, final BlockPos pos, final ICTMTexture<?> tex) {
        return new TextureContextCTM(state, world, pos, (TextureCTM<?>) tex) {
        
            @Override
            protected CTMLogic createCTM(BlockState state) {
                CTMLogic ctm = super.createCTM(state);
            
                ctm.connectionCheck.disableObscuredFaceCheck = Optional.of(true);
            
                return ctm;
            }
        };
    }
    
    @Override
    public int getQuadsPerSide() {
        return 1;
    }
    
    @Override
    public int requiredTextures() {
    	return 1;
    }
}