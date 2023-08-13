package team.chisel.ctm.client.util;

import java.util.function.Function;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

public class SpriteFunctionWrapper implements Function<Material, TextureAtlasSprite> {

    private final Function<Material, TextureAtlasSprite> internal;
    private final ResourceLocation modelLocation;

    public SpriteFunctionWrapper(Function<Material, TextureAtlasSprite> internal, ResourceLocation modelLocation) {
        if (internal instanceof SpriteFunctionWrapper wrapper) {
            this.internal = wrapper.internal;
        } else {
            this.internal = internal;
        }
        this.modelLocation = modelLocation;
    }

    @Override
    public TextureAtlasSprite apply(Material material) {
        TextureMetadataHandler.INSTANCE.textureScraped(modelLocation, material);
        return internal.apply(material);
    }
}