package team.chisel.ctm.client.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Throwables;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import team.chisel.ctm.client.texture.MetadataSectionCTM;

public class ResourceUtil {
    
    public static ResourceLocation toResourceLocation(TextureAtlasSprite sprite) {
        return new ResourceLocation(sprite.getIconName());
    }
    
    public static IResource getResource(TextureAtlasSprite sprite) throws IOException {
        return getResource(spriteToAbsolute(toResourceLocation(sprite)));
    }
    
    public static ResourceLocation spriteToAbsolute(ResourceLocation sprite) {
        if (!sprite.getResourcePath().startsWith("textures/")) {
            sprite = new ResourceLocation(sprite.getResourceDomain(), "textures/" + sprite.getResourcePath());
        }
        if (!sprite.getResourcePath().endsWith(".png")) {
            sprite = new ResourceLocation(sprite.getResourceDomain(), sprite.getResourcePath() + ".png");
        }
        return sprite;
    }
    
    public static IResource getResource(ResourceLocation res) throws IOException {
        return Minecraft.getMinecraft().getResourceManager().getResource(res);
    }
    
    public static IResource getResourceUnsafe(ResourceLocation res) {
        try {
            return getResource(res);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
    
    private static final Map<ResourceLocation, MetadataSectionCTM> metadataCache = new HashMap<>();

    public static @Nullable MetadataSectionCTM getMetadata(ResourceLocation res) throws IOException {
        // Note, semantically different from computeIfAbsent, as we DO care about keys mapped to null values
        if (metadataCache.containsKey(res)) {
            return metadataCache.get(res);
        }
        MetadataSectionCTM ret;
        metadataCache.put(res, ret = getResource(res).getMetadata(MetadataSectionCTM.SECTION_NAME));
        return ret;
    }
    
    public static @Nullable MetadataSectionCTM getMetadata(TextureAtlasSprite sprite) throws IOException {
        return getMetadata(spriteToAbsolute(toResourceLocation(sprite)));
    }
    
    public static @Nullable MetadataSectionCTM getMetadataUnsafe(TextureAtlasSprite sprite) {
        try {
            return getMetadata(sprite);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
    
    public static void invalidateCaches() {
        metadataCache.clear();
    }
}
