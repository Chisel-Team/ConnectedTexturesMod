package team.chisel.ctm.api.util;

import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.gson.JsonObject;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;

/**
 * Bean to hold information that the IBlockRenderType should use to make an IChiselTexture
 */
@ParametersAreNonnullByDefault
public class TextureInfo {

    private TextureAtlasSprite[] sprites;

    private Optional<JsonObject> info;

    private BlockRenderLayer renderLayer;

    public TextureInfo(TextureAtlasSprite[] sprites, Optional<JsonObject> info, BlockRenderLayer layer){
        this.sprites = sprites;
        this.info = info;
        this.renderLayer = layer;
    }

    /**
     * Gets the sprites to use for this texture
     */
    public TextureAtlasSprite[] getSprites() {
        return this.sprites;
    }

    /**
     * Gets a JsonObject that had the key "info" for extra texture information
     * This JsonObject might not exist
     */
    public Optional<JsonObject> getInfo()
    {
        return this.info;
    }

    /**
     * Returns the render layer for this texture
     */
    public BlockRenderLayer getRenderLayer(){
        return this.renderLayer;
    }

    /**
     * Get whether this block should be rendered in fullbright
     */
    @Deprecated
    public boolean getFullbright() {
        return false;
    }
}
