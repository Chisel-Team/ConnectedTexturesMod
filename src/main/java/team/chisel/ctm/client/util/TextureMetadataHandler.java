package team.chisel.ctm.client.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.ProgressManager.ProgressBar;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.client.model.ModelBakedCTM;
import team.chisel.ctm.client.model.ModelCTM;
import team.chisel.ctm.client.model.parsing.ModelLoaderCTM;
import team.chisel.ctm.client.texture.MetadataSectionCTM;

public enum TextureMetadataHandler {

    INSTANCE;
    
    /*
     * Handle stitching metadata additional textures
     */
    
    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        if (Minecraft.getMinecraft().getTextureMapBlocks() != null) {
            Map<String, TextureAtlasSprite> mapRegisteredSprites = ReflectionHelper.getPrivateValue(TextureMap.class, Minecraft.getMinecraft().getTextureMapBlocks(), "mapRegisteredSprites");
            ProgressBar prog = ProgressManager.push("Loading Chisel metadata", mapRegisteredSprites.size());
            for (String res : ImmutableMap.copyOf(mapRegisteredSprites).keySet()) {
                try {
                    ResourceLocation rel = new ResourceLocation(res);
                    prog.step(rel.toString());
                    rel = new ResourceLocation(rel.getResourceDomain(), "textures/" + rel.getResourcePath() + ".png");
                    MetadataSectionCTM metadata = ResourceUtil.getMetadata(rel);
                    if (metadata != null) {
                        for (ResourceLocation r : metadata.getAdditionalTextures()) {
                            event.getMap().registerSprite(r);
                        }
                    }
                }
                catch (FileNotFoundException e) {} // Ignore these, they are reported by vanilla
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            ProgressManager.pop(prog);
        }
    }
    
    /*
     * Handle wrapping models that use CTM textures 
     */

    private static final Class<?> multipartModelClass;
    static {
        try {
            multipartModelClass = Class.forName("net.minecraftforge.client.model.ModelLoader$MultipartModel");
        } catch (ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent(priority = EventPriority.LOWEST) // low priority to capture all event-registered models
    @SneakyThrows
    public void onModelBake(ModelBakeEvent event) {
        Map<ModelResourceLocation, IModel> stateModels = ReflectionHelper.getPrivateValue(ModelLoader.class, event.getModelLoader(), "stateModels");
        for (ModelResourceLocation mrl : event.getModelRegistry().getKeys()) {
            IModel model = stateModels.get(mrl);
            if (model != null && !(model instanceof IModelCTM) && Collections.disjoint(model.getDependencies(), ModelLoaderCTM.parsedLocations)) {
                Collection<ResourceLocation> textures = model.getTextures();
                // FORGE WHY
                if (multipartModelClass.isAssignableFrom(model.getClass())) {
                    Field _partModels = multipartModelClass.getDeclaredField("partModels");
                    _partModels.setAccessible(true);
                    Map<?, IModel> partModels = (Map<?, IModel>) _partModels.get(model);
                    textures = partModels.values().stream().map(m -> m.getTextures()).flatMap(Collection::stream).collect(Collectors.toList());
                }
                for (ResourceLocation tex : textures) {
                    MetadataSectionCTM meta = null;
                    try {
                        meta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(tex));
                    } catch (IOException e) {} // Fallthrough
                    if (meta != null) {
                        event.getModelRegistry().putObject(mrl, wrap(model, event.getModelRegistry().getObject(mrl)));
                        break;
                    }
                }
            }
        }
    }

    private @Nonnull IBakedModel wrap(IModel model, IBakedModel object) {
        ModelCTM modelchisel = new ModelCTM(null, model, Int2ObjectMaps.emptyMap());
        modelchisel.bake(TRSRTransformation.identity(), DefaultVertexFormats.ITEM, rl -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(rl.toString()));
        return new ModelBakedCTM(modelchisel, object);
    }
}
