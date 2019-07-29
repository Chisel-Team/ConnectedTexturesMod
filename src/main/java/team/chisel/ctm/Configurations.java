package team.chisel.ctm;

import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;

@EventBusSubscriber(modid = CTM.MOD_ID, bus = Bus.MOD)
public class Configurations {
    
//    @Config.Comment("Disable connected textures entirely.")
    public static boolean disableCTM = false;
    
//    @Config.Comment("Choose whether the inside corner is disconnected on a CTM block - http://imgur.com/eUywLZ4")
    public static boolean connectInsideCTM = false;

    @SubscribeEvent
    public static void onConfigChange(ModConfig.Reloading event) {
        AbstractCTMBakedModel.invalidateCaches();
        Minecraft.getInstance().worldRenderer.loadRenderers();
    }
}
