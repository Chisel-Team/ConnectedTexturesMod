package team.chisel.ctm.client.mixin;

import java.util.function.BiFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;

@Mixin(targets = {"net.minecraft.client.resources.model.ModelBakery$ModelBakerImpl"})
public interface ModelBakerImplAccessor {
    
    @Invoker(value = "<init>")
    static ModelBakery.ModelBakerImpl createImpl(ModelBakery outer, BiFunction<ResourceLocation, Material, TextureAtlasSprite> p_249651_, ResourceLocation p_251408_) {
        throw new AssertionError();
    }
}