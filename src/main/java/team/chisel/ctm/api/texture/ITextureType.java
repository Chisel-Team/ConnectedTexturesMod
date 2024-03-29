package team.chisel.ctm.api.texture;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.util.Submap;

/**
 * Root interface representing a type of CTM texture. To register, use {@link TextureType}.
 */
@ParametersAreNonnullByDefault
public interface ITextureType extends IContextProvider {

    /**
     * Make a CTM Texture from a list of sprites.
     * <p>
     * Tip: You can explicitly type the return of this method without any warnings or errors. For instance <blockquote>
     * <code>public ICTMTexture{@literal <}MyRenderType{@literal >} makeTexture(...) {...}</code> </blockquote> Is a valid override of this method.
     * 
     * @param info A {@link TextureInfo} object which contains all the information that about this texture
     */
    <T extends ITextureType> ICTMTexture<? extends T> makeTexture(TextureInfo info);


    /**
     * Gets the amount of quads per side
     * 
     * @return The Amount of quads per side
     */
    @Deprecated
    default int getQuadsPerSide() {
        return getOutputFaces().size();
    }

    default List<ISubmap> getOutputFaces() {
        return List.of(Submap.X1);
    }

    /**
     * The amount of textures required for this render type. For instance CTM requires two.
     * 
     * @return The amount of textures required.
     */
    default int requiredTextures() {
        return 1;
    }
}
