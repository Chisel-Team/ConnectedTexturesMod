package team.chisel.ctm.api.util;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.custom_hash.TObjectLongCustomHashMap;
import gnu.trove.strategy.IdentityHashingStrategy;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Maps;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.client.util.ProfileUtil;
import team.chisel.ctm.client.util.RegionCache;

/**
 * List of IBlockRenderContext's
 */
@ParametersAreNonnullByDefault
public class RenderContextList {
    
    private static final ThreadLocal<RegionCache> regionMetaCache = ThreadLocal.withInitial(
            () -> new RegionCache(BlockPos.ORIGIN, 0, null));
    
    private final Map<ICTMTexture<?>, ITextureContext> contextMap = Maps.newIdentityHashMap();
    private final TObjectLongMap<ICTMTexture<?>> serialized = new TObjectLongCustomHashMap<>(new IdentityHashingStrategy<>());

    public RenderContextList(IBlockState state, Collection<ICTMTexture<?>> textures, final IBlockAccess world, BlockPos pos) {
        ProfileUtil.start("ctm_region_cache_update");
    	IBlockAccess cachedWorld = regionMetaCache.get().updateWorld(world);
    	
    	ProfileUtil.endAndStart("ctm_context_gather");
        for (ICTMTexture<?> tex : textures) {
            ITextureType type = tex.getType();
            ITextureContext ctx = type.getBlockRenderContext(state, cachedWorld, pos, tex);
            if (ctx != null) {
                contextMap.put(tex, ctx);
            }
        }
        
        ProfileUtil.endAndStart("ctm_context_serialize");
        for (Entry<ICTMTexture<?>, ITextureContext> e : contextMap.entrySet()) {
            serialized.put(e.getKey(), e.getValue().getCompressedData());
        }
        ProfileUtil.end();
    }

    public @Nullable ITextureContext getRenderContext(ICTMTexture<?> tex) {
        return this.contextMap.get(tex);
    }

    public boolean contains(ICTMTexture<?> tex) {
        return getRenderContext(tex) != null;
    }

    public TObjectLongMap<ICTMTexture<?>> serialized() {
        return serialized;
    }
}
