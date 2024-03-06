package team.chisel.ctm.client.texture.ctx;

import java.util.EnumMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.client.texture.render.TextureCTM;
import team.chisel.ctm.client.util.CTMLogic;

public class TextureContextCTM implements ITextureContext {
    
	protected final TextureCTM<?> tex;
	
    private EnumMap<Direction, CTMLogic> ctmData = new EnumMap<>(Direction.class);

    private long data;

    public TextureContextCTM(@NotNull BlockState state, BlockAndTintGetter world, BlockPos pos, TextureCTM<?> tex) {
    	this.tex = tex;
    	
        for (Direction face : Direction.values()) {
            CTMLogic ctm = createCTM(state);
            ctm.getSubmapIds(world, pos, face);
            ctmData.put(face, ctm);
            this.data |= ctm.serialized() << (face.ordinal() * 10);
        }
    }
    
    protected CTMLogic createCTM(@NotNull BlockState state) {
        CTMLogic ret = CTMLogic.getInstance();
        tex.applyTo(ret.connectionCheck);
        return ret;
    }

    public CTMLogic getCTM(Direction face) {
        return ctmData.get(face);
    }

    @Override
    public long getCompressedData(){
        return this.data;
    }
}
