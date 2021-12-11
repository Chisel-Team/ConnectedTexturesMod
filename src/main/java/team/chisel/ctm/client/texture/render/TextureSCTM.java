package team.chisel.ctm.client.texture.render;

import net.minecraft.client.renderer.block.model.BakedQuad;
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
    public TextureSCTM(final TextureTypeSCTM type, final TextureInfo info) {
        super(type, info);
    }

    @Override
    public List<BakedQuad> transformQuad(final BakedQuad bakedQuad, final ITextureContext context, final int quads) {
        final Quad quad = this.makeQuad(bakedQuad, context);
        final CTMLogic ctm = (context instanceof TextureContextCTM) ? ((TextureContextCTM) context).getCTM(bakedQuad.getDirection()) : null;
        return Collections.singletonList(quad.transformUVs(this.sprites[0], this.getQuad(ctm)).rebake());
    }

    private ISubmap getQuad(final CTMLogic logic) {
        if (logic == null) {
            return Submap.X2[0][0];
        }
        final boolean top = logic.connected(Dir.TOP);
        final boolean bottom = logic.connected(Dir.BOTTOM);
        final boolean left = logic.connected(Dir.LEFT);
        final boolean right = logic.connected(Dir.RIGHT);
        if (top || bottom || left || right) {
            if (!top || !bottom) {
                return Submap.X2[0][(left && right) ? 1 : 0];
            }
            if (!left || !right) {
                return Submap.X2[1][0];
            }
            if (logic.connected(Dir.TOP_LEFT) && logic.connected(Dir.TOP_RIGHT)) {
                if (logic.connected(Dir.BOTTOM_LEFT) && logic.connected(Dir.BOTTOM_RIGHT)) {
                    return Submap.X2[1][1];
                }
            }
        }
        return Submap.X2[0][0];
    }

    @Override
    public Optional<Boolean> connectInside() {
        return Optional.of(true);
    }
}
