package team.chisel.ctm.api.util;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Maps;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.custom_hash.TObjectLongCustomHashMap;
import gnu.trove.strategy.IdentityHashingStrategy;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;

/**
 * List of IBlockRenderContext's
 */
@ParametersAreNonnullByDefault
public class RenderContextList {
    
    private final Map<ITextureType, ITextureContext> contextMap = Maps.newIdentityHashMap();
    private final TObjectLongMap<ITextureType> serialized = new TObjectLongCustomHashMap<>(new IdentityHashingStrategy<>());

    public RenderContextList(IBlockState state, Collection<ICTMTexture<?>> textures, IBlockAccess world, BlockPos pos) {        
        for (ICTMTexture<?> tex : textures) {
            ITextureType type = tex.getType();
            ITextureContext ctx = type.getBlockRenderContext(state, world, pos, tex);
            if (ctx != null) {
                contextMap.put(type, ctx);
            }
        }
        
        for (Entry<ITextureType, ITextureContext> e : contextMap.entrySet()) {
            serialized.put(e.getKey(), e.getValue().getCompressedData());
        }
    }

    public @Nullable ITextureContext getRenderContext(ITextureType type) {
        return this.contextMap.get(type);
    }

    public boolean contains(ITextureType type) {
        return getRenderContext(type) != null;
    }

    public TObjectLongMap<ITextureType> serialized() {
        return serialized;
    }
}
