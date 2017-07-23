package team.chisel.ctm.client.texture.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.render.TextureEdges;
import team.chisel.ctm.client.util.CTMLogic;

@TextureType("EDGES")
public class TextureTypeEdges extends TextureTypeCTM {

    @Override
    public ICTMTexture<? extends TextureTypeCTM> makeTexture(TextureInfo info) {
        return new TextureEdges(this, info);
    }
    
    @RequiredArgsConstructor
    public static class CTMLogicEdges extends CTMLogic {
        
        protected final TextureEdges tex;
        
        @Setter
        @Getter
        private boolean obscured;
        
        @Override
        public boolean isConnected(IBlockAccess world, BlockPos current, BlockPos connection, EnumFacing dir, IBlockState state) {
            if (obscured) {
                return false;
            }
            IBlockState con = world.getBlockState(connection);
            if (((TextureEdges)tex).getConnectTo().contains(con.getBlock())) {
                return true;
            }
            IBlockState obscuring = world.getBlockState(current.offset(dir));
            if (((TextureEdges)tex).getConnectTo().contains(obscuring.getBlock())) {
                obscured = true;
                return false;
            }
            IBlockState obscuringcon = world.getBlockState(connection.offset(dir));
            if (((TextureEdges)tex).getConnectTo().contains(obscuringcon.getBlock())) {
                return true;
            }
            return false;
        }
        
        @Override
        public long serialized() {
            return isObscured() ? (super.serialized() | (1 << 8)) : super.serialized();
        }
    }
    
    @Override
    public TextureContextCTM getBlockRenderContext(IBlockState state, IBlockAccess world, BlockPos pos, ICTMTexture<?> tex) {
        return new TextureContextCTM(state, world, pos, (TextureEdges) tex) {
            
            @Override
            protected CTMLogic createCTM(IBlockState state) {
                return new CTMLogicEdges((TextureEdges) tex);
            }
        };
    }

    @Override
    public int requiredTextures() {
        return 3;
    }
}
