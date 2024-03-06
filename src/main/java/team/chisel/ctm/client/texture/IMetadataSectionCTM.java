package team.chisel.ctm.client.texture;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ObjectArrays;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import lombok.Getter;
import lombok.ToString;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.chisel.ctm.CTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.type.TextureTypeRegistry;
import team.chisel.ctm.client.util.BlockRenderLayer;
import team.chisel.ctm.client.util.ResourceUtil;

@ParametersAreNonnullByDefault
public interface IMetadataSectionCTM {
    
    public static final String SECTION_NAME = "ctm";
    
    int getVersion();
    
    ITextureType getType();
    
    BlockRenderLayer getLayer();
    
    ResourceLocation[] getAdditionalTextures();
    
    @Nullable String getProxy();

    JsonObject getExtraData();
    
    default ICTMTexture<?> makeTexture(TextureAtlasSprite sprite, Function<Material, TextureAtlasSprite> bakedTextureGetter) {
        IMetadataSectionCTM meta = this;
        boolean hasProxy = getProxy() != null;
        if (hasProxy) {
            TextureAtlasSprite proxySprite = bakedTextureGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation(getProxy())));
            try {
                meta = ResourceUtil.getMetadata(proxySprite).orElse(new V1());
                sprite = proxySprite;
            } catch (IOException e) {
                CTM.logger.error("Could not parse metadata of proxy, ignoring proxy and using base texture." + getProxy(), e);
                meta = this;
                hasProxy = false;
            }
        }
        return meta.getType().makeTexture(new TextureInfo(
                Arrays.stream(ObjectArrays.concat(sprite.contents().name(), meta.getAdditionalTextures()))
                        .map(rl -> new Material(TextureAtlas.LOCATION_BLOCKS, rl))
                        .map(bakedTextureGetter)
                        .toArray(TextureAtlasSprite[]::new),
                Optional.of(meta.getExtraData()), 
                meta.getLayer(),
                hasProxy
        ));
    }
    
    @ToString
    @Getter
    class V1 implements IMetadataSectionCTM {
        
        private ITextureType type = TextureTypeRegistry.getType("NORMAL");
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
                    ITextureType type = TextureTypeRegistry.getType(typeEle.getAsString());
                    if (type == null) {
                        throw new JsonParseException("Invalid render type given: " + typeEle);
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
    
    class Serializer implements MetadataSectionSerializer<IMetadataSectionCTM> {

        @Override
        public @Nullable IMetadataSectionCTM fromJson(@Nullable JsonObject json) throws JsonParseException {
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
        public @NotNull String getMetadataSectionName() {
            return SECTION_NAME;
        }
    }

}
