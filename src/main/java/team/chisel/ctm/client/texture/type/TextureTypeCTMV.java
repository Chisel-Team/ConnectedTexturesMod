package team.chisel.ctm.client.texture.type;

import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.render.TextureCTMV;

@TextureType("ctm_vertical")
public class TextureTypeCTMV extends TextureTypeCTM {

    @Override
    public ICTMTexture<TextureTypeCTMV> makeTexture(TextureInfo info) {
        return new TextureCTMV(this, info);
    }
    
    @Override
    public int getQuadsPerSide() {
        return 1;
    }
    
    @Override
    public int requiredTextures() {
    	return 1;
    }
}