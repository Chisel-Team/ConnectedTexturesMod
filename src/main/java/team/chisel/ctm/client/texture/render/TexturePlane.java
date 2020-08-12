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

    public TexturePlane(final TextureTypePlane type, final TextureInfo info) {
        super(type, info);
        this.plane = type.getPlane();
    }

    @Override
    public List<BakedQuad> transformQuad(final BakedQuad bakedQuad, final ITextureContext context, final int quads) {
        final Quad quad = this.makeQuad(bakedQuad, context);
        final CTMLogic logic = (context instanceof TextureContextCTM) ? ((TextureContextCTM) context).getCTM(bakedQuad.getFace()) : null;
        return Collections.singletonList(quad.transformUVs(this.sprites[0], this.getQuad(logic)).rebake());
    }

    private ISubmap getQuad(final CTMLogic logic) {
        if (logic == null) {
            return Submap.X2[0][0];
        }
        final int u;
        final int v;
        if (this.plane == Plane.VERTICAL) {
            final boolean top = logic.connected(Dir.TOP);
            u = (top == logic.connected(Dir.BOTTOM)) ? 0 : 1;
            v = top ? 1 : 0;
        } else {
            final boolean left = logic.connected(Dir.LEFT);
            u = left ? 1 : 0;
            v = (left == logic.connected(Dir.RIGHT)) ? 0 : 1;
        }
        return Submap.X2[v][u];
    }
}
