package team.chisel.ctm;

import static team.chisel.ctm.CTM.MOD_ID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
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
    public static final String VERSION = "@VERSION@";

    public static final Logger logger = LogManager.getLogger("CTM");
    
    public static CTM instance;
    @Getter
    private CTMDefinitionManager definitionManager;
    @Getter
    private CTMPackReloadListener reloadListener;
    
    public CTM() {
    	instance = this;
    	
    	DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
    	    IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
    	    modBus.addListener(this::modelRegistry);
    	    modBus.register(TextureMetadataHandler.INSTANCE);
    	    
            TextureTypeRegistry.scan();
            
            definitionManager = new CTMDefinitionManager();
            ReloadableResourceManager resourceManager = (ReloadableResourceManager) Minecraft.getInstance().getResourceManager();
            resourceManager.registerReloadListener(definitionManager.getReloadListener());
            reloadListener = new CTMPackReloadListener();
            modBus.addListener(this::reloadListenersLate);
    	});
    	
      ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }
    
    private void modelRegistry(ModelEvent.RegisterGeometryLoaders event) {
        event.register("ctm", ModelLoaderCTM.INSTANCE);
    }
    
    private void reloadListenersLate(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(reloadListener);
    }
}
