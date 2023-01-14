package team.chisel.ctm.mixins;

import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.chisel.ctm.client.util.CTMPackReloadListener;

@Mixin(WeightedBakedModel.class)
public class WeightedBakedModelMixin {
    @Inject(method = "getRenderTypes", at = @At("HEAD"), cancellable = true, remap = false)
    private void getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data, CallbackInfoReturnable<ChunkRenderTypeSet> cir) {
        cir.setReturnValue(CTMPackReloadListener.CachingLayerCheck.renderTypeSet(state, ((WeightedBakedModel) (Object) this).list, WeightedEntry.Wrapper::getData));
    }
}
