package team.chisel.ctm.client.model;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.RegistryAwareItemModelShaper;

@UtilityClass
public class ModelUtil {

    /**
     * Look up a MRL for a given ItemStack.
     * 
     * @param stack
     *            The ItemStack.
     * @return The MRL definition, or null if none exists.
     */
    @SuppressWarnings("UnstableApiUsage")
    @SneakyThrows
    public static ModelResourceLocation getMesh(ItemStack stack) {
        ItemModelShaper shaper = Minecraft.getInstance().getItemRenderer().getItemModelShaper();
        if (shaper instanceof RegistryAwareItemModelShaper registryAwareShaper) {
            return registryAwareShaper.getLocation(stack);
        }
        return ModelBakery.MISSING_MODEL_LOCATION;
    }

}
