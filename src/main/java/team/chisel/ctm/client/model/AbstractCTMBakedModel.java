package team.chisel.ctm.client.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.util.RenderContextList;
import team.chisel.ctm.client.state.CTMContext;
import team.chisel.ctm.client.util.ProfileUtil;

@RequiredArgsConstructor
public abstract class AbstractCTMBakedModel implements IDynamicBakedModel {

    private static Cache<ModelResourceLocation, AbstractCTMBakedModel> itemcache = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.SECONDS).<ModelResourceLocation, AbstractCTMBakedModel>build();
    private static Cache<State, AbstractCTMBakedModel> modelcache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).maximumSize(5000).<State, AbstractCTMBakedModel>build();

    public static void invalidateCaches()
    {
        itemcache.invalidateAll();
        modelcache.invalidateAll();
    }

    @ParametersAreNonnullByDefault
    private class Overrides extends ItemOverrides {
                
        public Overrides() {
            super();
        }

        @Override
        @SneakyThrows
        public BakedModel resolve(BakedModel originalModel, ItemStack stack, ClientLevel world, LivingEntity entity, int unknown) {
            Block block = null;
            if (stack.getItem() instanceof BlockItem blockItem) {
                block = blockItem.getBlock();
            }
            final BlockState state = block == null ? null : block.defaultBlockState();
            ModelResourceLocation mrl = ModelUtil.getMesh(stack);
            if (mrl == null) {
                // this must be a missing/invalid model
                return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getModelManager().getMissingModel();
            }
            Random random = new Random();
            random.setSeed(42L);
            return itemcache.get(mrl, () -> createModel(state, model, getParent(random), null, random));
        }
    }
    
    @Getter 
    @RequiredArgsConstructor 
    @ToString
    private static class State {
        private final @Nonnull BlockState cleanState;
        private final @Nullable Object2LongMap<ICTMTexture<?>> serializedContext;
        private final @Nonnull BakedModel parent;
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            State other = (State) obj;
            
            if (cleanState != other.cleanState) {
                return false;
            }
            if (parent != other.parent) {
                return false;
            }

            if (serializedContext == null) {
                if (other.serializedContext != null) {
                    return false;
                }
            } else if (!serializedContext.equals(other.serializedContext)) {
                return false;
            }
            return true;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            // for some reason blockstates hash their properties, we only care about the identity hash
            result = prime * result + System.identityHashCode(cleanState);
            result = prime * result + (parent == null ? 0 : parent.hashCode());
            result = prime * result + (serializedContext == null ? 0 : serializedContext.hashCode());
            return result;
        }
    }
    
    @Getter
    private final @Nonnull IModelCTM model;
    @Getter
    private final @Nonnull BakedModel parent;
    private final @Nonnull Overrides overrides = new Overrides();

    protected final ListMultimap<RenderType, BakedQuad> genQuads = MultimapBuilder.hashKeys().arrayListValues().build();
    protected final Table<RenderType, Direction, List<BakedQuad>> faceQuads = Tables.newCustomTable(new HashMap<>(), () -> Maps.newEnumMap(Direction.class));
    
    private final EnumMap<Direction, ImmutableList<BakedQuad>> noLayerCache = new EnumMap<>(Direction.class);
    private ImmutableList<BakedQuad> noSideNoLayerCache;
    
    protected static final ModelProperty<CTMContext> CTM_CONTEXT = new ModelProperty<>();

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand, IModelData extraData) {
        BakedModel parent = getParent(rand);

        ProfileUtil.start("ctm_models");
        
        AbstractCTMBakedModel baked = this;
        RenderType layer = MinecraftForgeClient.getRenderType();

        try {
	        if (Minecraft.getInstance().level != null && extraData.hasProperty(CTM_CONTEXT)) {
	            ProfileUtil.start("state_creation");
	            RenderContextList ctxList = extraData.getData(CTM_CONTEXT).getContextList(state, baked);
	
	            Object2LongMap<ICTMTexture<?>> serialized = ctxList.serialized();
	            ProfileUtil.endAndStart("model_creation");
	            baked = modelcache.get(new State(state, serialized, parent), () -> createModel(state, model, parent, ctxList, rand));
	            ProfileUtil.end();
	        } else if (state != null)  {
	            ProfileUtil.start("model_creation");
	            baked = modelcache.get(new State(state, null, parent), () -> createModel(state, model, parent, null, rand));
	            ProfileUtil.end();
	        } else {
	            // This SHOULD be invalid, but apparently forge doesn't call getModelData when rendering items. Moving this check to be more specific below
	            // throw new IllegalArgumentException("getQuads called without state and without going through overrides, this is not valid!");
	        }
        } catch (ExecutionException e) {
        	throw new RuntimeException(e);
        }

        ProfileUtil.start("quad_lookup");
        List<BakedQuad> ret;
        if (side != null && layer != null) {
            ret = baked.faceQuads.get(layer, side);
        } else if (side != null) {
            final AbstractCTMBakedModel _baked = baked;
            ret = baked.noLayerCache.computeIfAbsent(side, f -> ImmutableList.copyOf(_baked.faceQuads.column(f).values()
                    .stream()
                    .flatMap(List::stream)
                    .distinct()
                    .toList()));
        } else if (layer != null) {
            ret = baked.genQuads.get(layer);
        } else {
            ret = baked.noSideNoLayerCache;
            if (ret == null) {
                ret = baked.noSideNoLayerCache = ImmutableList.copyOf(baked.genQuads.values()
                        .stream()
                        .distinct()
                        .toList());
            }
        }
        ProfileUtil.end();

        ProfileUtil.end();
        if (ret == null) {
            throw new IllegalStateException("getQuads called on a model that was not properly initialized - by using getOverrides and/or getModelData");
        }
        return ret;
    }

    @Override
    public IModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, IModelData tileData) {
    	if (tileData == EmptyModelData.INSTANCE) {
    		tileData = new ModelDataMap.Builder().withProperty(CTM_CONTEXT).build();
    	}
    	tileData.setData(CTM_CONTEXT, new CTMContext(world, pos));
    	return tileData;
    }

    /**
     * Random sensitive parent, will proxy to {@link WeightedBakedModel} if possible.
     */
    @Nonnull
    public BakedModel getParent(Random rand) {
        if (getParent() instanceof WeightedBakedModel weightedBakedModel) {
            Optional<WeightedEntry.Wrapper<BakedModel>> model = WeightedRandom.getWeightedItem(weightedBakedModel.list, Math.abs((int)rand.nextLong()) % ((WeightedBakedModel)getParent()).totalWeight);
            if (model.isPresent()) {
                return model.get().getData();
            }
        }
        return getParent();
    }
    
    @Override
    public @Nonnull ItemOverrides getOverrides() {
        return overrides;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return parent.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return parent.isGui3d();
    }

    @Override
    public boolean isCustomRenderer() {
        return parent.isCustomRenderer();
    }

    @Override
    public @Nonnull TextureAtlasSprite getParticleIcon() {
        return this.parent.getParticleIcon();
    }

    @Override
    public @Nonnull ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public BakedModel handlePerspective(ItemTransforms.TransformType cameraTransformType, PoseStack poseStack) {
    	parent.handlePerspective(cameraTransformType, poseStack);
        return this;
    }
    
    protected static final RenderType[] LAYERS = RenderType.chunkBufferLayers().toArray(new RenderType[0]);
    
    protected abstract AbstractCTMBakedModel createModel(BlockState state, @Nonnull IModelCTM model, BakedModel parent, RenderContextList ctx, Random rand);

	@Override
	public boolean usesBlockLight() {
		return getParent().usesBlockLight();
	}

    private <T> T applyToParent(Random rand, Function<AbstractCTMBakedModel, T> func) {
        BakedModel parent = getParent(rand);
        if (parent instanceof AbstractCTMBakedModel ctmBakedModel) {
            return func.apply(ctmBakedModel);
        }
        return null;
    }

    protected ICTMTexture<?> getOverrideTexture(Random rand, int tintIndex, ResourceLocation texture) {
        ICTMTexture<?> ret = getModel().getOverrideTexture(tintIndex, texture);
        if (ret == null) {
            ret = applyToParent(rand, parent -> parent.getOverrideTexture(rand, tintIndex, texture));
        }
        return ret;
    }

    protected ICTMTexture<?> getTexture(Random rand, ResourceLocation texture) {
        ICTMTexture<?> ret = getModel().getTexture(texture);
        if (ret == null) {
            ret = applyToParent(rand, parent -> parent.getTexture(rand, texture));
        }
        return ret;
    }
    
    protected TextureAtlasSprite getOverrideSprite(Random rand, int tintIndex) {
        TextureAtlasSprite ret = getModel().getOverrideSprite(tintIndex);
        if (ret == null) {
            ret = applyToParent(rand, parent -> parent.getOverrideSprite(rand, tintIndex));
        }
        return ret;
    }

    public Collection<ICTMTexture<?>> getCTMTextures() {
        ImmutableList.Builder<ICTMTexture<?>> builder = ImmutableList.builder();
        builder.addAll(getModel().getCTMTextures());
        if (getParent() instanceof AbstractCTMBakedModel) {
            builder.addAll(((AbstractCTMBakedModel)getParent()).getCTMTextures());
        }
        return builder.build();
    }
}
