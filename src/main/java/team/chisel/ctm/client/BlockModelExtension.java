package team.chisel.ctm.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;

public interface BlockModelExtension {
    Int2ObjectMap<IMetadataSectionCTM> getMetaOverrides();

    void setMetaOverrides(Int2ObjectMap<IMetadataSectionCTM> overrides);
}
