package team.chisel.ctm;

import static team.chisel.ctm.CTM.MOD_ID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
    	
    	FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
    }
    
    @SubscribeEvent
    public void preInit(FMLClientSetupEvent event) {
        TextureTypeRegistry.preInit(event);

        ModelLoaderRegistry.registerLoader(new ResourceLocation(MOD_ID, "ctm"), ModelLoaderCTM.INSTANCE);
        
        MinecraftForge.EVENT_BUS.register(TextureMetadataHandler.INSTANCE);
        ((SimpleReloadableResourceManager)Minecraft.getInstance().getResourceManager()).addReloadListener(CTMPackReloadListener.INSTANCE);

        // TODO 1.15 temp
        RenderTypeLookup.setRenderLayer(Blocks.REDSTONE_BLOCK, RenderType.getCutoutMipped());
    }
}
