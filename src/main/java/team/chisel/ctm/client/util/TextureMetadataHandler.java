package team.chisel.ctm.client.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.block.model.MultiVariant;
import org.apache.logging.log4j.message.ParameterizedMessage;

import com.google.common.collect.Sets;

import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ForgeModelBakery;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import team.chisel.ctm.CTM;
import team.chisel.ctm.client.BlockModelExtension;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;
import team.chisel.ctm.client.model.ModelBakedCTM;
import team.chisel.ctm.client.model.ModelCTM;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;

public enum TextureMetadataHandler {

    INSTANCE;
	
	private final Set<ResourceLocation> registeredTextures = new HashSet<>();
	private final Object2BooleanMap<ResourceLocation> wrappedModels = new Object2BooleanLinkedOpenHashMap<>();
    
    /*
     * Handle stitching metadata additional textures
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTextureStitch(TextureStitchEvent.Pre event) {
    	Set<ResourceLocation> sprites = new HashSet<>(ObfuscationReflectionHelper.getPrivateValue(TextureStitchEvent.Pre.class, event, "sprites"));
        for (ResourceLocation rel : sprites) {
            try {
                rel = new ResourceLocation(rel.getNamespace(), "textures/" + rel.getPath() + ".png");
                IMetadataSectionCTM metadata = ResourceUtil.getMetadata(rel);
                if (metadata != null) {
                    // Load proxy data
                    if (metadata.getProxy() != null) {
                        ResourceLocation proxysprite = new ResourceLocation(metadata.getProxy());
                        IMetadataSectionCTM proxymeta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(proxysprite));
                        // Load proxy's base sprite
                        event.addSprite(proxysprite);
                        if (proxymeta != null) {
                            // Load proxy's additional textures
                            for (String r : proxymeta.getAdditionalTextures()) {
                            	if (registeredTextures.add(new ResourceLocation(r))) {
                            		event.addSprite(new ResourceLocation(r));
                            	}
                            }
                        }
                    }
                    // Load additional textures
                    for (String r : metadata.getAdditionalTextures()) {
                        if (registeredTextures.add(new ResourceLocation(r))) {
                            event.addSprite(new ResourceLocation(r));
                        }
                    }
                }
            }
            catch (FileNotFoundException e) {} // Ignore these, they are reported by vanilla
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
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

    @SuppressWarnings("unchecked")
    @SubscribeEvent(priority = EventPriority.LOWEST) // low priority to capture all event-registered models
    @SneakyThrows
    public void onModelBake(ModelBakeEvent event) {
        Map<ResourceLocation, UnbakedModel> stateModels = ObfuscationReflectionHelper.getPrivateValue(ModelBakery.class, event.getModelLoader(), "f_119212_");
        for (ResourceLocation rl : event.getModelRegistry().keySet()) {
            UnbakedModel rootModel = stateModels.get(rl);
            if (rootModel != null) {
            	BakedModel baked = event.getModelRegistry().get(rl);
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
                         model = dep == rl ? rootModel : event.getModelLoader().getModel(dep);
                    } catch (Exception e) {
                        continue;
                    }

                    if (model instanceof BlockModelExtension extension) {
                        shouldWrap = !extension.getMetaOverrides().isEmpty();
                    }

                    try {
                        Set<Material> textures = Sets.newHashSet(model.getMaterials(event.getModelLoader()::getModel, Sets.newHashSet()));
                    // FORGE WHY
//                    if (vanillaModelWrapperClass.isAssignableFrom(model.getClass())) {
//                        BlockModel parent = ((BlockModel) modelWrapperModel.get(model)).parent;
//                        while (parent != null) {
//                            textures.addAll(parent.textures.values().stream().filter(e -> e.left().isPresent()).map(e -> e.left().orElseThrow(IllegalStateException::new)).collect(Collectors.toSet()));
//                            parent = parent.parent;
//                        }
//                    }
                    
                        Set<ResourceLocation> newDependencies = Sets.newHashSet(model.getDependencies());
                        
                    // FORGE WHYYYYY
//                    if (multipartModelClass.isAssignableFrom(model.getClass())) {
//                        Map<?, IUnbakedModel> partModels = (Map<?, IUnbakedModel>) multipartPartModels.get(model);
//                        textures = partModels.values().stream().map(m -> m.getTextures(event.getModelLoader()::getUnbakedModel, Sets.newHashSet())).flatMap(Collection::stream).collect(Collectors.toSet());
//                        newDependencies.addAll(partModels.values().stream().flatMap(m -> m.getDependencies().stream()).collect(Collectors.toList()));
//                    }

                        for (Material tex : textures) {
                            IMetadataSectionCTM meta = null;
                            // Cache all dependent texture metadata
                            try {
                                meta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(tex.texture()));
                            } catch (IOException e) {} // Fallthrough
                            if (meta != null) {
                                // At least one texture has CTM metadata, so we should wrap this model
                                shouldWrap = true;
                            }
                        }
                        
                        for (ResourceLocation newDep : newDependencies) {
                            if (seenModels.add(newDep)) {
                                dependencies.push(newDep);
                            }
                        }
                    } catch (Exception e) {
                        CTM.logger.error(new ParameterizedMessage("Error loading model dependency {} for model {}. Skipping...", dep, rl), e);
                    }
                }
                wrappedModels.put(rl, shouldWrap);
                if (shouldWrap) {
                    try {
                        event.getModelRegistry().put(rl, wrap(rl, rootModel, baked, event.getModelLoader()));
                        dependencies.clear();
                    } catch (IOException e) {
                        CTM.logger.error("Could not wrap model " + rl + ". Aborting...", e);
                    }
                }
            }
        }
    }

    private @Nonnull BakedModel wrap(ResourceLocation loc, UnbakedModel model, BakedModel object, ForgeModelBakery loader) throws IOException {
        ModelCTM modelchisel = new ModelCTM(model);
        modelchisel.initializeTextures(loader, m -> Minecraft.getInstance().getTextureAtlas(m.atlasLocation()).apply(m.texture()));
        return new ModelBakedCTM(modelchisel, object); 	
    }

    public void invalidateCaches() {
        registeredTextures.clear();
        wrappedModels.clear();
    }
}
