package team.chisel.ctm.api.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraftforge.fml.common.eventhandler.Event;

@RequiredArgsConstructor
@Getter
public class TextureCollectedEvent extends Event {

    private final TextureMap map;
    private final TextureAtlasSprite sprite;

}
