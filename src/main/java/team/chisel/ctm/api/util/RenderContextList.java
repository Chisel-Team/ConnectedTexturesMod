package team.chisel.ctm.api.util;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import team.chisel.ctm.CTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.client.util.IdentityStrategy;
import team.chisel.ctm.client.util.RegionCache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;

/**
 * List of IBlockRenderContext's
 */
@ParametersAreNonnullByDefault
public class RenderContextList {
    
    private static final Cache<IBlockAccess, RegionCache> regionMetaCache = CacheBuilder
    		.newBuilder()
    		.weakKeys()
    		.weakValues()
    		.expireAfterWrite(10, TimeUnit.SECONDS)
    		.build();
    
    private final Map<ICTMTexture<?>, ITextureContext> contextMap = Maps.newIdentityHashMap();
    private final Object2LongMap<ICTMTexture<?>> serialized = new Object2LongOpenCustomHashMap<>(new IdentityStrategy<>());

    public RenderContextList(IBlockState state, Collection<ICTMTexture<?>> textures, final IBlockAccess world, BlockPos pos) {
    	IBlockAccess cachedWorld = world;
    	try {
    		cachedWorld = regionMetaCache.get(world, () -> new RegionCache(pos, 2, world));
    	} catch (ExecutionException e) {
    		CTM.logger.error(e);
    		// No-op, world reference remains as passed in
    	}
        for (ICTMTexture<?> tex : textures) {
            ITextureType type = tex.getType();
            ITextureContext ctx = type.getBlockRenderContext(state, cachedWorld, pos, tex);
            if (ctx != null) {
                contextMap.put(tex, ctx);
            }
        }
        
        for (Entry<ICTMTexture<?>, ITextureContext> e : contextMap.entrySet()) {
            serialized.put(e.getKey(), e.getValue().getCompressedData());
        }
    }

    public @Nullable ITextureContext getRenderContext(ICTMTexture<?> tex) {
        return this.contextMap.get(tex);
    }

    public boolean contains(ICTMTexture<?> tex) {
        return getRenderContext(tex) != null;
    }

    public Object2LongMap<ICTMTexture<?>> serialized() {
        return serialized;
    }
}
