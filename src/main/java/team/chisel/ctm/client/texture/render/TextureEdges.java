package team.chisel.ctm.client.texture.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.renderer.block.model.BakedQuad;
import team.chisel.ctm.Configurations;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.type.TextureTypeEdges;
import team.chisel.ctm.client.texture.type.TextureTypeEdges.CTMLogicEdges;
import team.chisel.ctm.client.util.Quad;

public class TextureEdges extends TextureCTM<TextureTypeEdges> {

    public TextureEdges(TextureTypeEdges type, TextureInfo info) {
        super(type, info);
    }
    
    @Override
    public List<BakedQuad> transformQuad(BakedQuad bq, ITextureContext context, int quadGoal) {
        Quad quad = makeQuad(bq, context);
        if (context == null || Configurations.isDisabled()) {
            return Collections.singletonList(quad.transformUVs(sprites[0]).rebake());
        }
        
        CTMLogicEdges logic = (CTMLogicEdges) ((TextureContextCTM)context).getCTM(bq.getDirection());
        if (logic.isObscured()) {
            return Arrays.stream(quad.transformUVs(sprites[2]).subdivide(4)).filter(Objects::nonNull).map(q -> q.rebake()).toList();
        }

        return super.transformQuad(bq, context, quadGoal);
    }
}
