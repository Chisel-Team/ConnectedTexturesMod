package team.chisel.ctm.client.texture.render;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.util.Direction.Plane;
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
    private final Plane plane;

    public TexturePlane(TextureTypePlane type, TextureInfo info) {
        super(type, info);
        plane = type.getPlane();
    }

    @Override
    public List<BakedQuad> transformQuad(BakedQuad quad, ITextureContext context, int quadGoal) {
        Quad q = makeQuad(quad, context);
        CTMLogic ctm = (context != null) ? ((TextureContextCTM) context).getCTM(quad.getFace()) : null;
        return Collections.singletonList(q.transformUVs(sprites[0], getQuad(ctm)).rebake());
    }

    private ISubmap getQuad(CTMLogic ctm) {
        if (ctm == null) {
            return Submap.X2[0][0];
        }
        final int u, v;
        if (this.plane == Plane.VERTICAL) {
            final boolean top = ctm.connected(Dir.TOP);
            u = (top == ctm.connected(Dir.BOTTOM)) ? 0 : 1;
            v = top ? 1 : 0;
        } else {
            final boolean left = ctm.connected(Dir.LEFT);
            u = left ? 1 : 0;
            v = (left == ctm.connected(Dir.RIGHT)) ? 0 : 1;
        }
        return Submap.X2[v][u];
    }
}
