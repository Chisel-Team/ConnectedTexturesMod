package team.chisel.ctm.client.texture.render;

import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenCustomHashMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.JsonUtils;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.type.TextureTypeCTM;
import team.chisel.ctm.client.util.BlockstatePredicateParser;
import team.chisel.ctm.client.util.CTMLogic;
import team.chisel.ctm.client.util.CTMLogic.StateComparisonCallback;
import team.chisel.ctm.client.util.IdentityStrategy;
import team.chisel.ctm.client.util.ParseUtils;
import team.chisel.ctm.client.util.Quad;

@ParametersAreNonnullByDefault
@Accessors(fluent = true)
public class TextureCTM<T extends TextureTypeCTM> extends AbstractTexture<T> {

    private static final BlockstatePredicateParser predicateParser = new BlockstatePredicateParser();

	@Getter
	private final Optional<Boolean> connectInside;
	
	@Getter
	private final boolean ignoreStates, actualStates;
	
	@Nullable
	private final BiPredicate<EnumFacing, IBlockState> connectionChecks;
	
	@RequiredArgsConstructor
	private static final class CacheKey {
		private final IBlockState from;
		private final EnumFacing dir;
		
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

	private final Map<CacheKey, Object2ByteMap<IBlockState>> connectionCache = new HashMap<>();

    public TextureCTM(T type, TextureInfo info) {
        super(type, info);
        this.connectInside = info.getInfo().flatMap(obj -> ParseUtils.getBoolean(obj, "connect_inside"));
        this.ignoreStates = info.getInfo().map(obj -> JsonUtils.getBoolean(obj, "ignore_states", false)).orElse(false);
        this.actualStates = info.getInfo().map(obj -> JsonUtils.getBoolean(obj, "use_actual_state", false)).orElse(false);
        this.connectionChecks = info.getInfo().map(obj -> predicateParser.parse(obj.get("connect_to"))).orElse(null);
    }
    
    public boolean connectTo(CTMLogic ctm, IBlockState from, IBlockState to, EnumFacing dir) {
        synchronized (connectionCache) {
        	Object2ByteMap<IBlockState> sidecache = connectionCache.computeIfAbsent(new CacheKey(from, dir), 
				k -> {
					Object2ByteMap<IBlockState> map = new Object2ByteOpenCustomHashMap<>(new IdentityStrategy<>());
					map.defaultReturnValue((byte) -1);
					return map;
				});

        	byte cached = sidecache.getByte(to);
            if (cached == -1) {
                sidecache.put(to, cached = (byte) ((connectionChecks == null ? StateComparisonCallback.DEFAULT.connects(ctm, from, to, dir) : connectionChecks.test(dir, to)) ? 1 : 0));
            }
            return cached == 1;
        }
    }

    @Override
    public List<BakedQuad> transformQuad(BakedQuad bq, ITextureContext context, int quadGoal) {
        Quad quad = makeQuad(bq, context);
        if (context == null) {
            return Collections.singletonList(quad.transformUVs(sprites[0]).rebake());
        }

        Quad[] quads = quad.subdivide(4);
        
        int[] ctm = ((TextureContextCTM)context).getCTM(bq.getFace()).getSubmapIndices();
        
        for (int i = 0; i < quads.length; i++) {
            Quad q = quads[i];
            if (q != null) {
                int ctmid = q.getUvs().normalize().getQuadrant();
                quads[i] = q.grow().transformUVs(sprites[ctm[ctmid] > 15 ? 0 : 1], CTMLogic.uvs[ctm[ctmid]].normalize());
            }
        }
        return Arrays.stream(quads).filter(Objects::nonNull).map(q -> q.rebake()).collect(Collectors.toList());
    }
    
    @Override
    protected Quad makeQuad(BakedQuad bq, ITextureContext context) {
        return super.makeQuad(bq, context).derotate();
    }
}
