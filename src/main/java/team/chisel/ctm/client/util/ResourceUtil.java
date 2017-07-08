package team.chisel.ctm.client.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Throwables;
import com.google.gson.JsonParseException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;

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
    
    private static final Map<ResourceLocation, IMetadataSectionCTM> metadataCache = new HashMap<>();

    public static @Nullable IMetadataSectionCTM getMetadata(ResourceLocation res) throws IOException {
        // Note, semantically different from computeIfAbsent, as we DO care about keys mapped to null values
        if (metadataCache.containsKey(res)) {
            return metadataCache.get(res);
        }
        IMetadataSectionCTM ret;
        try {
            ret = getResource(res).getMetadata(IMetadataSectionCTM.SECTION_NAME);
        } catch (FileNotFoundException e) {
            ret = null;  
        } catch (JsonParseException e) {
            throw new IOException("Error loading metadata for location " + res, e);
        }
        metadataCache.put(res, ret);
        return ret;
    }
    
    public static @Nullable IMetadataSectionCTM getMetadata(TextureAtlasSprite sprite) throws IOException {
        return getMetadata(spriteToAbsolute(toResourceLocation(sprite)));
    }
    
    public static @Nullable IMetadataSectionCTM getMetadataUnsafe(TextureAtlasSprite sprite) {
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
