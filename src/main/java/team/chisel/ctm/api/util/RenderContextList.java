package team.chisel.ctm.api.util;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Maps;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
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
    private final TLongSet serialized = new TLongHashSet();

    public RenderContextList(IBlockState state, Collection<ICTMTexture<?>> textures, IBlockAccess world, BlockPos pos) {        
        for (ICTMTexture<?> tex : textures) {
            ITextureType type = tex.getType();
            ITextureContext ctx = type.getBlockRenderContext(state, world, pos, tex);
            if (ctx != null) {
                contextMap.put(type, ctx);
            }
        }
        
        for (ITextureContext ctx : contextMap.values()) {
            serialized.add(ctx.getCompressedData());
        }
    }

    public @Nullable ITextureContext getRenderContext(ITextureType type) {
        return this.contextMap.get(type);
    }

    public boolean contains(ITextureType type) {
        return getRenderContext(type) != null;
    }

    public TLongSet serialized() {
        return serialized;
    }
}
