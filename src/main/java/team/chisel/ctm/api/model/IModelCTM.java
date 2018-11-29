package team.chisel.ctm.api.model;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.client.model.IModel;
import team.chisel.ctm.api.texture.ICTMTexture;

public interface IModelCTM extends IModel {
    
    IModel getVanillaParent();

    void load();
    
    Collection<ICTMTexture<?>> getCTMTextures();
    
    ICTMTexture<?> getTexture(String iconName);

    boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer);

    @Nullable
    TextureAtlasSprite getOverrideSprite(int tintIndex);

    @Nullable
    ICTMTexture<?> getOverrideTexture(int tintIndex, String sprite);
}
