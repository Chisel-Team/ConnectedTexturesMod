package team.chisel.ctm.api.model;

import javax.annotation.Nonnull;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

public interface IModelParser {

    @Nonnull
    IModelCTM fromJson(JsonDeserializationContext ctx, JsonObject json);
}
