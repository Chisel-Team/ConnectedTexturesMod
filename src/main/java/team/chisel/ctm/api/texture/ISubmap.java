package team.chisel.ctm.api.texture;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@ParametersAreNonnullByDefault
public interface ISubmap {

    float getYOffset();

    float getXOffset();

    float getWidth();

    float getHeight();

    float getInterpolatedU(TextureAtlasSprite sprite, float u);

    float getInterpolatedV(TextureAtlasSprite sprite, float v);

    float[] toArray();

    ISubmap normalize();

    ISubmap relativize();
}
