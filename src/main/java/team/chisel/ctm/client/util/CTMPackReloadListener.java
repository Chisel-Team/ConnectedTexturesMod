package team.chisel.ctm.client.util;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;

public enum CTMPackReloadListener implements IResourceManagerReloadListener {
    
    INSTANCE;

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        ResourceUtil.invalidateCaches();
    }
}
