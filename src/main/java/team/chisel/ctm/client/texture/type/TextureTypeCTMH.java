package team.chisel.ctm.client.texture.type;

import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.render.TextureCTMH;

@TextureType("CTMH")
public class TextureTypeCTMH extends TextureTypeCTM {

    @Override
    public ICTMTexture<TextureTypeCTMH> makeTexture(TextureInfo info) {
        return new TextureCTMH(this, info);
    }
    
    @Override
    public int getQuadsPerSide() {
        return 1;
    }
}