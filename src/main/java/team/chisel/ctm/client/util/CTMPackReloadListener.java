package team.chisel.ctm.client.util;

import java.util.function.Predicate;

import net.minecraft.resources.IResourceManager;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.resource.VanillaResourceType;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;

public enum CTMPackReloadListener implements ISelectiveResourceReloadListener {
    
    INSTANCE;

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> predicate) {
    	if (predicate.test(VanillaResourceType.TEXTURES)) {
    		ResourceUtil.invalidateCaches();
	        TextureMetadataHandler.INSTANCE.invalidateCaches();
    	}
    	if (predicate.test(VanillaResourceType.MODELS)) {
	        AbstractCTMBakedModel.invalidateCaches();
    	}
    }
}
