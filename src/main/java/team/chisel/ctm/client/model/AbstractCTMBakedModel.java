package team.chisel.ctm.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.BakedModelWrapper;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.util.RenderContextList;
import team.chisel.ctm.client.state.CTMContext;
import team.chisel.ctm.client.util.ProfileUtil;

public abstract class AbstractCTMBakedModel extends BakedModelWrapper<BakedModel> {

    private static Cache<ModelResourceLocation, AbstractCTMBakedModel> itemcache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .maximumSize(0)
            .<ModelResourceLocation, AbstractCTMBakedModel>build();
    private static Cache<State, AbstractCTMBakedModel> modelcache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
//            .maximumSize(5000)
            .maximumSize(0)
            .<State, AbstractCTMBakedModel>build();

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
            RandomSource random = RandomSource.create();
            random.setSeed(42L);
            return itemcache.get(mrl, () -> createModel(state, model, getParent(random), null, random, ModelData.EMPTY, null));
        }
    }
    
    @Getter 
    @RequiredArgsConstructor 
    @ToString
    private static class State {
        private final @Nonnull BlockState cleanState;
        private final @Nullable Object2LongMap<ICTMTexture<?>> serializedContext;
        private final @Nonnull BakedModel parent;
        private final @Nullable RenderType layer;
        
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
            if (layer != other.layer) {
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
            result = prime * result + (layer == null ? 0 : layer.hashCode());
            return result;
        }
    }
    
    @Getter
    private final @Nonnull IModelCTM model;
    private final @Nonnull Overrides overrides = new Overrides();

    private final @Nullable RenderType layer;
    protected final List<BakedQuad> genQuads = new ArrayList<>();
    protected final ListMultimap<Direction, BakedQuad> faceQuads = ArrayListMultimap.create();

    public AbstractCTMBakedModel(@Nonnull IModelCTM model, BakedModel parent, @Nullable RenderType layer) {
        super(parent);
        this.model = model;
        this.layer = layer;
    }
    
