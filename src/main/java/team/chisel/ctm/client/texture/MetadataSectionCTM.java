package team.chisel.ctm.client.texture;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Function;
import com.google.common.collect.ObjectArrays;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import lombok.Getter;
import lombok.ToString;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSectionSerializer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import team.chisel.ctm.CTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.type.TextureTypeRegistry;

@ParametersAreNonnullByDefault
public abstract class MetadataSectionCTM implements IMetadataSection {
    
    public static final String SECTION_NAME = "ctm";
    
    public abstract int getVersion();
    
    public abstract ITextureType getType();
    
    public abstract BlockRenderLayer getLayer();
    
    public abstract ResourceLocation[] getAdditionalTextures();
    
    public abstract JsonObject getExtraData();
    
    @SuppressWarnings("null")
    public ICTMTexture<?> makeTexture(TextureAtlasSprite sprite, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        return getType().makeTexture(new TextureInfo(
                Arrays.stream(ObjectArrays.concat(new ResourceLocation(sprite.getIconName()), getAdditionalTextures())).map(bakedTextureGetter::apply).toArray(TextureAtlasSprite[]::new), 
                Optional.of(getExtraData()), 
                getLayer()
        ));
    }
    
    @ToString
    @Getter
    public static class V1 extends MetadataSectionCTM {
        
        @SuppressWarnings("null")
        private ITextureType type = TextureTypeRegistry.getType("NORMAL");
        private BlockRenderLayer layer = BlockRenderLayer.SOLID;
        private ResourceLocation[] additionalTextures = new ResourceLocation[0];
        private JsonObject extraData = new JsonObject();

        @Override
        public int getVersion() {
            return 1;
        }

        public static MetadataSectionCTM fromJson(JsonObject obj) {
            V1 ret = new V1();

            if (obj.has("type")) {
                JsonElement typeEle = obj.get("type");
                if (typeEle.isJsonPrimitive() && typeEle.getAsJsonPrimitive().isString()) {
                    ITextureType type = TextureTypeRegistry.getType(typeEle.getAsString());
                    if (type == null) {
                        CTM.logger.error("Invalid render type given: {}", typeEle);
                    } else {
                        ret.type = type;
                    }
                }
            }

            if (obj.has("layer")) {
                JsonElement layerEle = obj.get("layer");
                if (layerEle.isJsonPrimitive() && layerEle.getAsJsonPrimitive().isString()) {
                    try {
                        ret.layer = BlockRenderLayer.valueOf(layerEle.getAsString());
                    } catch (IllegalArgumentException e) {
                        CTM.logger.error("Invalid block layer given: {}", layerEle);
                    }
                }
            }

            if (obj.has("textures")) {
                JsonElement texturesEle = obj.get("textures");
                if (texturesEle.isJsonArray()) {
                    JsonArray texturesArr = texturesEle.getAsJsonArray();
                    ret.additionalTextures = new ResourceLocation[texturesArr.size()];
                    for (int i = 0; i < texturesArr.size(); i++) {
                        JsonElement e = texturesArr.get(i);
                        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                            ret.additionalTextures[i] = new ResourceLocation(e.getAsString());
                        }
                    }
                }
            }
            
            if (obj.has("extra") && obj.get("extra").isJsonObject()) {
                ret.extraData = obj.getAsJsonObject("extra");
            }
            return ret;
        }
    }
    
    public static class Serializer implements IMetadataSectionSerializer<MetadataSectionCTM> {

        @Override
        public @Nullable MetadataSectionCTM deserialize(@Nullable JsonElement json, @Nullable Type typeOfT, @Nullable JsonDeserializationContext context) throws JsonParseException {
            if (json != null && json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                if (obj.has("ctm_version")) {
                    JsonElement version = obj.get("ctm_version");
                    if (version.isJsonPrimitive() && version.getAsJsonPrimitive().isNumber()) {
                        switch (version.getAsInt()) {
                        case 1:
                            return V1.fromJson(obj);
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public @Nonnull String getSectionName() {
            return SECTION_NAME;
        }
    }

}
