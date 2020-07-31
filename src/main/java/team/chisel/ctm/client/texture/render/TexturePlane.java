package team.chisel.ctm.client.texture.render;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.util.Direction;
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
        CTMLogic ctm = null;
        if (context != null)
            ctm = ((TextureContextCTM) context).getCTM(quad.getFace());
        q = q.transformUVs(sprites[0], getQuad(ctm));
        return Collections.singletonList(q.rebake());
    }

    private ISubmap getQuad(CTMLogic ctm) {
        boolean vertical = plane == Plane.VERTICAL;
        boolean c0 = (ctm != null) && ctm.connected(vertical ? Dir.TOP : Dir.LEFT);
        boolean c1 = (ctm != null) && ctm.connected(vertical ? Dir.BOTTOM : Dir.RIGHT);
        int u = (c0 == c1) ? 0 : 1, v = c0 ? 1 : 0;
        return Submap.X2[vertical ? v : u][vertical ? u : v];
    }
}
