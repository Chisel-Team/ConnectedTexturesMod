package team.chisel.ctm.api.texture;

import javax.annotation.ParametersAreNonnullByDefault;

import team.chisel.ctm.api.util.TextureInfo;

/**
 * Interface for chisel block render types MUST HAVE A NO ARGS CONSTRUCTOR
 */
@ParametersAreNonnullByDefault
public interface ITextureType extends IContextProvider {

    /**
     * Make a Chisel Texture from a list of sprites.
     * <p>
     * Tip: You can explicitly type the return of this method without any warnings or errors. For instance <blockquote>
     * <code>public IChiselTexture{@literal <}MyRenderType{@literal >} makeTexture(...) {...}</code> </blockquote> Is a valid override of this method.
     * 
     * @param info A {@link TextureInfo} object which contains all the information that about this texture
     */
    <T extends ITextureType> ICTMTexture<? extends T> makeTexture(TextureInfo info);


    /**
     * Gets the amount of quads per side
     * 
     * @return The Amount of quads per side
     */
    default int getQuadsPerSide() {
        return 1;
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
