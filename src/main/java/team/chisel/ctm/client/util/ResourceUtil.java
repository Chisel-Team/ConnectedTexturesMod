package team.chisel.ctm.client.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonParseException;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;

@UtilityClass
public class ResourceUtil {
    
    public static Resource getResource(TextureAtlasSprite sprite) throws IOException {
        return getResource(spriteToAbsolute(sprite.getName()));
    }
    
    public static ResourceLocation spriteToAbsolute(ResourceLocation sprite) {
        if (!sprite.getPath().startsWith("textures/")) {
            sprite = new ResourceLocation(sprite.getNamespace(), "textures/" + sprite.getPath());
        }
        if (!sprite.getPath().endsWith(".png")) {
            sprite = new ResourceLocation(sprite.getNamespace(), sprite.getPath() + ".png");
        }
        return sprite;
    }
    
    public static Resource getResource(ResourceLocation res) throws IOException {
        return Minecraft.getInstance().getResourceManager().getResource(res);
    }
    
    public static Resource getResourceUnsafe(ResourceLocation res) {
        try {
            return getResource(res);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static final Map<ResourceLocation, IMetadataSectionCTM> metadataCache = new HashMap<>();
    private static final IMetadataSectionCTM.Serializer SERIALIZER = new IMetadataSectionCTM.Serializer();

    public static @Nullable IMetadataSectionCTM getMetadata(ResourceLocation res) throws IOException {
        // Note, semantically different from computeIfAbsent, as we DO care about keys mapped to null values
        if (metadataCache.containsKey(res)) {
            return metadataCache.get(res);
        }
        IMetadataSectionCTM ret;
        try (Resource resource = getResource(res)) {
            ret = resource.getMetadata(SERIALIZER);
        } catch (FileNotFoundException e) {
            ret = null;  
        } catch (JsonParseException e) {
            throw new IOException("Error loading metadata for location " + res, e);
        }
        metadataCache.put(res, ret);
        return ret;
    }
    
    public static @Nullable IMetadataSectionCTM getMetadata(TextureAtlasSprite sprite) throws IOException {
        return getMetadata(spriteToAbsolute(sprite.getName()));
    }
    
    public static @Nullable IMetadataSectionCTM getMetadataUnsafe(TextureAtlasSprite sprite) {
        try {
            return getMetadata(sprite);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void invalidateCaches() {
        metadataCache.clear();
    }
}
