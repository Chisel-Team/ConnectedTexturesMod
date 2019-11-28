package team.chisel.ctm.client.texture;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

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
import team.chisel.ctm.client.texture.type.TextureTypes;
import team.chisel.ctm.client.util.ResourceUtil;

@ParametersAreNonnullByDefault
public interface IMetadataSectionCTM extends IMetadataSection {
    
    public static final String SECTION_NAME = "ctm";
    
    int getVersion();
    
    ITextureType getType();
    
    BlockRenderLayer getLayer();
    
    ResourceLocation[] getAdditionalTextures();
    
    @Nullable String getProxy();

    JsonObject getExtraData();
    
    default ICTMTexture<?> makeTexture(TextureAtlasSprite sprite, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        IMetadataSectionCTM meta = this;
        if (getProxy() != null) {
            TextureAtlasSprite proxySprite = bakedTextureGetter.apply(new ResourceLocation(getProxy()));
            try {
                meta = ResourceUtil.getMetadata(proxySprite);
                if (meta == null) {
                    meta = new V1();
                }
                sprite = proxySprite;
            } catch (IOException e) {
                CTM.logger.error("Could not parse metadata of proxy, ignoring proxy and using base texture." + getProxy(), e);
                meta = this;
            }
        }
        return meta.getType().makeTexture(new TextureInfo(
                Arrays.stream(ObjectArrays.concat(new ResourceLocation(sprite.getIconName()), meta.getAdditionalTextures())).map(bakedTextureGetter::apply).toArray(TextureAtlasSprite[]::new), 
                Optional.of(meta.getExtraData()), 
                meta.getLayer()
        ));
    }
    
    @ToString
    @Getter
    public static class V1 implements IMetadataSectionCTM {
        
        private ITextureType type = TextureTypes.getType("NORMAL").orElseThrow(() ->
                new IllegalStateException("Missing normal texture type"));
        private BlockRenderLayer layer = null;
        private String proxy;
        private ResourceLocation[] additionalTextures = new ResourceLocation[0];
        private JsonObject extraData = new JsonObject();

        @Override
        public int getVersion() {
            return 1;
        }

        public static IMetadataSectionCTM fromJson(JsonObject obj) throws JsonParseException {
            V1 ret = new V1();
            
            if (obj.has("proxy")) {
                JsonElement proxyEle = obj.get("proxy");
                if (proxyEle.isJsonPrimitive() && proxyEle.getAsJsonPrimitive().isString()) {
                    ret.proxy = proxyEle.getAsString();
                }
                
                if (obj.entrySet().stream().filter(e -> e.getKey().equals("ctm_version")).count() > 1) {
                    throw new JsonParseException("Cannot define other fields when using proxy");
                }
            }

            if (obj.has("type")) {
                JsonElement typeEle = obj.get("type");
                if (typeEle.isJsonPrimitive() && typeEle.getAsJsonPrimitive().isString()) {
                    ret.type = TextureTypes.getType(typeEle.getAsString()).orElseThrow(() ->
                            new JsonParseException("Invalid texture type: " + typeEle));
                }
            }

            if (obj.has("layer")) {
                JsonElement layerEle = obj.get("layer");
                if (layerEle.isJsonPrimitive() && layerEle.getAsJsonPrimitive().isString()) {
                    try {
                        ret.layer = BlockRenderLayer.valueOf(layerEle.getAsString());
                    } catch (IllegalArgumentException e) {
                        throw new JsonParseException("Invalid block layer given: " + layerEle);
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
    
    public static class Serializer implements IMetadataSectionSerializer<IMetadataSectionCTM> {

        @Override
        public @Nullable IMetadataSectionCTM deserialize(@Nullable JsonElement json, @Nullable Type typeOfT, @Nullable JsonDeserializationContext context) throws JsonParseException {
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
                } else {
                    throw new JsonParseException("Found ctm section without ctm_version");
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
