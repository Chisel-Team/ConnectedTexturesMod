package team.chisel.ctm.client.util;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.message.ParameterizedMessage;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import lombok.SneakyThrows;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import team.chisel.ctm.CTM;
import team.chisel.ctm.client.mixin.ModelBakerImplAccessor;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;
import team.chisel.ctm.client.model.ModelBakedCTM;
import team.chisel.ctm.client.model.ModelCTM;

public enum TextureMetadataHandler {

    INSTANCE;
	
	private final Set<ResourceLocation> registeredTextures = new HashSet<>();
	private final Object2BooleanMap<ResourceLocation> wrappedModels = new Object2BooleanLinkedOpenHashMap<>();
    private final Multimap<ResourceLocation, Material> scrapedTextures = HashMultimap.create();

    public void textureScraped(ResourceLocation modelLocation, Material material) {
        scrapedTextures.put(modelLocation, material);
    }
    
    /*
     * Handle stitching metadata additional textures
     */
//    @SubscribeEvent(priority = EventPriority.LOWEST)
//    public void onTextureStitch(TextureStitchEvent.Pre event) {
//    	Set<ResourceLocation> sprites = new HashSet<>(ObfuscationReflectionHelper.getPrivateValue(TextureStitchEvent.Pre.class, event, "sprites"));
//        for (ResourceLocation rel : sprites) {
//            try {
//                rel = new ResourceLocation(rel.getNamespace(), "textures/" + rel.getPath() + ".png");
//                Optional<IMetadataSectionCTM> metadata = ResourceUtil.getMetadata(rel);
//                var proxy = metadata.map(IMetadataSectionCTM::getProxy);
//                if (proxy.isPresent()) {
//                    ResourceLocation proxysprite = new ResourceLocation(proxy.get());
//                    Optional<IMetadataSectionCTM> proxymeta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(proxysprite));
//                    // Load proxy's base sprite
//                    event.addSprite(proxysprite);
//                    proxymeta.ifPresent(m -> {
//                        // Load proxy's additional textures
//                        for (ResourceLocation r : m.getAdditionalTextures()) {
//                        	if (registeredTextures.add(r)) {
//                        		event.addSprite(r);
//                        	}
//                        }
//                    });
//                }
//                
//                metadata.map(IMetadataSectionCTM::getAdditionalTextures)
//                    .ifPresent(textures -> {
//                    // Load additional textures
//                        for (ResourceLocation r : textures) {
//                            if (registeredTextures.add(r)) {
//                                event.addSprite(r);
//                            }
//                        }
//                    });
//            }
//            catch (FileNotFoundException e) {} // Ignore these, they are reported by vanilla
//            catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
    /*
     * Handle wrapping models that use CTM textures 
     */

//    private static final Class<?> multipartModelClass;
//    private static final Class<?> vanillaModelWrapperClass;
//    private static final Field multipartPartModels;
//    private static final Field modelWrapperModel;
//    static {
//        try {
//            multipartModelClass = Class.forName("net.minecraftforge.client.model.ModelLoader$MultipartModel");
//            multipartPartModels = multipartModelClass.getDeclaredField("partModels");
//            multipartPartModels.setAccessible(true);
//            vanillaModelWrapperClass = Class.forName("net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper");
//            modelWrapperModel = vanillaModelWrapperClass.getDeclaredField("model");
//            modelWrapperModel.setAccessible(true);
//        } catch (ClassNotFoundException | NoSuchFieldException | SecurityException e) {
//            throw Throwables.propagate(e);
//        }
//    }

    @SubscribeEvent(priority = EventPriority.LOWEST) // low priority to capture all event-registered models
    @SneakyThrows
    public void onModelBake(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, UnbakedModel> stateModels = ObfuscationReflectionHelper.getPrivateValue(ModelBakery.class, event.getModelBakery(), "unbakedCache");
        Map<ResourceLocation, BakedModel> models = event.getModels();
        for (Map.Entry<ResourceLocation, BakedModel> entry : models.entrySet()) {
            ResourceLocation rl = entry.getKey();
            UnbakedModel rootModel = stateModels.get(rl);
            if (rootModel != null) {
            	BakedModel baked = entry.getValue();
            	if (baked instanceof AbstractCTMBakedModel) {
            		continue;
            	}
            	if (baked.isCustomRenderer()) { // Nothing we can add to builtin models
            	    continue;
            	}
                Deque<ResourceLocation> dependencies = new ArrayDeque<>();
                Set<ResourceLocation> seenModels = new HashSet<>();
                dependencies.push(rl);
                seenModels.add(rl);
                boolean shouldWrap = wrappedModels.getOrDefault(rl, false);
                // Breadth-first loop through dependencies, exiting as soon as a CTM texture is found, and skipping duplicates/cycles
                while (!shouldWrap && !dependencies.isEmpty()) {
                    ResourceLocation dep = dependencies.pop();
                    UnbakedModel model;
                    try {
                         model = dep == rl ? rootModel : event.getModelBakery().getModel(dep);
                    } catch (Exception e) {
                        continue;
                    }

                    try {
                        Set<Material> textures = new HashSet<>(scrapedTextures.get(dep));
                        for (Material tex : textures) {
                            // Cache all dependent texture metadata
                            try {
                                // At least one texture has CTM metadata, so we should wrap this model
                                if (ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(tex.texture())).isPresent()) { // TODO lazy
                                    shouldWrap = true;
                                    break;
                                }
                            } catch (IOException e) {} // Fallthrough
                        }
                        if (!shouldWrap) {
                            for (ResourceLocation newDep : model.getDependencies()) {
                                if (seenModels.add(newDep)) {
                                    dependencies.push(newDep);
                                }
                            }
                        }
                    } catch (Exception e) {
                        CTM.logger.error(new ParameterizedMessage("Error loading model dependency {} for model {}. Skipping...", dep, rl), e);
                    }
                }
                wrappedModels.put(rl, shouldWrap);
                if (shouldWrap) {
                    try {
                        entry.setValue(wrap(rootModel, baked));
                    } catch (IOException e) {
                        CTM.logger.error("Could not wrap model " + rl + ". Aborting...", e);
                    }
                }
            }
        }
    }

    private @Nonnull BakedModel wrap(UnbakedModel model, BakedModel object) throws IOException {
        ModelCTM modelchisel = new ModelCTM(model);
        return new ModelBakedCTM(modelchisel, object, null); 	
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @SubscribeEvent
    public void onModelBake(ModelEvent.BakingCompleted event) {
        var cache = ObfuscationReflectionHelper.<Map, ModelBakery>getPrivateValue(ModelBakery.class, event.getModelBakery(), "bakedCache");
        var cacheCopy = Map.copyOf(cache);
        cache.clear();
        for (var e : event.getModels().entrySet()) {
            if (e.getValue() instanceof AbstractCTMBakedModel baked && 
                    baked.getModel() instanceof ModelCTM ctmModel && 
                    !ctmModel.isInitialized()) {
                var baker = ModelBakerImplAccessor.createImpl(event.getModelBakery(), ($, m) -> m.sprite(), e.getKey());
                ctmModel.bake(baker, Material::sprite, BlockModelRotation.X0_Y0, e.getKey());
                //Note: We have to clear the cache after baking each model to ensure that we can initialize and capture any textures
                // that might be done by parent models
                cache.clear();
            }
        }
        cache.putAll(cacheCopy);
    }

    public void invalidateCaches() {
        registeredTextures.clear();
        wrappedModels.clear();
        scrapedTextures.clear();
    }
}
