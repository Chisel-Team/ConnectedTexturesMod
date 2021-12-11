package team.chisel.ctm.client.texture.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.Direction;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.render.TexturePlane;

@RequiredArgsConstructor
public class TextureTypePlane extends TextureTypeCTM {
    @TextureType("ctmh")
    @TextureType("ctm_horizontal")
    public static final TextureTypePlane H = new TextureTypePlane(Direction.Plane.HORIZONTAL);
    @TextureType("ctm_vertical")
    public static final TextureTypePlane V = new TextureTypePlane(Direction.Plane.VERTICAL);
    
    @Getter
    private final Direction.Plane plane;
    
    @Override
    public ICTMTexture<TextureTypePlane> makeTexture(TextureInfo info) {
        return new TexturePlane(this, info);
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