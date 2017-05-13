package team.chisel.ctm.api.model;

import javax.annotation.Nonnull;

import com.google.gson.JsonObject;

import net.minecraft.util.ResourceLocation;

public interface IModelParser {

    @Nonnull
    IModelCTM fromJson(ResourceLocation res, JsonObject json);
}
