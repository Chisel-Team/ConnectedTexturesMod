package team.chisel.ctm.client.texture.ctx;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import team.chisel.ctm.api.IOffsetProvider;

@ParametersAreNonnullByDefault
public enum OffsetProviderRegistry {
    
    INSTANCE;
    
    private List<IOffsetProvider> providers = new ArrayList<>();
    
    public void registerProvider(IOffsetProvider provider) {
        this.providers.add(provider);
    }
    
    public BlockPos getOffset(BlockAndTintGetter world, BlockPos pos) {
        BlockPos ret = BlockPos.ZERO;
        for (IOffsetProvider p : providers) {
            ret = ret.offset(p.getOffset(world, pos));
        }
        return ret;
    }

}
