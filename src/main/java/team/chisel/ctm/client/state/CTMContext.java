package team.chisel.ctm.client.state;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import team.chisel.ctm.api.util.RenderContextList;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;

@ParametersAreNonnullByDefault
public class CTMContext  {
    
    @Getter
    private final BlockAndTintGetter world;
    @Getter
    private final BlockPos pos;
    
    private @Nullable RenderContextList ctxCache;
    
    public CTMContext(BlockAndTintGetter world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
    }

    public CTMContext(CTMContext parent) {
        this(parent.world, parent.pos);
    }
    
    public RenderContextList getContextList(BlockState state, AbstractCTMBakedModel model) {
        if (ctxCache == null) {
            ctxCache = new RenderContextList(state, model.getCTMTextures(), world, pos);
        }
        return ctxCache;
    }
}
