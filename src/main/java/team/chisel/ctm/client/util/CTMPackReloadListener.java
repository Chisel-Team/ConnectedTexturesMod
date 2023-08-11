package team.chisel.ctm.client.util;

import com.mojang.datafixers.util.Unit;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;

public class CTMPackReloadListener extends SimplePreparableReloadListener<Unit> {
    
    @Override
    protected Unit prepare(ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
        return Unit.INSTANCE;
    }

    @Override
    protected void apply(Unit objectIn, ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
        ResourceUtil.invalidateCaches();
        TextureMetadataHandler.INSTANCE.invalidateCaches();
        TextureMetadataHandler.TEXTURES_SCRAPED.clear();
        AbstractCTMBakedModel.invalidateCaches();
    }
}
