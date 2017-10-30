package team.chisel.ctm.client.texture.render;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.jcraft.jogg.Packet;
import lombok.Getter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.util.NonnullType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.shader.ShaderCompileException;
import team.chisel.ctm.client.shader.ShaderUtil;
import team.chisel.ctm.client.util.Quad;


/**
 * Abstract implementation of IChiselTexture
 */
@ParametersAreNonnullByDefault
@SuppressWarnings("deprecation")
public abstract class AbstractTexture<T extends ITextureType> implements ICTMTexture<T> {

    @Getter
    protected T type;
    @Getter
    protected BlockRenderLayer layer;

    @SuppressWarnings("null")
    protected @NonnullType TextureAtlasSprite @NonnullType[] sprites;

    @Deprecated
    protected boolean fullbright;

    protected boolean hasLight;
    protected int skylight, blocklight;

    protected boolean hasShaders;
    protected String vertShader;
    protected String fragShader;
    protected int shaderProgram;

    @Deprecated
    public AbstractTexture(T type, BlockRenderLayer layer, TextureAtlasSprite... sprites) {
        this.type = type;
        this.layer = layer;
        this.sprites = sprites;
        this.skylight = this.blocklight = 0;
    }

    public AbstractTexture(T type, TextureInfo info) {
        this.type = type;
        this.layer = info.getRenderLayer();
        this.sprites = info.getSprites();
        this.fullbright = info.getFullbright();
        if (info.getInfo().isPresent()) {
            JsonElement light = info.getInfo().get().get("light");
            if (light != null) {
                if (light.isJsonPrimitive()) {
                    this.hasLight = true;
                    this.skylight = this.blocklight = parseLightValue(light);
                } else if (light.isJsonObject()) {
                    this.hasLight = true;
                    JsonObject lightObj = light.getAsJsonObject();
                    this.blocklight = parseLightValue(lightObj.get("block"));
                    this.skylight = parseLightValue(lightObj.get("sky"));
                }
            }
            JsonElement shaders = info.getInfo().get().get("shaders");
            if (shaders != null){
                if (shaders.isJsonObject()){
                    JsonObject shaderObj = shaders.getAsJsonObject();
                    JsonElement vert = shaderObj.get("vertex");
                    JsonElement fragment = shaderObj.get("fragment");
                    if (vert != null || fragment != null) {
                        if (vert != null) {
                            this.vertShader = vert.getAsString();
                        }
                        if (fragment != null) {
                            this.fragShader = fragment.getAsString();
                        }
                        try {
                            this.shaderProgram = ShaderUtil.createProgram(this.fragShader, this.vertShader);
                            this.hasShaders = true;
                        } catch (ShaderCompileException exception) {
                            exception.printStackTrace();
                            this.hasShaders = false;
                        }
                        //todo add fallback, lets get shaders working first
                    }
                }
            }
        }
    }
    
    private final int parseLightValue(@Nullable JsonElement data) {
        if (data != null && data.isJsonPrimitive() && data.getAsJsonPrimitive().isNumber()) {
            return MathHelper.clamp(data.getAsInt(), 0, 15);
        }
        return 0;
    }

    @Override
    public TextureAtlasSprite getParticle() {
        return sprites[0];
    }
    
    @Override
    public Collection<ResourceLocation> getTextures() {
        return Arrays.stream(sprites).map(s -> new ResourceLocation(s.getIconName())).collect(Collectors.toList());
    }

    protected Quad makeQuad(BakedQuad bq, @Nullable ITextureContext context) {
        Quad q = Quad.from(bq);
        if (context != null) {
            if (hasLight) {
                q = q.setLight(blocklight, skylight);
            } else {
                q = q.setFullbright(fullbright);
            }
        }
        return q;
    }
}
