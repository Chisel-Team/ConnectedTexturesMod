package team.chisel.ctm.client.texture.render;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.type.TextureTypeEdges;
import team.chisel.ctm.client.texture.type.TextureTypeEdges.CTMLogicEdges;
import team.chisel.ctm.client.util.Dir;
import team.chisel.ctm.client.util.Quad;
import team.chisel.ctm.client.util.Submap;

public class TextureEdgesFull extends TextureEdges {

    public TextureEdgesFull(TextureTypeEdges type, TextureInfo info) {
        super(type, info);
    }

    @Override
    public List<BakedQuad> transformQuad(BakedQuad bq, ITextureContext context, int quadGoal) {
        Quad quad = makeQuad(bq, context);
        if (context == null) {
            return Collections.singletonList(quad.transformUVs(sprites[0]).rebake());
        }
        
        CTMLogicEdges ctm = (CTMLogicEdges) ((TextureContextCTM)context).getCTM(bq.getFace());
        TextureAtlasSprite sprite;
        ISubmap submap = null;
        if (!ctm.isObscured() && !ctm.connectedOr(Dir.VALUES)) {
            sprite = sprites[0];
            submap = Submap.X1;
        } else if (ctm.isObscured() || ctm.connectedAnd(Dir.TOP, Dir.BOTTOM) || ctm.connectedAnd(Dir.LEFT, Dir.RIGHT)) {
            sprite = sprites[1];
            submap = Submap.X3[1][1];
        } else if (ctm.connectedAnd(Dir.LEFT, Dir.TOP)) {
            sprite = sprites[2];
            submap = Submap.X2[0][0];
        } else if (ctm.connectedAnd(Dir.TOP, Dir.RIGHT)) {
            sprite = sprites[2];
            submap = Submap.X2[0][1];
        } else if (ctm.connectedAnd(Dir.BOTTOM, Dir.LEFT)) {
            sprite = sprites[2];
            submap = Submap.X2[1][0];
        } else if (ctm.connectedAnd(Dir.RIGHT, Dir.BOTTOM)) {
            sprite = sprites[2];
            submap = Submap.X2[1][1];
        }else {
            sprite = sprites[1];
            if (ctm.connected(Dir.BOTTOM) || ctm.connectedAnd(Dir.BOTTOM_LEFT, Dir.BOTTOM_RIGHT)) {
                submap = Submap.X3[0][1];
            } else if (ctm.connected(Dir.RIGHT) || ctm.connectedAnd(Dir.TOP_RIGHT, Dir.BOTTOM_RIGHT)) {
                submap = Submap.X3[1][0];
            } else if (ctm.connected(Dir.LEFT) || ctm.connectedAnd(Dir.TOP_LEFT, Dir.BOTTOM_LEFT)) {
                submap = Submap.X3[1][2];
            } else if (ctm.connected(Dir.TOP) || ctm.connectedAnd(Dir.TOP_LEFT, Dir.TOP_RIGHT)) {
                submap = Submap.X3[2][1];
            } else if (ctm.connected(Dir.BOTTOM_LEFT)) {
                submap = Submap.X3[0][2];
            } else if (ctm.connected(Dir.BOTTOM_RIGHT)) {
                submap = Submap.X3[0][0];
            } else if (ctm.connected(Dir.TOP_RIGHT)) {
                submap = Submap.X3[2][0];
            } else if (ctm.connected(Dir.TOP_LEFT)) {
                submap = Submap.X3[2][2];
            }
            if (submap == null) {
                submap = Submap.X1;
            }
        }
        
        return Collections.singletonList(quad.transformUVs(sprite, submap).rebake());
    }
}
