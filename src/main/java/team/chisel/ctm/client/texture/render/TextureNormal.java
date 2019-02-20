package team.chisel.ctm.client.texture.render;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import net.minecraft.client.renderer.model.BakedQuad;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.type.TextureTypeNormal;

/**
 * CTM texture for a normal texture
 */
public class TextureNormal extends AbstractTexture<TextureTypeNormal> {

    public TextureNormal(TextureTypeNormal type, TextureInfo info){
        super(type, info);
    }

    @Override
    public List<BakedQuad> transformQuad(BakedQuad quad, ITextureContext context, int quadGoal) {
        if (quadGoal == 4) {
            return Arrays.stream(makeQuad(quad, context).transformUVs(sprites[0]).subdivide(4)).filter(Objects::nonNull).map(qu -> qu.rebake()).collect(Collectors.toList());
        }
        return Lists.newArrayList(makeQuad(quad, context).transformUVs(sprites[0]).rebake());
    }
}
