package team.chisel.ctm.client.texture.type;

import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.render.TextureCTMFull;

@TextureType("ctm_full")
public class TextureTypeCTMFull extends TextureTypeCTM {

    @Override
    public ICTMTexture<TextureTypeCTMFull> makeTexture(TextureInfo info) {
        return new TextureCTMFull(this, info);
    }

    @Override
    public int getQuadsPerSide() {
        return 1;
    }

    @Override
    public int requiredTextures() {
        return 3;
    }
}
