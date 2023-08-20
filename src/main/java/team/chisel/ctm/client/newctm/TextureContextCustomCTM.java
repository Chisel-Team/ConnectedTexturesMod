package team.chisel.ctm.client.newctm;

import java.util.EnumMap;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.client.texture.render.TextureCTM;

public class TextureContextCustomCTM implements ITextureContext {
    
	protected final ICTMTexture<?> tex;
	private final ICTMLogic logic;
	
    private EnumMap<Direction, ILogicCache> ctmData = new EnumMap<>(Direction.class);

    private long data;

    public TextureContextCustomCTM(@Nonnull BlockState state, BlockAndTintGetter world, BlockPos pos, ICTMTexture<?> tex, ICTMLogic logic) {
    	this.tex = tex;
    	this.logic = logic;
        ConnectionCheck connectionCheckOverride = null;
        if (this.tex instanceof TextureCustomCTM<?> texCtm) {
            connectionCheckOverride = new ConnectionCheck()
                  .ignoreStates(texCtm.ignoreStates())
                  .stateComparator(texCtm::connectTo);
            connectionCheckOverride.disableObscuredFaceCheck = texCtm.connectInside();
        } else if (this.tex instanceof TextureCTM<?> texCtm) {
            connectionCheckOverride = new ConnectionCheck()
                  .ignoreStates(texCtm.ignoreStates())
                  .stateComparator(texCtm::connectTo);
            connectionCheckOverride.disableObscuredFaceCheck = texCtm.connectInside();
        }
    	
        for (Direction face : Direction.values()) {
            ILogicCache ctm = createCTM(state, connectionCheckOverride);
            ctm.buildConnectionMap(world, pos, face);
            ctmData.put(face, ctm);
            this.data |= ctm.serialized() << (face.ordinal() * 10);
        }
    }
    
    protected ILogicCache createCTM(@Nonnull BlockState state, @Nullable ConnectionCheck connectionCheckOverride) {
        return logic.cached(connectionCheckOverride);
    }

    public ILogicCache getCTM(Direction face) {
        return ctmData.get(face);
    }

    @Override
    public long getCompressedData(){
        return this.data;
    }
}
