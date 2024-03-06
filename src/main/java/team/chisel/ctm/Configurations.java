package team.chisel.ctm;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;

public class Configurations {

    public static final Configurations INSTANCE = new Configurations();

    public static void register(ModContainer modContainer, IEventBus modBus) {
        modContainer.addConfig(new ModConfig(Type.CLIENT, INSTANCE.configSpec, modContainer, "ctm.toml"));
        modBus.addListener(ModConfigEvent.Reloading.class, event -> {
            if (event.getConfig().getModId().equals(CTM.MOD_ID)) {
                //Only reload when our config changes
                AbstractCTMBakedModel.invalidateCaches();
                //Queue the change to occur on the render thread (as otherwise an exception will be thrown)
                RenderSystem.recordRenderCall(() -> Minecraft.getInstance().levelRenderer.allChanged());
            }
        });
    }

    private final ModConfigSpec configSpec;

    public final BooleanValue disableCTM;
    public final BooleanValue connectInsideCTM;

    private Configurations() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        disableCTM = builder.comment("Disable connected textures entirely").define("disableCTM", false);
        connectInsideCTM = builder.comment("Choose whether the inside corner is disconnected on a CTM block - http://imgur.com/eUywLZ4")
              .define("connectInsideCTM", false);
        configSpec = builder.build();
    }

    public static boolean isDisabled() {
        return INSTANCE.disableCTM.get();
    }

    public static boolean connectInsideCTM() {
        if (INSTANCE.configSpec.isLoaded()) {
            return INSTANCE.disableCTM.get();
        }
        return INSTANCE.disableCTM.getDefault();
    }
}
