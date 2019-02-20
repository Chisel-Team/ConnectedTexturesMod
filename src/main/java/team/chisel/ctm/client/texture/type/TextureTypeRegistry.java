package team.chisel.ctm.client.texture.type;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.texture.TextureTypeList;

/**
 * Registry for all the different texture types
 */
public class TextureTypeRegistry {

    private static Map<String, ITextureType> map = Maps.newHashMap();

    @SuppressWarnings("unchecked")
    public static void preInit(FMLClientSetupEvent event) {
        Multimap<ASMData, String> annots = HashMultimap.create();
        for (ASMData list : event.getAsmData().getAll(TextureTypeList.class.getName())) {
            for (String value : ((List<Map<String, String>>) list.getAnnotationInfo().get("value")).stream().map(m -> m.get("value")).collect(Collectors.toList())) {
                annots.put(list, value);
            }
        }
        for (ASMData single : event.getAsmData().getAll(TextureType.class.getName())) {
            if (single.getObjectName() != null) {
                annots.put(single, (String) single.getAnnotationInfo().get("value"));
            }
        }
        for (Entry<ASMData, Collection<String>> data : annots.asMap().entrySet()) {
            ITextureType type;
            try {
                type = ((Class<? extends ITextureType>) Class.forName(data.getKey().getClassName())).newInstance();
            } catch (InstantiationException e) {
                // This might be a field, let's try that
                try {
                    Class<?> c = Class.forName(data.getKey().getClassName());
                    type = (ITextureType) c.getDeclaredField(data.getKey().getObjectName()).get(null);
                } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException | ClassNotFoundException e1) {
                    // nope
                    throw Throwables.propagate(e1);
                }
            } catch (IllegalAccessException | ClassNotFoundException e) {
                throw Throwables.propagate(e);
            }
            for (String name : data.getValue()) {
                if (StringUtils.isNullOrEmpty(name)) {
                    name = data.getKey().getObjectName();
                    name = name.substring(name.lastIndexOf('.') + 1);
                }
                register(name, type);
            }
        }
    }

    public static void register(String name, ITextureType type){
        String key = name.toLowerCase(Locale.ROOT);
        if (map.containsKey(key) && map.get(key) != type){
            throw new IllegalArgumentException("Render Type with name "+key+" has already been registered!");
        }
        else if (map.get(key) != type){
            map.put(key, type);
        }
    }

    public static ITextureType getType(String name){
        String key = name.toLowerCase(Locale.ROOT);
        return map.get(key);
    }

    public static boolean isValid(String name){
        return getType(name) != null;
    }
}
