package team.chisel.ctm.api.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public interface IModelParser {

    @NotNull
    IModelCTM fromJson(JsonDeserializationContext ctx, JsonObject json);
}
