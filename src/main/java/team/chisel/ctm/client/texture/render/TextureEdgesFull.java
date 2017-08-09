package team.chisel.ctm.client.texture.render;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

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
        // Short circuit zero connections, as this is almost always the most common case
        if (!ctm.isObscured() && ctm.connectedNone(Dir.VALUES)) {
            sprite = sprites[0];
            submap = Submap.X1;
        } else {
            sprite = sprites[1];
            boolean top     = ctm.connected(Dir.TOP)    || ctm.connectedAnd(Dir.TOP_LEFT, Dir.TOP_RIGHT);
            boolean right   = ctm.connected(Dir.RIGHT)  || ctm.connectedAnd(Dir.TOP_RIGHT, Dir.BOTTOM_RIGHT);
            boolean bottom  = ctm.connected(Dir.BOTTOM) || ctm.connectedAnd(Dir.BOTTOM_LEFT, Dir.BOTTOM_RIGHT);
            boolean left    = ctm.connected(Dir.LEFT)   || ctm.connectedAnd(Dir.TOP_LEFT, Dir.BOTTOM_LEFT);
            if (ctm.isObscured() || (top && bottom) || (right && left)) {
                submap = Submap.X4[2][1];
            } else if (!(top || right || bottom || left) && ctm.connectedAnd(Dir.TOP_LEFT, Dir.BOTTOM_RIGHT)) {
                submap = Submap.X4[0][1];
            } else if (!(top || right || bottom || left) && ctm.connectedAnd(Dir.TOP_RIGHT, Dir.BOTTOM_LEFT)) {
                submap = Submap.X4[0][2];
            } else if (!(bottom || right) && ctm.connectedOr(Dir.LEFT, Dir.BOTTOM_LEFT) && ctm.connectedOr(Dir.TOP, Dir.TOP_RIGHT)) {
                submap = Submap.X4[0][3];
            } else if (!(bottom || left) && ctm.connectedOr(Dir.TOP, Dir.TOP_LEFT) && ctm.connectedOr(Dir.RIGHT, Dir.BOTTOM_RIGHT)) {
                submap = Submap.X4[1][3];
            } else if (!(top || left) && ctm.connectedOr(Dir.RIGHT, Dir.TOP_RIGHT) && ctm.connectedOr(Dir.BOTTOM, Dir.BOTTOM_LEFT)) {
                submap = Submap.X4[2][3];
            } else if (!(top || right) && ctm.connectedOr(Dir.BOTTOM, Dir.BOTTOM_RIGHT) && ctm.connectedOr(Dir.LEFT, Dir.TOP_LEFT)) {
                submap = Submap.X4[3][3];
            } else if (bottom) {
                submap = Submap.X4[1][1];
            } else if (right) {
                submap = Submap.X4[2][0];
            } else if (left) {
                submap = Submap.X4[2][2];
            } else if (top) {
                submap = Submap.X4[3][1];
            } else if (ctm.connected(Dir.BOTTOM_LEFT)) {
                submap = Submap.X4[1][2];
            } else if (ctm.connected(Dir.BOTTOM_RIGHT)) {
                submap = Submap.X4[1][0];
            } else if (ctm.connected(Dir.TOP_RIGHT)) {
                submap = Submap.X4[3][0];
            } else if (ctm.connected(Dir.TOP_LEFT)) {
                submap = Submap.X4[3][2];
            }
            if (submap == null) {
                submap = Submap.X1;
            }
        }
        
        quad = quad.transformUVs(sprite, submap);
        
        if (quadGoal == 1) {
            return Collections.singletonList(quad.rebake());
        }
        
        return Lists.newArrayList(quad.subdivide(quadGoal)).stream()
                .filter(Objects::nonNull)
                .map(Quad::rebake)
                .collect(Collectors.toList());
    }
}
