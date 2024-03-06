package team.chisel.ctm.client.texture.type;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import team.chisel.ctm.Configurations;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.render.TextureCTM;

@TextureType("ctm")
public class TextureTypeCTM implements ITextureType {

    @Override
    public ICTMTexture<? extends TextureTypeCTM> makeTexture(TextureInfo info) {
      return new TextureCTM<>(this, info);
    }

    @Override
    public TextureContextCTM getBlockRenderContext(BlockState state, BlockAndTintGetter world, BlockPos pos, ICTMTexture<?> tex) {
        return new TextureContextCTM(state, world, pos, (TextureCTM<?>) tex);
    }

    @Override
    public int getQuadsPerSide() {
        return Configurations.isDisabled() ? 1 : 4;
    }

    @Override
    public int requiredTextures() {
        return 2;
    }

	@Override
	public ITextureContext getContextFromData(long data) {
		throw new UnsupportedOperationException();
	}
}
