package team.chisel.ctm.client.model;

import gnu.trove.map.TIntObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ItemModelMesherForge;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.registries.IRegistryDelegate;
import team.chisel.ctm.CTM;

public class ModelUtil {
    
    private static final MethodHandle _locations;
    static {
        try {
            _locations = MethodHandles.lookup().unreflectGetter(ReflectionHelper.findField(ItemModelMesherForge.class, "locations"));
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

        ItemModelMesher mesher = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
        
        // First try simple damage overrides
        Object locations = _locations.invokeExact(mesher);
        if (locations != null) {
        	locations = ((Map<IRegistryDelegate<Item>, ?>) locations).get(stack.getItem().delegate);
        }
        ModelResourceLocation modelResourceLocation;
        int meta = mesher.getMetadata(stack);
        if (locations instanceof TIntObjectMap) {
        	modelResourceLocation = ((TIntObjectMap<ModelResourceLocation>)locations).get(meta);
        } else if (locations instanceof Int2ObjectMap) {
        	modelResourceLocation = ((Int2ObjectMap<ModelResourceLocation>)locations).get(meta);
        } else {
        	CTM.logger.error("Could not determine type of mesher locations.");
        	modelResourceLocation = null;
        }
        
        // Next, try mesh definitions
        if (modelResourceLocation == null) {
            ItemMeshDefinition itemMeshDefinition = mesher.shapers.get(stack.getItem());
            modelResourceLocation = Optional.ofNullable(itemMeshDefinition).map(mesh -> mesh.getModelLocation(stack)).orElse(null);
        }

        return modelResourceLocation;
    }

}
