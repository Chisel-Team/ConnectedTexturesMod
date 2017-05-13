package team.chisel.ctm.api.texture;


/**
 * Interface for a block's render context
 */
public interface ITextureContext {

    /**
     * Gets the compressed data, will only use bits up to the given compressed data length
     */
    long getCompressedData();

}
