package team.chisel.ctm.client.texture.type;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.texture.TextureType;

/**
 * Registry for all the different texture types
 */
@Log4j2
@UtilityClass
public class TextureTypeRegistry {

    private static Map<String, ITextureType> map = Maps.newHashMap();
    public static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @SuppressWarnings("unchecked")
    public static void scan() {
        try {
            lock.writeLock().lock();
            final List<ModFileScanData.AnnotationData> annotations = ModList.get().getAllScanData().stream()
                    .map(ModFileScanData::getAnnotations)
                    .flatMap(Collection::stream)
                    .filter(a -> TextureType.class.getName().equals(a.annotationType().getClassName()))
                    .toList();
            log.debug("Found {} @TextureType annotations from scan", annotations.size());
    
            Multimap<ModFileScanData.AnnotationData, String> annots = HashMultimap.create();        
            for (ModFileScanData.AnnotationData single : annotations) {
            	annots.put(single, (String) single.annotationData().get("value"));
            }
            log.debug("Found {} unique texture types", annots.size());
            
            for (Entry<ModFileScanData.AnnotationData, Collection<String>> data : annots.asMap().entrySet()) {
                ITextureType type;
                if (data.getKey().targetType() == ElementType.FIELD) {
                    try {
                        Class<?> c = Class.forName(data.getKey().clazz().getClassName());
                        Field f = c.getDeclaredField(data.getKey().memberName());
                        type = (ITextureType) f.get(null);
                    } catch (Exception e) {
                        throw new RuntimeException("Exception loading texture type for class: " + data.getKey().clazz(), e);
                    }
                } else if (data.getKey().targetType() == ElementType.TYPE) {
    	            try {
    	                Class<? extends ITextureType> clazz = (Class<? extends ITextureType>) Class.forName(data.getKey().clazz().getClassName());
    	                type = clazz.newInstance();
    	            } catch (Exception e) {
    	                throw new RuntimeException("Exception loading texture type for class: " + data.getKey().clazz() + " (on member " + data.getKey().memberName() + ")", e);
    	            }
                } else {
                	throw new IllegalArgumentException("@TextureType found on invalid element type: " + data.getKey().targetType() + " (" + data.getKey().clazz() + ")");
                }
                for (String name : data.getValue()) {
                    log.debug("Registering scanned texture type: {}", name);
                    register(name, type);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void register(String name, ITextureType type) {
        try {
            lock.writeLock().lock();
            String key = name.toLowerCase(Locale.ROOT);
            if (map.containsKey(key) && map.get(key) != type){
                throw new IllegalArgumentException("Render Type with name "+key+" has already been registered!");
            }
            else if (map.get(key) != type){
                map.put(key, type);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public static ITextureType remove(String name) {
        try {
            lock.writeLock().lock();
            String key = name.toLowerCase(Locale.ROOT);
            return map.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static ITextureType getType(String name) {
        try {
            lock.readLock().lock();
            String key = name.toLowerCase(Locale.ROOT);
            return map.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public static boolean isValid(String name){
        return getType(name) != null;
    }
}
