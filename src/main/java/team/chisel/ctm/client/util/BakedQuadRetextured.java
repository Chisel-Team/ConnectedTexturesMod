package team.chisel.ctm.client.util;

import java.util.Arrays;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.model.IQuadTransformer;

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
          this.vertices[offset] = Float.floatToRawIntBits(this.texture.getU(this.sprite.getUOffset(Float.intBitsToFloat(this.vertices[offset]))));
          this.vertices[offset + 1] = Float.floatToRawIntBits(this.texture.getV(this.sprite.getUOffset(Float.intBitsToFloat(this.vertices[offset + 1]))));
      }
   }
   
   @Override
	public TextureAtlasSprite getSprite() {
      return texture;
	}
}