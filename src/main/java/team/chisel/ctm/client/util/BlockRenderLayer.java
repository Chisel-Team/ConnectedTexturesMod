package team.chisel.ctm.client.util;

import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@RequiredArgsConstructor
public enum BlockRenderLayer {
	
	SOLID(() -> RenderType::getSolid),
	CUTOUT(() -> RenderType::getCutout),
	CUTOUT_MIPPED(() -> RenderType::getCutoutMipped),
	TRANSLUCENT(() -> RenderType::getTranslucent),
	;
	
	private final Supplier<Supplier<RenderType>> renderType;
	
	@OnlyIn(Dist.CLIENT)
	public RenderType getRenderType() {
		return renderType.get().get();
	}
}
