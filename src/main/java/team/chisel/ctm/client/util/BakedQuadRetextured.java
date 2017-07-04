package team.chisel.ctm.client.util;

import lombok.Getter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class BakedQuadRetextured extends net.minecraft.client.renderer.block.model.BakedQuadRetextured {

	@Getter
	private final TextureAtlasSprite sprite;
	
	public BakedQuadRetextured(BakedQuad quad, TextureAtlasSprite textureIn) {
		super(quad, textureIn);
		this.sprite = textureIn;
	}
}
