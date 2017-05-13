package team.chisel.ctm.client.texture.ctx;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import team.chisel.ctm.api.texture.ITextureContext;

public class TextureContextPosition implements ITextureContext {

    protected @Nonnull BlockPos position;

    public TextureContextPosition(@Nonnull BlockPos pos) {
        this.position = pos;
    }

    public TextureContextPosition(int x, int y, int z) {
        this(new BlockPos(x, y, z));
    }
    
    public TextureContextPosition applyOffset() {
        this.position = position.add(OffsetProviderRegistry.INSTANCE.getOffset(Minecraft.getMinecraft().world, position));
        return this;
    }

    public @Nonnull BlockPos getPosition() {
        return position;
    }

    @Override
    public long getCompressedData() {
        return 0L; // Position data is not useful for serialization (and in fact breaks caching as each location is a new key)
    }
}
