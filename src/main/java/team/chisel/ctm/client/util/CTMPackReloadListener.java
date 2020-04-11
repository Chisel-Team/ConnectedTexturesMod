package team.chisel.ctm.client.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Unit;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.WeightedBakedModel;
import net.minecraft.client.resources.ReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.SimpleReloadableResourceManager;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IRegistryDelegate;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;

public class CTMPackReloadListener extends ReloadListener<Unit> {
    
    @SubscribeEvent
    public void onParticleFactoryRegister(ParticleFactoryRegisterEvent event) {
        // Apparently this is the only event that is posted after other resource loaders are registered, but before
        // the reload begins. We must register here to be AFTER model baking.
        ((SimpleReloadableResourceManager)Minecraft.getInstance().getResourceManager()).addReloadListener(this);
    }
    
    @Override
    protected Unit prepare(IResourceManager resourceManagerIn, IProfiler profilerIn) {
        ResourceUtil.invalidateCaches();
        TextureMetadataHandler.INSTANCE.invalidateCaches();
        AbstractCTMBakedModel.invalidateCaches();
        return Unit.INSTANCE;
    }

    @Override
    protected void apply(Unit objectIn, IResourceManager resourceManagerIn, IProfiler profilerIn) {
        refreshLayerHacks();
    }

    private static final Map<IRegistryDelegate<Block>, Predicate<RenderType>> blockRenderChecks = Maps.newHashMap();

    private void refreshLayerHacks() {
        blockRenderChecks.forEach((b, p) -> RenderTypeLookup.setRenderLayer(b.get(), p));
        blockRenderChecks.clear();

        for (Block block : ForgeRegistries.BLOCKS.getValues()) {
            BlockState state = block.getDefaultState();
            IBakedModel model = Minecraft.getInstance().getModelManager().getBlockModelShapes().getModel(state);
            final IBakedModel actualModel = model instanceof WeightedBakedModel ? ((WeightedBakedModel) model).baseModel : model;

            if (actualModel instanceof AbstractCTMBakedModel) {
                blockRenderChecks.put(block.delegate, getExistingRenderCheck(block));
                RenderTypeLookup.setRenderLayer(block, layer -> ((AbstractCTMBakedModel) actualModel).getModel().canRenderInLayer(state, layer));
            }
        }
    }

    private static final Field _blockRenderChecks = ObfuscationReflectionHelper.findField(RenderTypeLookup.class, "blockRenderChecks");
    private static final MethodHandle _fancyGraphics;
    static {
        try {
            _fancyGraphics = MethodHandles.lookup().unreflectGetter(ObfuscationReflectionHelper.findField(RenderTypeLookup.class, "field_228388_c_"));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Predicate<RenderType> getExistingRenderCheck(Block block) {
        try {
            return ((Map<IRegistryDelegate<Block>, Predicate<RenderType>>) _blockRenderChecks.get(null)).get(block.delegate);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean canRenderInLayerFallback(BlockState state, RenderType type) {
        Block block = state.getBlock();
        if (block instanceof LeavesBlock) {
            try {
                return ((boolean) _fancyGraphics.invokeExact()) ? type == RenderType.getCutoutMipped() : type == RenderType.getSolid();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            java.util.function.Predicate<RenderType> rendertype;
            synchronized (RenderTypeLookup.class) {
                rendertype = blockRenderChecks.get(block.delegate);
            }
            return rendertype != null ? rendertype.test(type) : type == RenderType.getSolid();
        }
    }
}
