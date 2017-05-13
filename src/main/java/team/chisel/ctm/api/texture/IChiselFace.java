package team.chisel.ctm.api.texture;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Deprecated
public interface IChiselFace {

    List<ICTMTexture<?>> getTextureList();

    @Nonnull TextureAtlasSprite getParticle();
}
