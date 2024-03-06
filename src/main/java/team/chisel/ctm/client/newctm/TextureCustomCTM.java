package team.chisel.ctm.client.newctm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.state.BlockState;
import team.chisel.ctm.Configurations;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.newctm.CTMLogicBakery.OutputFace;
import team.chisel.ctm.client.texture.render.AbstractTexture;
import team.chisel.ctm.client.util.BlockstatePredicateParser;
import team.chisel.ctm.client.util.CTMLogic.StateComparisonCallback;
import team.chisel.ctm.client.util.ParseUtils;
import team.chisel.ctm.client.util.PartialTextureAtlasSprite;
import team.chisel.ctm.client.util.Quad;
import team.chisel.ctm.client.util.Submap;

@ParametersAreNonnullByDefault
@Accessors(fluent = true)
public class TextureCustomCTM<T extends TextureTypeCustom> extends AbstractTexture<T> implements ITextureConnection {

    private static final BlockstatePredicateParser predicateParser = new BlockstatePredicateParser();

	@Getter
	private final Optional<Boolean> connectInside;
	
	@Getter
	private final boolean ignoreStates;
	
	@Nullable
	private final BiPredicate<Direction, BlockState> connectionChecks;

    private final TextureAtlasSprite particleSprite;
	
	@RequiredArgsConstructor
	private static final class CacheKey {
		private final BlockState from;
		private final Direction dir;
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + dir.hashCode();
			result = prime * result + System.identityHashCode(from);
			return result;
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CacheKey other = (CacheKey) obj;
			if (dir != other.dir)
				return false;
			if (from != other.from)
				return false;
			return true;
		}
	}

    public TextureCustomCTM(T type, TextureInfo info) {
        super(type, info);
        this.connectInside = info.getInfo().flatMap(obj -> ParseUtils.getBoolean(obj, "connect_inside"));
        this.ignoreStates = info.getInfo().map(obj -> GsonHelper.getAsBoolean(obj, "ignore_states", false)).orElse(false);
        this.connectionChecks = info.getInfo().map(obj -> predicateParser.parse(obj.get("connect_to"))).orElse(null);
        //Crop the particle sprite so that it only contains the bit it should
        this.particleSprite = PartialTextureAtlasSprite.createPartial(super.getParticle(), getFallbackUvs());
    }

    @Override
    public boolean connectTo(ConnectionCheck ctm, BlockState from, BlockState to, Direction dir) {
        try {
            return ((connectionChecks == null ? StateComparisonCallback.DEFAULT.connects(ctm, from, to, dir) : connectionChecks.test(dir, to)) ? 1 : 0) == 1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TextureAtlasSprite getParticle() {
        return this.particleSprite;
    }

    private ISubmap getFallbackUvs() {
        //TODO: Is this the proper submap to use when not a proxy?
        return isProxy ? type.getFallbackUvs() : Submap.X1;
    }

    @Override
    public List<BakedQuad> transformQuad(BakedQuad bq, ITextureContext context, int quadGoal) {
        Quad quad = makeQuad(bq, context);
        if (context == null || Configurations.disableCTM) {
            return Collections.singletonList(quad.setUVs(sprites[0], getFallbackUvs()).rebake());
        }

        OutputFace[] ctm = ((TextureContextCustomCTM)context).getCTM(bq.getDirection()).getCachedSubmaps();
        List<BakedQuad> ret = new ArrayList<>();
        for (var face : ctm) {
            //System.out.println(bq.getDirection() + "\t" + face.getFace() + ": " + face.getTex() + "@ " + face.getUvs());
            Quad sub = quad.subsect(face.getFace());
            if (sub != null) {
                ret.add(sub.setUVs(sprites[face.getTex()], face.getUvs()).rebake());
            }
        }
        return ret;
    }
    
    @Override
    protected Quad makeQuad(BakedQuad bq, ITextureContext context) {
        return super.makeQuad(bq, context).derotate();
    }
}
