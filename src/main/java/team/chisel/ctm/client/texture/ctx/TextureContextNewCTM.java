package team.chisel.ctm.client.texture.ctx;

import java.util.EnumMap;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.client.util.CTMLogicBakery;
import team.chisel.ctm.client.util.NewCTMLogic;

public class TextureContextNewCTM implements ITextureContext {
    
	protected final ICTMTexture<?> tex;
	
    private EnumMap<Direction, NewCTMLogic> ctmData = new EnumMap<>(Direction.class);

    private long data;

    public TextureContextNewCTM(@Nonnull BlockState state, BlockGetter world, BlockPos pos, ICTMTexture<?> tex) {
    	this.tex = tex;
    	
        for (Direction face : Direction.values()) {
            NewCTMLogic ctm = createCTM(state);
            ctm.getSubmaps(world, pos, face);
            ctmData.put(face, ctm);
            this.data |= ctm.serialized() << (face.ordinal() * 10);
        }
    }
    
    protected NewCTMLogic createCTM(@Nonnull BlockState state) {
        return CTMLogicBakery.TEST_OF.bake();
    }

    public NewCTMLogic getCTM(Direction face) {
        return ctmData.get(face);
    }

    @Override
    public long getCompressedData(){
        return this.data;
    }
}
