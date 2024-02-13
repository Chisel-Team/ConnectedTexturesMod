package team.chisel.ctm.client.mixin;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.chisel.ctm.client.BlockModelExtension;
import team.chisel.ctm.client.model.ModelBakedCTM;
import team.chisel.ctm.client.model.ModelCTM;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@Mixin(BlockModel.class)
public abstract class BlockModelMixin implements UnbakedModel, BlockModelExtension {

    @Shadow public abstract BakedModel bake(ModelBakery pBakery, BlockModel pModel, Function<Material, TextureAtlasSprite> pSpriteGetter, ModelState pTransform, ResourceLocation pLocation, boolean pGuiLight3d);

    @Shadow public abstract Material getMaterial(String pName);

    @Shadow public abstract String toString();

    @Unique
    protected Int2ObjectMap<IMetadataSectionCTM> metaOverrides = new Int2ObjectArrayMap<>();

    @Inject(at = @At("TAIL"), method = "bake(Lnet/minecraft/client/resources/model/ModelBakery;Lnet/minecraft/client/renderer/block/model/BlockModel;Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/client/resources/model/BakedModel;",
            cancellable = true)
    private void ctm$bake(ModelBakery pBakery, BlockModel pModel, Function<Material, TextureAtlasSprite> pSpriteGetter, ModelState pTransform, ResourceLocation pLocation, boolean pGuiLight3d, CallbackInfoReturnable<BakedModel> cir) {
        if (pModel instanceof BlockModelExtension extension && !extension.getMetaOverrides().isEmpty()) {
            ModelCTM model = new ModelCTM(pModel);
            model.initializeTextures(pBakery, pSpriteGetter);
            ModelBakedCTM bakedCTM = new ModelBakedCTM(model, cir.getReturnValue());
            cir.setReturnValue(bakedCTM);
        }
    }

    @Inject(at = @At("RETURN"), method = "getMaterials")
    private void ctm$materials(Function<ResourceLocation, UnbakedModel> pModelGetter, Set<Pair<String, String>> pMissingTextureErrors, CallbackInfoReturnable<Collection<Material>> cir) {
        for (IMetadataSectionCTM meta : getMetaOverrides().values()) {
            for (String s : meta.getAdditionalTextures()) {
                Material material = this.getMaterial(s);

                if (Objects.equals(material.texture(), MissingTextureAtlasSprite.getLocation())) {
                    pMissingTextureErrors.add(Pair.of(s, this.toString()));
                }

                cir.getReturnValue().add(material);
            }
        }
    }

    private Int2ObjectMap<IMetadataSectionCTM> ctm$merge(Int2ObjectMap<IMetadataSectionCTM> other) {
        Int2ObjectMap<IMetadataSectionCTM> copy = new Int2ObjectArrayMap<>(this.metaOverrides).clone();
        for (int i: other.keySet()) {
            if (!metaOverrides.containsKey(i)) {
                copy.put(i, other.get(i));
            } else {
                copy.get(i).merge(other.get(i));
            }
        }
        return copy;
    }

    public Int2ObjectMap<IMetadataSectionCTM> getMetaOverrides() {
        if (((BlockModel) (Object) this).parent != null ) {
            return ctm$merge(((BlockModelMixin) (Object) ((BlockModel) (Object) this).parent).getMetaOverrides());
        }
        return metaOverrides;
    }

    @Override
    public void setMetaOverrides(Int2ObjectMap<IMetadataSectionCTM> overrides) {
        this.metaOverrides = overrides;
    }
}
