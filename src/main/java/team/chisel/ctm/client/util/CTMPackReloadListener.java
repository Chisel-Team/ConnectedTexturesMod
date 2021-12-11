package team.chisel.ctm.client.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.server.packs.resources.SimpleReloadableResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Unit;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IRegistryDelegate;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;

public class CTMPackReloadListener extends SimplePreparableReloadListener<Unit> {
    
    @SubscribeEvent
    public void onParticleFactoryRegister(ParticleFactoryRegisterEvent event) {
        // Apparently this is the only event that is posted after other resource loaders are registered, but before
        // the reload begins. We must register here to be AFTER model baking.
        ((SimpleReloadableResourceManager)Minecraft.getInstance().getResourceManager()).registerReloadListener(this);
    }
    
    @Override
    protected Unit prepare(ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
        return Unit.INSTANCE;
    }

    @Override
    protected void apply(Unit objectIn, ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
        ResourceUtil.invalidateCaches();
        TextureMetadataHandler.INSTANCE.invalidateCaches();
        AbstractCTMBakedModel.invalidateCaches();
        refreshLayerHacks();
    }

    private static final Map<IRegistryDelegate<Block>, Predicate<RenderType>> blockRenderChecks = Maps.newHashMap();

    private void refreshLayerHacks() {
        blockRenderChecks.forEach((b, p) -> ItemBlockRenderTypes.setRenderLayer(b.get(), p));
        blockRenderChecks.clear();

        for (Block block : ForgeRegistries.BLOCKS.getValues()) {
            BlockState state = block.defaultBlockState();
            Predicate<RenderType> predicate = getLayerCheck(state, Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(state));

            if (predicate != null) {
                blockRenderChecks.put(block.delegate, getExistingRenderCheck(block));
                ItemBlockRenderTypes.setRenderLayer(block, predicate);
            }
        }
    }
    
    @RequiredArgsConstructor
    private static class CachingLayerCheck implements Predicate<RenderType> {
        
        static <T> CachingLayerCheck of(BlockState state, Collection<T> rawModels, Function<T, BakedModel> converter) {
            List<AbstractCTMBakedModel> ctmModels = rawModels.stream()
                    .map(converter)
                    .filter(m -> m instanceof AbstractCTMBakedModel)
                    .map(m -> (AbstractCTMBakedModel) m)
                    .toList();
            return new CachingLayerCheck(state, ctmModels, ctmModels.size() < rawModels.size());
        }
        
        private final BlockState state;
        private final List<AbstractCTMBakedModel> models;
        private final boolean useFallback;
        
        private final Object2BooleanMap<RenderType> cache = new Object2BooleanOpenHashMap<>();
        
        @Override
        public boolean test(RenderType layer) {
            return cache.computeBooleanIfAbsent(layer, type -> 
                models.stream().anyMatch(m -> m.getModel().canRenderInLayer(state, type)) ||
                (useFallback && canRenderInLayerFallback(state, layer)));
        }
    }
    
    private Predicate<RenderType> getLayerCheck(BlockState state, BakedModel model) {
        if (model instanceof AbstractCTMBakedModel ctmModel) {
            return layer -> ctmModel.getModel().canRenderInLayer(state, layer);
        }
        if (model instanceof WeightedBakedModel weightedModel) {
            return CachingLayerCheck.of(state, weightedModel.list, wm -> wm.getData());
        }
        if (model instanceof MultiPartBakedModel multiPartModel) {
            return CachingLayerCheck.of(state, multiPartModel.selectors, Pair::getRight);
        }
        return null;
    }

    private static final Field _blockRenderChecks = ObfuscationReflectionHelper.findField(ItemBlockRenderTypes.class, "blockRenderChecks");
    private static final MethodHandle _fancyGraphics;
    static {
        try {
            _fancyGraphics = MethodHandles.lookup().unreflectGetter(ObfuscationReflectionHelper.findField(ItemBlockRenderTypes.class, "renderCutout"));
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
                return ((boolean) _fancyGraphics.invokeExact()) ? type == RenderType.cutoutMipped() : type == RenderType.solid();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            java.util.function.Predicate<RenderType> rendertype;
            synchronized (ItemBlockRenderTypes.class) {
                rendertype = blockRenderChecks.get(block.delegate);
            }
            return rendertype != null ? rendertype.test(type) : type == RenderType.solid();
        }
    }
}
