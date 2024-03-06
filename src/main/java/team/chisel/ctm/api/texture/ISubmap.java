package team.chisel.ctm.api.texture;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import team.chisel.ctm.client.util.Submap;

@ParametersAreNonnullByDefault
public interface ISubmap {

    float getYOffset();

    float getXOffset();

    float getWidth();

    float getHeight();

    default float getInterpolatedU(TextureAtlasSprite sprite, float u) {
        return sprite.getU((getXOffset() + u / getWidth()) / 16F);
    }

    default float getInterpolatedV(TextureAtlasSprite sprite, float v) {
        return sprite.getV((getYOffset() + v / getHeight()) / 16F);
    }

    default float[] toArray() {
        return new float[] { getXOffset(), getYOffset(), getXOffset() + getWidth(), getYOffset() + getHeight() };
    }

    default ISubmap unitScale() {
        return new SubmapRescaled(this, UNITS_PER_PIXEL, false);
    }

    default ISubmap pixelScale() {
        return this;
    }
    
    public interface ISpriteSubmap extends ISubmap {
     
        TextureAtlasSprite getSprite();
    }
    
    final float PIXELS_PER_UNIT = 16f;
    final float UNITS_PER_PIXEL = 1f / PIXELS_PER_UNIT;

    @RequiredArgsConstructor
    @EqualsAndHashCode
    @ToString(includeFieldNames = false)
    static class SubmapRescaled implements ISubmap {
        
        private final ISubmap parent;
        private final float ratio;
        private final boolean isPixelScale;

        @Override
        public float getXOffset() {
            return parent.getXOffset() * ratio;
        }

        @Override
        public float getYOffset() {
            return parent.getYOffset() * ratio;
        }

        @Override
        public float getWidth() {
            return parent.getWidth() * ratio;
        }

        @Override
        public float getHeight() {
            return parent.getHeight() * ratio;
        }

        @Override
        public ISubmap pixelScale() {
            return isPixelScale ? this : parent;
        }

        @Override
        public ISubmap unitScale() {
            return isPixelScale ? parent : this;
        }

        @Override
        public float getInterpolatedU(TextureAtlasSprite sprite, float u) {
            return parent.getInterpolatedU(sprite, u);
        }

        @Override
        public float getInterpolatedV(TextureAtlasSprite sprite, float v) {
            return parent.getInterpolatedV(sprite, v);
        }

        @Override
        public float[] toArray() {
            return parent.toArray();
        }
    }
    
    default ISubmap flipX() {
        return Submap.fromPixelScale(getWidth(), getHeight(), PIXELS_PER_UNIT - getXOffset() - getWidth(), getYOffset());
    }

    default ISubmap flipY() {
        return Submap.fromPixelScale(getWidth(), getHeight(), getXOffset(), PIXELS_PER_UNIT - getYOffset() - getHeight());
    }
}
