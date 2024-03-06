package team.chisel.ctm;

import static team.chisel.ctm.CTM.MOD_ID;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.IExtensionPoint.DisplayTest;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.DistExecutor;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import team.chisel.ctm.client.model.parsing.ModelLoaderCTM;
import team.chisel.ctm.client.newctm.json.CTMDefinitionManager;
import team.chisel.ctm.client.texture.type.TextureTypeRegistry;
import team.chisel.ctm.client.util.CTMPackReloadListener;
import team.chisel.ctm.client.util.TextureMetadataHandler;

@Mod(MOD_ID)
public class CTM {
    
    public static final String MOD_ID = "ctm";
    public static final String MOD_NAME = "CTM";
    public static final String DOMAIN = MOD_ID;

    public static final Logger logger = LogManager.getLogger("CTM");
    
    public static CTM instance;
    @Getter
    private CTMDefinitionManager definitionManager;
    @Getter
    private CTMPackReloadListener reloadListener;

    public CTM(ModContainer modContainer, IEventBus modBus) {
    	instance = this;
        if (FMLEnvironment.dist.isClient()) {
    	    modBus.addListener(this::modelRegistry);
    	    modBus.register(TextureMetadataHandler.INSTANCE);
            Configurations.register(modContainer, modBus);
    	    
            TextureTypeRegistry.scan();

            definitionManager = new CTMDefinitionManager();
            ReloadableResourceManager resourceManager = (ReloadableResourceManager) Minecraft.getInstance().getResourceManager();
            resourceManager.registerReloadListener(definitionManager.getReloadListener());
            reloadListener = new CTMPackReloadListener();
            modBus.addListener(this::reloadListenersLate);
    	}
        ModLoadingContext.get().registerExtensionPoint(DisplayTest.class, () -> new DisplayTest(() -> DisplayTest.IGNORESERVERONLY, (a, b) -> true));
    }

    private void modelRegistry(ModelEvent.RegisterGeometryLoaders event) {
        event.register(new ResourceLocation(MOD_ID, "ctm"), ModelLoaderCTM.INSTANCE);
    }
    
    private void reloadListenersLate(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(reloadListener);
    }
}
