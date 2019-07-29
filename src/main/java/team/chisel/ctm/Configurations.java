package team.chisel.ctm;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;

@Config(modid = CTM.MOD_ID)
@EventBusSubscriber(modid = CTM.MOD_ID)
public class Configurations {
    
    @Config.Comment("Disable connected textures entirely.")
    public static boolean disableCTM = false;
    
    @Config.Comment("Choose whether the inside corner is disconnected on a CTM block - http://imgur.com/eUywLZ4")
    public static boolean connectInsideCTM = false;

    @SubscribeEvent
    public static void onConfigChange(ConfigChangedEvent event) {
        if (event.getModID().equals(CTM.MOD_ID)) {
            ConfigManager.sync(CTM.MOD_ID, Type.INSTANCE);
            AbstractCTMBakedModel.invalidateCaches();
            Minecraft.getMinecraft().renderGlobal.loadRenderers();
        }
    }
}
