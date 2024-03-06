package team.chisel.ctm.api.texture;

import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import team.chisel.ctm.client.util.BlockRenderLayer;

/**
 * Represents a CTM Texture/resource
 */
@ParametersAreNonnullByDefault
public interface ICTMTexture<T extends ITextureType> {

    /**
     * Transforms a quad to conform with this texture
     * 
     * @param quad
     *            The Quad
     * @param context
     *            The Context NULL CONTEXT MEANS INVENTORY
     * @param quadGoal
     *            Amount of quads that should be made
     * @return A List of Quads
     */
    List<BakedQuad> transformQuad(BakedQuad quad, @Nullable ITextureContext context, int quadGoal);

    Collection<ResourceLocation> getTextures();
    
    /**
     * Gets the block render type of this texture
     * 
     * @return The Rendertype of this texture
     */
    T getType();

    /**
     * Gets the texture for a particle
     * 
     * @return The Texture for a particle
     */
    TextureAtlasSprite getParticle();

    /**
     * The layer this texture requires. The layers will be prioritized for a face in the order:
     * <ul>
     * <li>{@link BlockRenderLayer#TRANSLUCENT}</li>
     * <li>{@link BlockRenderLayer#CUTOUT}</li>
     * <li>{@link BlockRenderLayer#SOLID}</li>
     * </ul>
     *
     * @return The layer of this texture.
     */
    @Nullable
    BlockRenderLayer getLayer();
}
