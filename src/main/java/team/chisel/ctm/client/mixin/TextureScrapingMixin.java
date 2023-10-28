package team.chisel.ctm.client.mixin;

import java.util.function.Function;

import net.minecraft.client.resources.model.ModelState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import team.chisel.ctm.client.util.SpriteFunctionWrapper;

@Mixin(targets = {"net.minecraft.client.resources.model.ModelBakery$ModelBakerImpl"})
public abstract class TextureScrapingMixin {

    //Note: We don't remap this method as it is a forge added method
    @ModifyVariable(at = @At("HEAD"), method = "bake(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/resources/model/ModelState;Ljava/util/function/Function;)Lnet/minecraft/client/resources/model/BakedModel;", argsOnly = true, remap = false)
    private Function<Material, TextureAtlasSprite> ctm_scrapeTextures(Function<Material, TextureAtlasSprite> sprites, ResourceLocation modelLocation, ModelState modelState) {
        return new SpriteFunctionWrapper(sprites, modelLocation);
    }
}
