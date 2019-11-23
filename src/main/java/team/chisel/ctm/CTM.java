package team.chisel.ctm;

import static team.chisel.ctm.CTM.MOD_ID;
import static team.chisel.ctm.CTM.MOD_NAME;
import static team.chisel.ctm.CTM.VERSION;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import team.chisel.ctm.client.model.parsing.ModelLoaderCTM;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;
import team.chisel.ctm.client.texture.type.TextureTypeRegistry;
import team.chisel.ctm.client.util.CTMPackReloadListener;
import team.chisel.ctm.client.util.TextureMetadataHandler;

@Mod(name = MOD_NAME, modid = MOD_ID, version = VERSION, dependencies = "before:chisel;after:forge@[14.23,)", clientSideOnly = true)
public class CTM {
    
    public static final String MOD_ID = "ctm";
    public static final String MOD_NAME = "CTM";
    public static final String DOMAIN = MOD_ID;
    public static final String VERSION = "@VERSION@";

    public static final Logger logger = LogManager.getLogger("CTM");
    
    @Mod.Instance(MOD_ID)
    public static CTM instance;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        TextureTypeRegistry.preInit(event);

        ModelLoaderRegistry.registerLoader(ModelLoaderCTM.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ModelLoaderCTM.INSTANCE);
        Minecraft.getMinecraft().metadataSerializer_.registerMetadataSectionType(new IMetadataSectionCTM.Serializer(), IMetadataSectionCTM.class);
        
        MinecraftForge.EVENT_BUS.register(TextureMetadataHandler.INSTANCE);
        ((SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(CTMPackReloadListener.INSTANCE);
    }
}
