package team.chisel.ctm.client.util;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;
import team.chisel.ctm.client.model.parsing.ModelLoaderCTM;

public enum CTMPackReloadListener implements IResourceManagerReloadListener {
    
    INSTANCE;

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        ResourceUtil.invalidateCaches();
        AbstractCTMBakedModel.invalidateCaches();
        ModelLoaderCTM.parsedLocations.clear();
        TextureMetadataHandler.INSTANCE.invalidateCaches();
    }
}
