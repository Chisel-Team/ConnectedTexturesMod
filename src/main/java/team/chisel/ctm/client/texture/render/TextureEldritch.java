package team.chisel.ctm.client.texture.render;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextPosition;
import team.chisel.ctm.client.texture.type.TextureTypeEldritch;
import team.chisel.ctm.client.util.Quad;

@ParametersAreNonnullByDefault
public class TextureEldritch extends AbstractTexture<TextureTypeEldritch> {

    private static final Random rand = new Random();

    public TextureEldritch(TextureTypeEldritch type, TextureInfo info) {
        super(type, info);
    }

    @Override
    public List<BakedQuad> transformQuad(BakedQuad quad, @Nullable ITextureContext context, int quadGoal) {

        Quad q = makeQuad(quad, context);

        Quad.UVs uvs = q.getUvs();
        Vec2 min = new Vec2(uvs.getMinU(), uvs.getMinV());
        Vec2 max = new Vec2(uvs.getMaxU(), uvs.getMaxV());

        Direction facing = quad.getDirection();

        BlockPos pos = context == null ? BlockPos.ZERO : ((TextureContextPosition) context).getPosition();
        rand.setSeed(Mth.getSeed(pos) + facing.ordinal());

        float offx = offsetRand(), offy = offsetRand();

        Quad[] subdiv = q.subdivide(4);
        for (int i = 0; i < subdiv.length; i++) {
            Quad quadrant = subdiv[i];
            for (int j = 0; quadrant != null && j < 4; j++) {
            	Vec2 uv = quadrant.getUv(j);
                if (uv.x != min.x && uv.x != max.x && uv.y != min.y && uv.y != max.y) {
                    float xinterp = Quad.normalize(min.x, max.x, uv.x);
                    float yinterp = Quad.normalize(min.y, max.y, uv.y);
                    xinterp += offx;
                    yinterp += offy;
                    uv = new Vec2(Quad.lerp(min.x, max.x, xinterp), Quad.lerp(min.y, max.y, yinterp));
                    subdiv[i] = quadrant.withUv(j, uv);
                }
            }
        }

        return Arrays.stream(subdiv).filter(Objects::nonNull).map(Quad::rebake).toList();
    }

    private float offsetRand() {
        return (float) rand.nextGaussian() * 0.08f;
    }
}
