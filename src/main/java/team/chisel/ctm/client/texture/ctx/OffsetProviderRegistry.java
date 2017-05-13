package team.chisel.ctm.client.texture.ctx;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import team.chisel.ctm.api.IOffsetProvider;

@ParametersAreNonnullByDefault
public enum OffsetProviderRegistry {
    
    INSTANCE;
    
    private List<IOffsetProvider> providers = new ArrayList<>();
    
    public void registerProvider(IOffsetProvider provider) {
        this.providers.add(provider);
    }
    
    public BlockPos getOffset(World world, BlockPos pos) {
        BlockPos ret = BlockPos.ORIGIN;
        for (IOffsetProvider p : providers) {
            ret = ret.add(p.getOffset(world, pos));
        }
        return ret;
    }

}
