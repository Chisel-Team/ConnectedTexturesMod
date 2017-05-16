package team.chisel.ctm.client.model.parsing;

import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.SneakyThrows;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.model.IModelParser;
import team.chisel.ctm.client.model.ModelCTM;

public class ModelParserV1 implements IModelParser {
    
    private static final Deque<ResourceLocation> _loadingModels = ReflectionHelper.getPrivateValue(ModelLoaderRegistry.class, null, "loadingModels");
    private static final Gson GSON = new Gson();
    
    @Override
    @Nonnull
    @SneakyThrows
    public IModelCTM fromJson(ResourceLocation res, JsonObject json) {
        ModelBlock modelinfo = ModelBlock.deserialize(json.toString());
        
        // Hack around circularity detection to load vanilla model
        ResourceLocation prev = _loadingModels.removeLast();
        IModel vanillamodel = ModelLoaderRegistry.getModel(new ResourceLocation(res.getResourceDomain(), res.getResourcePath().replace("models/", "")));
        _loadingModels.addLast(prev);

        Map<String, JsonElement> parsed = GSON.fromJson(json.getAsJsonObject("ctm_overrides"), new TypeToken<Map<String, JsonElement>>(){}.getType());
        if (parsed == null) {
            parsed = Collections.emptyMap();
        }
        Int2ObjectMap<JsonElement> replacements = new Int2ObjectArrayMap<>(parsed.size());
        for (Entry<String, JsonElement> e : parsed.entrySet()) {
            try {
                int index = Integer.parseInt(e.getKey());
                replacements.put(index, e.getValue());
            } catch (NumberFormatException ex) {}
        }
        return new ModelCTM(modelinfo, vanillamodel, replacements);
    }
}
