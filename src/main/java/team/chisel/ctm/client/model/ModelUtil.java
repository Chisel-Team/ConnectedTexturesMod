package team.chisel.ctm.client.model;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.IdentityHashMap;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.base.Throwables;

import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ItemModelMesherForge;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class ModelUtil {
    
    private static final MethodHandle _locations;
    static {
        try {
            _locations = MethodHandles.lookup().unreflectGetter(ReflectionHelper.findField(ItemModelMesherForge.class, "locations"));
        } catch (IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Look up a MRL for a given ItemStack.
     * 
     * @param stack
     *            The ItemStack.
     * @return The MRL definition, or null if none exists.
     */
    @SneakyThrows
    public static @Nullable ModelResourceLocation getMesh(ItemStack stack) {

        ItemModelMesher mesher = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
        
        // First try simple damage overrides
        ModelResourceLocation modelResourceLocation = Optional.ofNullable(
                ((IdentityHashMap<Item, TIntObjectHashMap<ModelResourceLocation>>) _locations.invoke(mesher)))
                        .map(map -> map.get(stack.getItem()))
                        .map(map -> map.get(mesher.getMetadata(stack)))
                        .orElse(null);
        
        // Next, try mesh definitions
        if (modelResourceLocation == null) {
            ItemMeshDefinition itemMeshDefinition = mesher.shapers.get(stack.getItem());
            modelResourceLocation = Optional.ofNullable(itemMeshDefinition).map(mesh -> mesh.getModelLocation(stack)).orElse(null);
        }

        return modelResourceLocation;
    }

}
