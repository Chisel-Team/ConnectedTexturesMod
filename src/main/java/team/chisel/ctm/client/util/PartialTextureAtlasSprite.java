package team.chisel.ctm.client.util;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import team.chisel.ctm.api.texture.ISubmap;

public class PartialTextureAtlasSprite extends TextureAtlasSprite {

    public static TextureAtlasSprite createPartial(TextureAtlasSprite sprite, ISubmap submap) {
        submap = submap.unitScale();
        if (submap.getXOffset() == 0 && submap.getYOffset() == 0 && submap.getWidth() == 1 && submap.getHeight() == 1) {
            return sprite;
        }

        float width = sprite.getU1() - sprite.getU0();
        float height = sprite.getV1() - sprite.getV0();

        float atlasWidth = sprite.contents().width() / width;
        float atlasHeight = sprite.contents().height() / height;

        float uWidth = width * submap.getWidth();
        float vHeight = height * submap.getHeight();
        float xOffset = Quad.lerp(sprite.getU0(), sprite.getU1(), submap.getXOffset());
        float yOffset = Quad.lerp(sprite.getV0(), sprite.getV1(), submap.getYOffset());
        return new PartialTextureAtlasSprite(sprite, atlasWidth, atlasHeight, xOffset, uWidth, yOffset, vHeight);
    }

    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;

    protected PartialTextureAtlasSprite(TextureAtlasSprite sprite, float atlasWidth, float atlasHeight, float xOffset, float uWidth, float yOffset, float vHeight) {
        super(sprite.atlasLocation(), sprite.contents(), (int) atlasWidth, (int) atlasHeight, sprite.getX(), sprite.getY());
        this.u0 = xOffset;
        this.u1 = xOffset + uWidth;
        this.v0 = yOffset;
        this.v1 = yOffset + vHeight;
    }

    @Override
    public float getU0() {
        return this.u0;
    }

    @Override
    public float getU1() {
        return this.u1;
    }

    @Override
    public float getU(float u) {
        float width = getU1() - getU0();
        return getU0() + width * u;
    }

    @Override
    public float getUOffset(float offset) {
        float width = getU1() - getU0();
        return (offset - getU0()) / width;
    }

    @Override
    public float getV0() {
        return this.v0;
    }

    @Override
    public float getV1() {
        return this.v1;
    }

    @Override
    public float getV(float v) {
        float height = getV1() - getV0();
        return getV0() + height * v;
    }

    @Override
    public float getVOffset(float offset) {
        float height = getV1() - getV0();
        return (offset - getV0()) / height;
    }

    @Override
    public String toString() {
        return "PartialTextureAtlasSprite{contents='" + contents() + "', u0=" + getU0() + ", u1=" + getU1() + ", v0=" + getV0() + ", v1=" + getV1() + "}";
    }

    private float atlasSize() {
        float atlasWidth = contents().width() / (getU1() - getU0());
        float atlasHeight = contents().height() / (getV1() - getV0());
        return Math.max(atlasWidth, atlasHeight);
    }

    @Override
    public float uvShrinkRatio() {
        return 4.0F / atlasSize();
    }
}