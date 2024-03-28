package team.chisel.ctm.client.texture.ctx;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.jetbrains.annotations.NotNull;
import team.chisel.ctm.api.texture.ITextureContext;

public class TextureContextPosition implements ITextureContext {

    protected @NotNull
    BlockPos position;

    public TextureContextPosition(@NotNull BlockPos pos) {
        this.position = pos.immutable();
    }

    public TextureContextPosition(int x, int y, int z) {
        this(new BlockPos(x, y, z));
    }
    
    public TextureContextPosition applyOffset(BlockAndTintGetter world) {
        this.position = position.offset(OffsetProviderRegistry.INSTANCE.getOffset(world, position));
        return this;
    }

    public @NotNull BlockPos getPosition() {
        return position;
    }

    @Override
    public long getCompressedData() {
        return 0L; // Position data is not useful for serialization (and in fact breaks caching as each location is a new key)
    }
}
