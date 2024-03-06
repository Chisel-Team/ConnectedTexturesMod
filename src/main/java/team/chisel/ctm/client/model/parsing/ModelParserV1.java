package team.chisel.ctm.client.model.parsing;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.SneakyThrows;
import net.minecraft.client.renderer.block.model.BlockModel;
import org.jetbrains.annotations.NotNull;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.model.IModelParser;
import team.chisel.ctm.client.model.ModelCTM;

@SuppressWarnings("unchecked")
public class ModelParserV1 implements IModelParser {
    
    private static final Gson GSON = new Gson();
    
    @Override
    @NotNull
    @SneakyThrows
    public IModelCTM fromJson(JsonDeserializationContext ctx, JsonObject json) {
    	BlockModel modelinfo = ctx.deserialize(json, BlockModel.class);

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
        return new ModelCTM(modelinfo, replacements);
    }
}
