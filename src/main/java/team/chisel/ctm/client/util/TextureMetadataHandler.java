package team.chisel.ctm.client.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.TransformationMatrix;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.Material;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.SimpleModelTransform;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import team.chisel.ctm.CTM;
import team.chisel.ctm.api.model.IModelCTM;
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
	/* TODO 1.15
    @SubscribeEvent
    public void onTextureStitch(TextureCollectedEvent event) {
        if (Minecraft.getInstance().getTextureMap() != null) {
            TextureAtlasSprite sprite = event.getSprite();
            try {
                ResourceLocation rel = sprite.getName();
                rel = new ResourceLocation(rel.getNamespace(), "textures/" + rel.getPath() + ".png");
                IMetadataSectionCTM metadata = ResourceUtil.getMetadata(rel);
                if (metadata != null) {
                	IResourceManager rm = Minecraft.getInstance().getResourceManager();
                    // Load proxy data
                    if (metadata.getProxy() != null) {
                        ResourceLocation proxysprite = new ResourceLocation(metadata.getProxy());
                        IMetadataSectionCTM proxymeta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(proxysprite));
                        // Load proxy's base sprite
                        event.getMap().registerSprite(rm, proxysprite);
                        if (proxymeta != null) {
                            // Load proxy's additional textures
                            for (ResourceLocation r : proxymeta.getAdditionalTextures()) {
                            	if (registeredTextures.add(r)) {
                            		event.getMap().registerSprite(rm, r);
                            	}
                            }
                        }
                    }
                    // Load additional textures
                    for (ResourceLocation r : metadata.getAdditionalTextures()) {
                        if (registeredTextures.add(r)) {
                            event.getMap().registerSprite(rm, r);
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
    */
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
        Map<ResourceLocation, IUnbakedModel> stateModels = ObfuscationReflectionHelper.getPrivateValue(ModelBakery.class, event.getModelLoader(), "field_217849_F");
        for (ResourceLocation rl : event.getModelRegistry().keySet()) {
            IUnbakedModel rootModel = stateModels.get(rl);
            if (rootModel != null && !(rootModel instanceof IModelCTM)) {
                Deque<ResourceLocation> dependencies = new ArrayDeque<>();
                Set<ResourceLocation> seenModels = new HashSet<>();
                dependencies.push(rl);
                seenModels.add(rl);
                boolean shouldWrap = wrappedModels.getOrDefault(rl, false);
                // Breadth-first loop through dependencies, exiting as soon as a CTM texture is found, and skipping duplicates/cycles
                while (!shouldWrap && !dependencies.isEmpty()) {
                    ResourceLocation dep = dependencies.pop();
                    IUnbakedModel model;
                    try {
                         model = dep == rl ? rootModel : event.getModelLoader().getUnbakedModel(dep);
                    } catch (Exception e) {
                        continue;
                    }

                    Set<Material> textures = Sets.newHashSet(model.getTextures(event.getModelLoader()::getUnbakedModel, Sets.newHashSet()));
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
                        try {
                            meta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(tex.getTextureLocation()));
                        } catch (IOException e) {} // Fallthrough
                        if (meta != null) {
                            shouldWrap = true;
                            break;
                        }
                    }
                    
                    for (ResourceLocation newDep : newDependencies) {
                        if (seenModels.add(newDep)) {
                            dependencies.push(newDep);
                        }
                    }
                }
                wrappedModels.put(rl, shouldWrap);
                if (shouldWrap) {
                    try {
                        event.getModelRegistry().put(rl, wrap(rl, rootModel, event.getModelRegistry().get(rl), event.getModelLoader()));
                        dependencies.clear();
                    } catch (IOException e) {
                        CTM.logger.error("Could not wrap model " + rl + ". Aborting...", e);
                    }
                }
            }
        }
    }

    private @Nonnull IBakedModel wrap(ResourceLocation loc, IUnbakedModel model, IBakedModel object, ModelLoader loader) throws IOException {
        ModelCTM modelchisel = new ModelCTM(model);
        modelchisel.bake(loader, m -> Minecraft.getInstance().getAtlasSpriteGetter(m.getAtlasLocation()).apply(m.getTextureLocation()), new SimpleModelTransform(TransformationMatrix.identity()), loc);
        return new ModelBakedCTM(modelchisel, object); 	
    }

    public void invalidateCaches() {
        registeredTextures.clear();
        wrappedModels.clear();
    }
}
