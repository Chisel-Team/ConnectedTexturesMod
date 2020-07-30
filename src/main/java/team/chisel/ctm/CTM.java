package team.chisel.ctm;

import static team.chisel.ctm.CTM.MOD_ID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import team.chisel.ctm.client.model.parsing.ModelLoaderCTM;
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
    
    public CTM() {
    	instance = this;
    	
    	DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
    	    IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
    	    ModelLoaderRegistry.registerLoader(new ResourceLocation(MOD_ID, "ctm"), ModelLoaderCTM.INSTANCE);
    	    modBus.register(TextureMetadataHandler.INSTANCE);
    	    modBus.register(new CTMPackReloadListener());
    	    
            TextureTypeRegistry.scan();
    	});
    }
}
