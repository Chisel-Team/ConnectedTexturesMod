package team.chisel.ctm.client.util;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.RenderType;

@RequiredArgsConstructor
public enum BlockRenderLayer {
	
	SOLID(RenderType::solid),
	CUTOUT(RenderType::cutout),
	CUTOUT_MIPPED(RenderType::cutoutMipped),
	TRANSLUCENT(RenderType::translucent),
    TRIPWIRE(RenderType::tripwire)
	;
	
    private static final Map<RenderType, BlockRenderLayer> reverseLookup = new IdentityHashMap<>();
    static {
        for (BlockRenderLayer layer : values()) {
            reverseLookup.put(layer.getRenderType(), layer);
        }
    }
    
    @Getter
	private final RenderType renderType;
	
	private BlockRenderLayer(Supplier<RenderType> renderType) {
	    this.renderType = renderType.get();
	}
	
    public static BlockRenderLayer fromType(RenderType layer) {
        return reverseLookup.get(layer);
    }
}
