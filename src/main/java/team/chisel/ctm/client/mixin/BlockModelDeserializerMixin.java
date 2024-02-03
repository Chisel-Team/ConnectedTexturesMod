package team.chisel.ctm.client.mixin;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.block.model.BlockModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.chisel.ctm.client.BlockModelExtension;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

@Mixin(BlockModel.Deserializer.class)
public abstract class BlockModelDeserializerMixin implements JsonDeserializer<BlockModel> {

    private static final Gson GSON = new Gson();

    @Inject(at = @At("TAIL"), method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/BlockModel;")
    private void addCTMData(JsonElement pJson, Type pType, JsonDeserializationContext pContext, CallbackInfoReturnable<BlockModel> cir) {
        Map<String, JsonElement> parsed = GSON.fromJson(pJson.getAsJsonObject().getAsJsonObject("ctm_overrides"), new TypeToken<Map<String, JsonElement>>(){}.getType());
        if (parsed == null) {
            parsed = Collections.emptyMap();
        }
        Int2ObjectMap<IMetadataSectionCTM> overrides = new Int2ObjectArrayMap<>(parsed.size());
        for (Map.Entry<String, JsonElement> e : parsed.entrySet()) {
            try {
                int index = Integer.parseInt(e.getKey());
                if (!e.getValue().getAsJsonObject().has("ctm_version")) {
                    // This model can only be version 1, TODO improve this
                    e.getValue().getAsJsonObject().add("ctm_version", new JsonPrimitive(1));
                }
                overrides.put(index, new IMetadataSectionCTM.Serializer().fromJson(e.getValue().getAsJsonObject()));
            } catch (NumberFormatException ex) {}
        }

        ((BlockModelExtension) cir.getReturnValue()).setMetaOverrides(overrides);

    }

}
