package team.chisel.ctm.client.texture.type;

import com.google.common.collect.Maps;
import team.chisel.ctm.api.texture.ITextureType;

import java.util.Map;

/**
 * Registry for all the different texture types
 */
@Deprecated
public class TextureTypeRegistry {
    private static Map<String, ITextureType> map = Maps.newHashMap();

    public static void register(String name, ITextureType type) {
        TextureTypes.register(type, name);
    }

    public static ITextureType getType(String name) {
        return TextureTypes.getType(name).orElse(null);
    }

    public static boolean isValid(String name) {
        return TextureTypes.isPresent(name);
    }
}
