package team.chisel.ctm.client.texture.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.state.BlockState;
import team.chisel.ctm.Configurations;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.newctm.ConnectionCheck;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.type.TextureTypeCTM;
import team.chisel.ctm.client.util.BlockstatePredicateParser;
import team.chisel.ctm.client.util.CTMLogic;
import team.chisel.ctm.client.util.CTMLogic.StateComparisonCallback;
import team.chisel.ctm.client.util.ParseUtils;
import team.chisel.ctm.client.util.Quad;

@ParametersAreNonnullByDefault
@Accessors(fluent = true)
public class TextureCTM<T extends TextureTypeCTM> extends AbstractTexture<T> {

    private static final BlockstatePredicateParser predicateParser = new BlockstatePredicateParser();

	@Getter
	private final Optional<Boolean> connectInside;
	
	@Getter
	private final boolean ignoreStates;
	
	@Nullable
	private final BiPredicate<Direction, BlockState> connectionChecks;
	
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

	private final Cache<CacheKey, Object2ByteMap<BlockState>> connectionCache = CacheBuilder.newBuilder().build();

    public TextureCTM(T type, TextureInfo info) {
        super(type, info);
        this.connectInside = info.getInfo().flatMap(obj -> ParseUtils.getBoolean(obj, "connect_inside"));
        this.ignoreStates = info.getInfo().map(obj -> GsonHelper.getAsBoolean(obj, "ignore_states", false)).orElse(false);
        this.connectionChecks = info.getInfo().map(obj -> predicateParser.parse(obj.get("connect_to"))).orElse(null);
    }

    public boolean connectTo(ConnectionCheck ctm, BlockState from, BlockState to, Direction dir) {
        try {
            return ((connectionChecks == null ? StateComparisonCallback.DEFAULT.connects(ctm, from, to, dir) : connectionChecks.test(dir, to)) ? 1 : 0) == 1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<BakedQuad> transformQuad(BakedQuad bq, ITextureContext context, int quadGoal) {
        Quad quad = makeQuad(bq, context);
        if (context == null || Configurations.disableCTM) {
            return Collections.singletonList(quad.transformUVs(sprites[0]).rebake());
        }

        Quad[] quads = quad.subdivide(4);
        
        int[] ctm = ((TextureContextCTM)context).getCTM(bq.getDirection()).getSubmapIndices();
        //System.out.println(bq.getDirection() + ": " + Arrays.toString(ctm));

        for (int i = 0; i < quads.length; i++) {
            Quad q = quads[i];
            if (q != null) {
                int ctmid = q.getUvs().normalize().getQuadrant();
//              quads[i] = q.grow().transformUVs(sprites[1], CTMLogic.uvs[16]);

                quads[i] = q.grow().transformUVs(sprites[ctm[ctmid] > 15 ? 0 : 1], CTMLogic.uvs[ctm[ctmid]].unitScale());
            }
        }
        return Arrays.stream(quads).filter(Objects::nonNull).map(q -> q.rebake()).toList();
    }
    
    @Override
    protected Quad makeQuad(BakedQuad bq, ITextureContext context) {
        return super.makeQuad(bq, context).derotate();
    }
}
