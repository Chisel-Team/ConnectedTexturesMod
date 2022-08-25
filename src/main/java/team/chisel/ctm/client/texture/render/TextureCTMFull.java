package team.chisel.ctm.client.texture.render;

import com.google.common.collect.Lists;


import lombok.experimental.Accessors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import team.chisel.ctm.Configurations;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.type.TextureTypeCTMFull;
import team.chisel.ctm.client.util.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@Accessors(fluent = true)
public class TextureCTMFull extends TextureCTM<TextureTypeCTMFull> {

	public TextureCTMFull(TextureTypeCTMFull type, TextureInfo info) {
		super(type, info);
	}

	@Override
	public List<BakedQuad> transformQuad(BakedQuad bq, ITextureContext context, int quadGoal) {
		Quad quad = makeQuad(bq, context);
		CTMLogic ctm = (context instanceof TextureContextCTM ctmContext) ? ctmContext.getCTM(bq.getDirection()) : null;
		if (context == null || ctm == null || Configurations.disableCTM) {
			return Collections.singletonList(quad.transformUVs(sprites[0], Submap.X4[0][0]).rebake());
		}

		TextureAtlasSprite sprite;
		ISubmap submap;

		boolean top   		= ctm.connected(Dir.TOP);
		boolean right  		= ctm.connected(Dir.RIGHT);
		boolean bottom		= ctm.connected(Dir.BOTTOM);
		boolean left  		= ctm.connected(Dir.LEFT);
		boolean topLeft   	= ctm.connected(Dir.TOP_LEFT);
		boolean topRight  	= ctm.connected(Dir.TOP_RIGHT);
		boolean bottomLeft 	= ctm.connected(Dir.BOTTOM_LEFT);
		boolean bottomRight = ctm.connected(Dir.BOTTOM_RIGHT);

		if (!top && !bottom && !left && !right) {
			sprite = sprites[0];
			submap = Submap.X4[0][0];
		} else if (top && bottom && left && right) {
			sprite = sprites[2];
			if (topLeft && topRight && bottomLeft && bottomRight) {
				sprite = sprites[0];
				submap = Submap.X4[2][2];
			} else if (topLeft && topRight && bottomLeft) {
				submap = Submap.X4[0][0];
			} else if (topLeft && topRight && bottomRight) {
				submap = Submap.X4[0][1];
			} else if (topLeft && bottomLeft && bottomRight) {
				submap = Submap.X4[1][0];
			} else if (topRight && bottomLeft && bottomRight) {
				submap = Submap.X4[1][1];
			} else if (topLeft && topRight) {
				submap = Submap.X4[2][0];
			} else if (bottomLeft && bottomRight) {
				submap = Submap.X4[3][1];
			} else if (topLeft && bottomLeft) {
				submap = Submap.X4[3][0];
			} else if (topRight && bottomRight) {
				submap = Submap.X4[2][1];
			} else if (topLeft && bottomRight) {
				submap = Submap.X4[2][2];
			} else if (topRight && bottomLeft) {
				submap = Submap.X4[2][3];
			} else if (topLeft) {
				submap = Submap.X4[0][2];
			} else if (topRight) {
				submap = Submap.X4[0][3];
			} else if (bottomLeft) {
				submap = Submap.X4[1][2];
			} else if (bottomRight) {
				submap = Submap.X4[1][3];
			} else {
				submap = Submap.X4[3][2];
			}
		} else if (top && left && right) {
			sprite = sprites[1];
			if (topLeft && topRight) {
				sprite = sprites[0];
				submap = Submap.X4[3][2];
			} else if (topLeft) {
				submap = Submap.X4[1][3];
			} else if (topRight) {
				submap = Submap.X4[3][1];
			} else {
				submap = Submap.X4[3][3];
			}
		} else if (top && bottom && right) {
			sprite = sprites[1];
			if (topRight && bottomRight) {
				sprite = sprites[0];
				submap = Submap.X4[2][1];
			} else if (topRight) {
				submap = Submap.X4[1][2];
			} else if (bottomRight) {
				submap = Submap.X4[3][0];
			} else {
				submap = Submap.X4[3][2];
			}
		} else if (bottom && left && right) {
			sprite = sprites[1];
			if (bottomLeft && bottomRight) {
				sprite = sprites[0];
				submap = Submap.X4[1][2];
			} else if (bottomLeft) {
				submap = Submap.X4[2][0];
			} else if (bottomRight) {
				submap = Submap.X4[0][2];
			} else {
				submap = Submap.X4[2][2];
			}
		} else if (top && bottom && left) {
			sprite = sprites[1];
			if (topLeft && bottomLeft) {
				sprite = sprites[0];
				submap = Submap.X4[2][3];
			} else if (topLeft) {
				submap = Submap.X4[2][1];
			} else if (bottomLeft) {
				submap = Submap.X4[0][3];
			} else {
				submap = Submap.X4[2][3];
			}
		} else if (bottom && right) {
			if (bottomRight) {
				sprite = sprites[0];
				submap = Submap.X4[1][1];
			} else {
				sprite = sprites[1];
				submap = Submap.X4[0][0];
			}
		} else if (bottom && left) {
			if (bottomLeft) {
				sprite = sprites[0];
				submap = Submap.X4[1][3];
			} else {
				sprite = sprites[1];
				submap = Submap.X4[0][1];
			}
		} else if (top && right) {
			if (topRight) {
				sprite = sprites[0];
				submap = Submap.X4[3][1];
			} else {
				sprite = sprites[1];
				submap = Submap.X4[1][0];
			}
		} else if (top && left) {
			if (topLeft) {
				sprite = sprites[0];
				submap = Submap.X4[3][3];
			} else {
				sprite = sprites[1];
				submap = Submap.X4[1][1];
			}
		} else if (top && bottom) {
			sprite = sprites[0];
			submap = Submap.X4[2][0];
		} else if (left && right) {
			sprite = sprites[0];
			submap = Submap.X4[0][2];
		} else if (top) {
			sprite = sprites[0];
			submap = Submap.X4[3][0];
		} else if (bottom) {
			sprite = sprites[0];
			submap = Submap.X4[1][0];
		} else if (left) {
			sprite = sprites[0];
			submap = Submap.X4[0][3];
		} else {
			sprite = sprites[0];
			submap = Submap.X4[0][1];
		}

		if (quadGoal == 1)
			return Collections.singletonList(quad.transformUVs(sprite, submap).rebake());

		ISubmap finalSubmap = submap;
		TextureAtlasSprite finalSprite = sprite;
		return Lists.newArrayList(quad.subdivide(quadGoal)).stream()
				.filter(Objects::nonNull)
				.map(q -> q.transformUVs(finalSprite, finalSubmap).rebake())
				.toList();
	}
}
