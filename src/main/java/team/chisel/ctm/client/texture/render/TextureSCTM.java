package team.chisel.ctm.client.texture.render;

import net.minecraft.client.renderer.model.BakedQuad;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.type.TextureTypeSCTM;
import team.chisel.ctm.client.util.CTMLogic;
import team.chisel.ctm.client.util.Dir;
import team.chisel.ctm.client.util.Quad;
import team.chisel.ctm.client.util.Submap;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TextureSCTM extends TextureCTM<TextureTypeSCTM> {

    public TextureSCTM(TextureTypeSCTM type, TextureInfo info) {
        super(type, info);
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
        if (ctm == null || !ctm.connectedOr(Dir.TOP, Dir.RIGHT, Dir.BOTTOM, Dir.LEFT)) {
            return Submap.X2[0][0];
        } else if (ctm.connectedAnd(Dir.TOP, Dir.TOP_RIGHT, Dir.RIGHT, Dir.BOTTOM_RIGHT, Dir.BOTTOM, Dir.BOTTOM_LEFT, Dir.LEFT, Dir.TOP_LEFT)) {
            return Submap.X2[1][1];
        } else if (ctm.connectedAnd(Dir.TOP, Dir.RIGHT, Dir.BOTTOM, Dir.LEFT)) {
            return Submap.X2[0][0];
        } else if (ctm.connectedAnd(Dir.LEFT, Dir.RIGHT)) {
            return Submap.X2[0][1];
        } else if (ctm.connectedAnd(Dir.TOP, Dir.BOTTOM)) {
            return Submap.X2[1][0];
        } else {
            return Submap.X2[0][0];
        }
    }

    @Override
    public Optional<Boolean> connectInside() {
        return Optional.of(true);
    }
}
