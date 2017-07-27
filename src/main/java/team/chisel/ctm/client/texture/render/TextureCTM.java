package team.chisel.ctm.client.texture.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import gnu.trove.impl.Constants;
import gnu.trove.map.TObjectByteMap;
import gnu.trove.map.custom_hash.TObjectByteCustomHashMap;
import gnu.trove.strategy.IdentityHashingStrategy;
import lombok.Getter;
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
	private final BiPredicate<EnumFacing, IBlockState> connectionChecks;
	
	private final EnumMap<EnumFacing, TObjectByteMap<IBlockState>> connectionCache = new EnumMap<>(EnumFacing.class);
	{
	    for (EnumFacing dir : EnumFacing.VALUES) {
	        connectionCache.put(dir, new TObjectByteCustomHashMap<>(new IdentityHashingStrategy<>(), Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, (byte) -1));
	    }
	}

    public TextureCTM(T type, TextureInfo info) {
        super(type, info);
        this.connectInside = info.getInfo().flatMap(obj -> ParseUtils.getBoolean(obj, "connectInside"));
        this.ignoreStates = info.getInfo().map(obj -> JsonUtils.getBoolean(obj, "ignoreStates", false)).orElse(false);
        this.connectionChecks = info.getInfo().map(obj -> predicateParser.parse(obj.get("connectTo"))).orElse(null);
    }
    
    public boolean connectTo(CTMLogic ctm, IBlockState from, IBlockState to, EnumFacing dir) {
        synchronized (connectionCache) {
            byte cached = connectionCache.get(dir).get(to);
            if (cached == -1) {
                connectionCache.get(dir).put(to, cached = (byte) ((connectionChecks == null ? StateComparisonCallback.DEFAULT.connects(ctm, from, to, dir) : connectionChecks.test(dir, to)) ? 1 : 0));
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
