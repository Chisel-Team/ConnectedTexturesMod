package team.chisel.ctm.client.texture.type;

import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.render.TextureEdgesFull;

@TextureType("EDGES_FULL")
public class TextureTypeEdgesFull extends TextureTypeEdges {
    
    @Override
    public ICTMTexture<? extends TextureTypeCTM> makeTexture(TextureInfo info) {
        return new TextureEdgesFull(this, info);
    }

    @Override
    public int requiredTextures() {
        return 2;
    }

    @Override
    public int getQuadsPerSide() {
        return 1;
    }
}