//    private final EnumMap<Direction, ImmutableList<BakedQuad>> noLayerCache = new EnumMap<>(Direction.class);
//    private ImmutableList<BakedQuad> noSideNoLayerCache;
    
    protected static final ModelProperty<CTMContext> CTM_CONTEXT = new ModelProperty<>();

    @Nonnull
    @Override
    public final List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand) {
        //Unlike IDynamicBakedModel pass our actual layer as a proxy
        return getQuads(state, side, rand, ModelData.EMPTY, layer);
    }
    
    @Override
    public final List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand, @Nonnull ModelData extraData, @Nullable RenderType layer) {
        ProfileUtil.start("ctm_models");

        BakedModel parent = getParent(rand);
        AbstractCTMBakedModel baked;

        try {
            if (state == null) {
                return quadLookup(side, layer);
            } else if (Minecraft.getInstance().level != null && extraData.has(CTM_CONTEXT)) {
	            ProfileUtil.start("state_creation");
	            RenderContextList ctxList = extraData.get(CTM_CONTEXT).getContextList(state, this);
	
	            Object2LongMap<ICTMTexture<?>> serialized = ctxList.serialized();
	            ProfileUtil.endAndStart("model_creation"); // state_creation
	            baked = modelcache.get(new State(state, serialized, parent, layer), () -> createModel(state, model, parent, ctxList, rand, extraData, layer));
	            ProfileUtil.end(); // model_creation
	        } else if (state != null)  {
	            ProfileUtil.start("model_creation");
	            baked = modelcache.get(new State(state, null, parent, layer), () -> createModel(state, model, parent, null, rand, extraData, layer));
	            ProfileUtil.end(); // model_creation
	        } else {
	            throw new IllegalStateException("Unreachable? Block: " + state + "   layer: " + layer);
	        }
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
        	throw new RuntimeException(e);
        }
        
        ProfileUtil.end(); // ctm_models
        
        var quads = baked.quadLookup(side, layer);
//        System.out.println(Objects.toString(state) + "/" + Objects.toString(side) + "/" + Objects.toString(layer == null ? layer : layer.toString().substring(11, 17)) + "/" + (baked.layer == null ? "null" : baked.layer.toString().substring(11, 17)) + ": " + quads.size());
        return quads;
    }

    protected final List<BakedQuad> quadLookup(@Nullable Direction side, @Nullable RenderType layer) {
        ProfileUtil.start("quad_lookup");
        List<BakedQuad> ret = Collections.emptyList();
        if (layer == this.layer) {
            if (side != null) {
                ret = this.faceQuads.get(side);
            } else {
                ret = this.genQuads;
            }
        }
        ProfileUtil.end(); // quad_lookup
        
        if (ret == null) {
            throw new IllegalStateException("getQuads called on a model that was not properly initialized - by using getOverrides and/or getModelData");
        }
        
        ProfileUtil.end(); // ctm_models
        return ret;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@Nonnull BlockState state, @Nonnull RandomSource rand, @Nonnull ModelData data) {
        ChunkRenderTypeSet extraTypes = layer != null ? ChunkRenderTypeSet.of(layer) : ChunkRenderTypeSet.of(getModel().getExtraLayers(state));
        return ChunkRenderTypeSet.union(extraTypes, super.getRenderTypes(state, rand, data));
    }

    @Override
    public List<RenderType> getRenderTypes(@Nonnull ItemStack itemStack, boolean fabulous) {
        List<RenderType> ret = new ArrayList<>(super.getRenderTypes(itemStack, fabulous));
        if (this.layer != null) {
            if (!ret.contains(layer)) {
                ret.add(layer);
            }
        } else {
            //Note: Uses this model as opposed to parent so that any layers added by CTM can be checked as well
            var type = RenderTypeHelper.getFallbackItemRenderType(itemStack, this, false);
            if (!ret.contains(type)) {
                ret.add(type);
            }
        }
        
        return ret;
    }

    @Override
    public BakedModel applyTransform(@Nonnull ItemDisplayContext displayContext, @Nonnull PoseStack mat, boolean applyLeftHandTransform) {
        // have the original model apply any perspective transforms onto the MatrixStack
        super.applyTransform(displayContext, mat, applyLeftHandTransform);
        // return this model, as we want to draw the item variant quads ourselves
        return this;
    }

    @Override
    public List<BakedModel> getRenderPasses(@Nonnull ItemStack stack, boolean fabulous) {
        //Make sure our model is the one that gets rendered rather than the internal one
        return List.of(this);
    }

    @Nonnull
    @Override
    public ModelData getModelData(@Nonnull BlockAndTintGetter world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull ModelData tileData) {
        //Add any extra model data the parent model may be expecting or want to add
        tileData = super.getModelData(world, pos, state, tileData);
    	if (!tileData.has(CTM_CONTEXT)) {
    		//Ensure the position is immutable in case another mod persists the model data longer than the position
    		tileData = tileData.derive().with(CTM_CONTEXT, new CTMContext(world, pos.immutable())).build();
    	}
    	return tileData;
    }

    /**
     * Random sensitive parent, will proxy to {@link WeightedBakedModel} if possible.
     */
    @Nonnull
    public BakedModel getParent(RandomSource rand) {
        if (getParent() instanceof WeightedBakedModel weightedBakedModel) {
            Optional<WeightedEntry.Wrapper<BakedModel>> model = WeightedRandom.getWeightedItem(weightedBakedModel.list, Math.abs((int)rand.nextLong()) % weightedBakedModel.totalWeight);
            if (model.isPresent()) {
                return model.get().getData();
            }
        }
        return getParent();
    }

    @Nonnull
    public BakedModel getParent() {
        return originalModel;
    }
    
    @Override
    public @Nonnull ItemOverrides getOverrides() {
        return overrides;
    }
    
    protected abstract AbstractCTMBakedModel createModel(BlockState state, @Nonnull IModelCTM model, BakedModel parent, RenderContextList ctx, RandomSource rand, ModelData data, @Nullable RenderType layer);

    @Nullable
    private <T> T applyToParent(RandomSource rand, Function<AbstractCTMBakedModel, T> func) {
        BakedModel parent = getParent(rand);
        if (parent instanceof AbstractCTMBakedModel ctmBakedModel) {
            return func.apply(ctmBakedModel);
        }
        return null;
    }

    @Nullable
    protected ICTMTexture<?> getOverrideTexture(RandomSource rand, int tintIndex, ResourceLocation texture) {
        ICTMTexture<?> ret = getModel().getOverrideTexture(tintIndex, texture);
        if (ret == null) {
            ret = applyToParent(rand, parent -> parent.getOverrideTexture(rand, tintIndex, texture));
        }
        return ret;
    }

    @Nullable
    protected ICTMTexture<?> getTexture(RandomSource rand, ResourceLocation texture) {
        ICTMTexture<?> ret = getModel().getTexture(texture);
        if (ret == null) {
            ret = applyToParent(rand, parent -> parent.getTexture(rand, texture));
        }
        return ret;
    }

    @Nullable
    protected TextureAtlasSprite getOverrideSprite(RandomSource rand, int tintIndex) {
        TextureAtlasSprite ret = getModel().getOverrideSprite(tintIndex);
        if (ret == null) {
            ret = applyToParent(rand, parent -> parent.getOverrideSprite(rand, tintIndex));
        }
        return ret;
    }

    public Collection<ICTMTexture<?>> getCTMTextures() {
        ImmutableList.Builder<ICTMTexture<?>> builder = ImmutableList.builder();
        builder.addAll(getModel().getCTMTextures());
        if (getParent() instanceof AbstractCTMBakedModel ctmBakedModel) {
            builder.addAll(ctmBakedModel.getCTMTextures());
        }
        return builder.build();
    }
}
