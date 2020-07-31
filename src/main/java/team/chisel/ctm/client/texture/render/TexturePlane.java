package team.chisel.ctm.client.texture.render;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.util.Direction;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.type.TextureTypePlane;
import team.chisel.ctm.client.util.CTMLogic;
import team.chisel.ctm.client.util.Dir;
import team.chisel.ctm.client.util.Quad;
import team.chisel.ctm.client.util.Submap;

import java.util.Collections;
import java.util.List;

public class TexturePlane extends TextureCTM<TextureTypePlane> {
    private final Dir dir0, dir1;
    
    public TexturePlane(TextureTypePlane type, TextureInfo info) {
        super(type, info);
        boolean v = type.getPlane() == Direction.Plane.VERTICAL;
        this.dir0 = v ? Dir.TOP : Dir.LEFT;
        this.dir1 = v ? Dir.BOTTOM : Dir.RIGHT;
    }

    @Override
    public List<BakedQuad> transformQuad(BakedQuad quad, ITextureContext context, int quadGoal) {
        Quad q = makeQuad(quad, context);
        CTMLogic ctm = context == null ? null : ((TextureContextCTM) context).getCTM(quad.getFace());
        ISubmap submap = getQuad(ctm);
        q = q.transformUVs(sprites[0], submap);
        return Collections.singletonList(q.rebake());
    }

    private ISubmap getQuad(CTMLogic ctm) {
        if (ctm == null || !ctm.connectedOr(dir0, dir1)) {
            return Submap.X2[0][0];
        } else if (ctm.connectedAnd(dir0, dir1)) {
            return Submap.X2[0][1];
        } else if (ctm.connected(dir0)) {
            return Submap.X2[1][1];
        } else {
            return Submap.X2[1][0];
        }
    }
}
