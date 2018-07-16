package team.chisel.ctm.client.asm;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.WeightedBakedModel;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.client.model.IModel;
import team.chisel.ctm.CTM;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;
import team.chisel.ctm.client.util.ProfileUtil;

public class CTMCoreMethods {
    
    @SneakyThrows
    public static Boolean canRenderInLayer(@Nonnull IBlockState state, @Nonnull BlockRenderLayer layer) {
        ProfileUtil.start("ctm_render_in_layer");
        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);
        if (model instanceof WeightedBakedModel) {
            model = ((WeightedBakedModel)model).baseModel;
        }
        
        Boolean ret;
        if (model instanceof AbstractCTMBakedModel) {
            ret = ((AbstractCTMBakedModel)model).getModel().canRenderInLayer(state, layer);
        } else {
            ret = null;
        }
        ProfileUtil.end();
        return ret;
    }
    
    public static ThreadLocal<Boolean> renderingDamageModel = ThreadLocal.withInitial(() -> false);
    
    public static void preDamageModel() {
        renderingDamageModel.set(true);
    }
    
    public static void postDamageModel() {
        renderingDamageModel.set(false);
    }
    
    public static IModel transformParent(IModel model) {
        if (model instanceof IModelCTM) {
            try {
                return ((IModelCTM) model).getVanillaParent();
            } catch (Throwable t) {
                CTM.logger.error("Please update Chisel!");
            }
        }
        return model;
    }
}
