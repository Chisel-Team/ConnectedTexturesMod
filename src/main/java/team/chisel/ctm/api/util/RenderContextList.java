package team.chisel.ctm.api.util;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.client.util.IdentityStrategy;
import team.chisel.ctm.client.util.RegionCache;

import com.google.common.collect.Maps;

/**
 * List of IBlockRenderContext's
 */
@ParametersAreNonnullByDefault
public class RenderContextList {

    /**
     * This Map is weird, to avoid GC issues where IBlockAccess instances aren't cleaned,
     * the IBlockAccess passed into RegionCache is wrapped in a WeakReference,
     * otherwise the WeakHashMap will never remove entries as there is always a non weak reference
     * back to the key.
     * It is preferable to wrap RegionCache's reference to IBlockAccess in a Wreak Reference
     * instead of the value of this map due to a mid render GC, this cache wouldn't really be
     * a cache then as it could be cleared.
     * The IBlockAccesses running through this are basically disposed after rebuilding the chunk.
     */
    private static final ThreadLocal<WeakHashMap<IBlockAccess, RegionCache>> regionMetaCache = ThreadLocal.withInitial(WeakHashMap::new);
    
    private final Map<ICTMTexture<?>, ITextureContext> contextMap = Maps.newIdentityHashMap();
    private final Object2LongMap<ICTMTexture<?>> serialized = new Object2LongOpenCustomHashMap<>(new IdentityStrategy<>());

    public RenderContextList(IBlockState state, Collection<ICTMTexture<?>> textures, IBlockAccess world, BlockPos pos) {
        world = regionMetaCache.get().computeIfAbsent(world, w -> new RegionCache(pos, 2, w));
        for (ICTMTexture<?> tex : textures) {
            ITextureType type = tex.getType();
            ITextureContext ctx = type.getBlockRenderContext(state, world, pos, tex);
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
