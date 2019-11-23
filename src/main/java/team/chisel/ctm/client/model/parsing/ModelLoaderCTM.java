package team.chisel.ctm.client.model.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.model.IModelParser;

public enum ModelLoaderCTM implements ICustomModelLoader {
    
    INSTANCE;
        
    private static final Map<Integer, IModelParser> parserVersions = ImmutableMap.of(1, new ModelParserV1());
    
    private IResourceManager manager;
    private Map<ResourceLocation, IModelCTM> loadedModels = Maps.newHashMap();

    private LoadingCache<ResourceLocation, JsonElement> jsonCache = CacheBuilder.newBuilder().maximumSize(128).build(
            new CacheLoader<ResourceLocation, JsonElement>() {
                @Override
                @SuppressWarnings("null")
                public JsonElement load(ResourceLocation modelLocation) throws Exception {
                    String path = modelLocation.getResourcePath() + ".json";
                    if (!path.startsWith("models/")) {
                        path = "models/" + path;
                    }
                    ResourceLocation absolute = new ResourceLocation(modelLocation.getResourceDomain(), path);

                    try (IResource resource = manager.getResource(absolute);
                         InputStream resourceInputStream = resource.getInputStream();
                         InputStreamReader resourceInputStreamReader = new InputStreamReader(resourceInputStream)) {
                        JsonElement ele = new JsonParser().parse(resourceInputStreamReader);
                        if (ele != null) {
                            return ele;
                        }
                    } catch (Exception e) {}

                    return JsonNull.INSTANCE;
                }
            }
    );

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterModelBaking(ModelBakeEvent event) {
        jsonCache.invalidateAll();
    }

    @Override
    public void onResourceManagerReload(@Nonnull IResourceManager resourceManager) {
        this.manager = resourceManager;
        jsonCache.invalidateAll();
        loadedModels.clear();
    }
    
    @Override
    public boolean accepts(ResourceLocation modelLocation) {        
        if (modelLocation instanceof ModelResourceLocation) {
            modelLocation = new ResourceLocation(modelLocation.getResourceDomain(), modelLocation.getResourcePath());
        }

        JsonElement json = jsonCache.getUnchecked(modelLocation);
        return json.isJsonObject() && json.getAsJsonObject().has("ctm_version");
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) throws IOException {
        loadedModels.computeIfAbsent(modelLocation, res -> loadFromFile(res, true));
        IModelCTM model = loadedModels.get(modelLocation);
        if (model != null) {
            model.load();
        }
        return model;
    }

    public static final Set<ResourceLocation> parsedLocations = new HashSet<>();

    private IModelCTM loadFromFile(ResourceLocation res, boolean forLoad) {
        if (forLoad) {
            parsedLocations.add(new ResourceLocation(res.getResourceDomain(), res.getResourcePath().replace("models/", "")));
        }

        JsonObject json = jsonCache.getUnchecked(res).getAsJsonObject();

        IModelParser parser = parserVersions.get(json.get("ctm_version").getAsInt());
        if (parser == null) {
            throw new IllegalArgumentException("Invalid \"ctm_version\" in model " + res);
        }
        return parser.fromJson(res, json);
    }
}
