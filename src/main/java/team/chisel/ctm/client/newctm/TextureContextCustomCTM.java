package team.chisel.ctm.client.newctm;

import java.util.EnumMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;

public class TextureContextCustomCTM implements ITextureContext {
    
	protected final ICTMTexture<?> tex;
	private final ICTMLogic logic;
	
    private EnumMap<Direction, ILogicCache> ctmData = new EnumMap<>(Direction.class);

    private long data;

    public TextureContextCustomCTM(@NotNull BlockState state, BlockAndTintGetter world, BlockPos pos, ICTMTexture<?> tex, ICTMLogic logic) {
    	this.tex = tex;
    	this.logic = logic;
        ConnectionCheck connectionCheckOverride = null;
        if (this.tex instanceof ITextureConnection texCtm) {
            connectionCheckOverride = texCtm.applyTo(new ConnectionCheck());
        }
    	
        for (Direction face : Direction.values()) {
            ILogicCache ctm = createCTM(state, connectionCheckOverride);
            ctm.buildConnectionMap(world, pos, face);
            ctmData.put(face, ctm);
            this.data |= ctm.serialized() << (face.ordinal() * 10);
        }
    }
    
    protected ILogicCache createCTM(@NotNull BlockState state, @Nullable ConnectionCheck connectionCheckOverride) {
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
