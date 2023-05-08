package team.chisel.ctm.client.mixin;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import team.chisel.ctm.client.util.TextureMetadataHandler;

@Mixin(targets = {"net.minecraft.client.resources.model.ModelBakery$ModelBakerImpl"})
public abstract class TextureScrapingMixin {
    
    @Shadow
    @Mutable
    private Function<Material, TextureAtlasSprite> modelTextureGetter;
    
    @Inject(at = @At("TAIL"), method = "<init>")
    private void _createWrapper(ModelBakery outer, BiFunction<ResourceLocation, Material, TextureAtlasSprite> p_249651_, ResourceLocation p_251408_, CallbackInfo callback) {
        final var oldGetter = this.modelTextureGetter;
        this.modelTextureGetter = (mat) -> {
            TextureMetadataHandler.TEXTURES_SCRAPED.put(p_251408_, mat);
            return oldGetter.apply(mat);
        };
    }
}
