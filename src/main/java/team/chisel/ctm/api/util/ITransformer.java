package team.chisel.ctm.api.util;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

/**
 * Implementation not in api
 * Api call to create with quad number and an IDataProvider
 */
public interface ITransformer<D extends ITransformer.IRenderData> {

    /**
     * Get the amount of quads this transform needs
     */
    int getQuads();

    /**
     * Actually do the transform
     * @param format The VertexFormat
     * @param storage The Data storage, this is the entire quad, all four vertex records joined, the base Int Storage implementation.
     * @param data The Render context data
     */
    void transform(VertexFormat format, IIntStorage storage, D data);

    IDataProvider<D> getDataProvider();

    /**
     * Provides data for transformer
     */
    interface IDataProvider<D extends IRenderData> {

        D getFromCompressed(long data);

        D getRenderData(IBlockState state, IBlockAccess world, @Nonnull BlockPos pos);

        /**
         * Convert the data, and then put it into the Int storage
         */
        void putData(IIntStorage storage, D data);

        IDataInserter<D> getDataInserter(D data);
    }

    /**
     * Represents the data needed to be used in a given transform
     */
    interface IRenderData {

        long getCompressed();
    }

    interface IDataInserter<D extends IRenderData> {

        /**
         * Inserts the data into the quad
         * @param data The data block for the element that this inserter targets in the given vertex in the given quad
         * @param quad The quad, index 0-(amount of quads - 1)
         * @param vertex The vertex, 1-4, clockwise
         */
        void putData(IIntStorage data, int quad, int vertex);

    }
}