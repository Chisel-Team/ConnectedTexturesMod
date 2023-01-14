package team.chisel.ctm.client.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Unit;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.Holder;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;

public class CTMPackReloadListener extends SimplePreparableReloadListener<Unit> {

    @SubscribeEvent
    public void onParticleFactoryRegister(RegisterParticleProvidersEvent event) {
        // Apparently this is the only event that is posted after other resource loaders are registered, but before
        // the reload begins. We must register here to be AFTER model baking.
        ((ReloadableResourceManager)Minecraft.getInstance().getResourceManager()).registerReloadListener(this);
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
    }

    private static final Map<Holder.Reference<Block>, Predicate<RenderType>> blockRenderChecks = Maps.newHashMap();

    @RequiredArgsConstructor
    public static class CachingLayerCheck implements Predicate<RenderType> {

        public static <T> ChunkRenderTypeSet renderTypeSet(BlockState state, Collection<T> rawModels, Function<T, BakedModel> converter) {
            return ChunkRenderTypeSet.of(
                    RenderType.chunkBufferLayers()
                            .stream()
                            .filter(of(state, rawModels, converter))
                            .toArray(RenderType[]::new)
            );
        }

        public static <T> CachingLayerCheck of(BlockState state, Collection<T> rawModels, Function<T, BakedModel> converter) {
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
        public boolean test(@Nullable RenderType layer) {
            return cache.computeIfAbsent(layer, (RenderType type) -> 
                models.stream().anyMatch(m -> m.getModel().canRenderInLayer(state, type)) ||
                (useFallback && canRenderInLayerFallback(state, type)));
        }
    }

    private static final Field _blockRenderChecks = ObfuscationReflectionHelper.findField(ItemBlockRenderTypes.class, "BLOCK_RENDER_TYPES");
    private static final MethodHandle _fancyGraphics;
    static {
        try {
            _fancyGraphics = MethodHandles.lookup().unreflectGetter(ObfuscationReflectionHelper.findField(ItemBlockRenderTypes.class, "f_109277_"));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private ChunkRenderTypeSet getExistingRenderCheck(Block block) {
        try {
            return ((Map<Holder.Reference<Block>, ChunkRenderTypeSet>) _blockRenderChecks.get(null)).get(ForgeRegistries.BLOCKS.getDelegateOrThrow(block));
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
                rendertype = blockRenderChecks.get(ForgeRegistries.BLOCKS.getDelegateOrThrow(block));
            }
            return rendertype != null ? rendertype.test(type) : type == RenderType.solid();
        }
    }
}
