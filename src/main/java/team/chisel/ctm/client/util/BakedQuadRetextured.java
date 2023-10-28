package team.chisel.ctm.client.util;

import java.util.Arrays;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.client.model.IQuadTransformer;

/**
 * Revived from 1.14
 * 
 * @author Mojang
 */
public class BakedQuadRetextured extends BakedQuad {
   private final TextureAtlasSprite texture;

   public BakedQuadRetextured(BakedQuad quad, TextureAtlasSprite textureIn) {
      super(Arrays.copyOf(quad.getVertices(), quad.getVertices().length), quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
      this.texture = textureIn;
      this.remapQuad();
   }

   private void remapQuad() {
      for(int i = 0; i < 4; ++i) {
          int offset = i * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
          this.vertices[offset] = Float.floatToRawIntBits(this.texture.getU(getUnInterpolatedU(this.sprite, Float.intBitsToFloat(this.vertices[offset]))));
          this.vertices[offset + 1] = Float.floatToRawIntBits(this.texture.getV(getUnInterpolatedV(this.sprite, Float.intBitsToFloat(this.vertices[offset + 1]))));
      }
   }
   
   @Override
	public TextureAtlasSprite getSprite() {
      return texture;
	}

	private static float getUnInterpolatedU(TextureAtlasSprite sprite, float u) {
		float f = sprite.getU1() - sprite.getU0();
		return (u - sprite.getU0()) / f * 16.0F;
	}
	
	private static float getUnInterpolatedV(TextureAtlasSprite sprite, float v) {
		float f = sprite.getV1() - sprite.getV0();
		return (v - sprite.getV0()) / f * 16.0F;
	}
}