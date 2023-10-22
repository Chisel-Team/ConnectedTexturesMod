package team.chisel.ctm.client.model;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import javax.annotation.Nullable;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.ForgeItemModelShaper;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;

@UtilityClass
public class ModelUtil {
    
    private static final MethodHandle _locations;
    static {
        try {
            _locations = MethodHandles.lookup().unreflectGetter(ObfuscationReflectionHelper.findField(ForgeItemModelShaper.class, "locations"));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Look up a MRL for a given ItemStack.
     * 
     * @param stack
     *            The ItemStack.
     * @return The MRL definition, or null if none exists.
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static @Nullable ModelResourceLocation getMesh(ItemStack stack) {
        ItemModelShaper shaper = Minecraft.getInstance().getItemRenderer().getItemModelShaper();
        
        Object locations = _locations.invoke(shaper);
        if (locations != null) {
        	return ((Map<Holder.Reference<Item>, ModelResourceLocation>) locations).get(ForgeRegistries.ITEMS.getDelegateOrThrow(stack.getItem()));
        }
        return null;
    }

}
