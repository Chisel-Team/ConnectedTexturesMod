package team.chisel.ctm.client.model;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.mojang.blaze3d.matrix.MatrixStack;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.model.WeightedBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import net.minecraft.world.World;
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
    private class Overrides extends ItemOverrideList {
                
        public Overrides() {
            super();
        }

        @Override
        @SneakyThrows
        public IBakedModel getModelWithOverrides(IBakedModel originalModel, ItemStack stack, World world, LivingEntity entity) {
            Block block = null;
            if (stack.getItem() instanceof BlockItem) {
                block = ((BlockItem) stack.getItem()).getBlock();
            }
            final BlockState state = block == null ? null : block.getDefaultState();
            ModelResourceLocation mrl = ModelUtil.getMesh(stack);
            if (mrl == null) {
                // this must be a missing/invalid model
                return Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getMissingModel();
            }
            return itemcache.get(mrl, () -> createModel(state, model, null, new Random()));
        }
    }
    
    @Getter 
    @RequiredArgsConstructor 
    @ToString
    private static class State {
        private final @Nonnull BlockState cleanState;
        private final @Nullable Object2LongMap<ICTMTexture<?>> serializedContext;
        private final @Nonnull IBakedModel parent;
        
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
    private final @Nonnull IBakedModel parent;
    private final @Nonnull Overrides overrides = new Overrides();

    protected final ListMultimap<RenderType, BakedQuad> genQuads = MultimapBuilder.hashKeys().arrayListValues().build();
    protected final Table<RenderType, Direction, List<BakedQuad>> faceQuads = Tables.newCustomTable(new HashMap<>(), () -> Maps.newEnumMap(Direction.class));
    
    protected static final ModelProperty<CTMContext> CTM_CONTEXT = new ModelProperty<>();

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand, IModelData extraData) {   
        IBakedModel parent = getParent(rand);

        ProfileUtil.start("ctm_models");
        
        AbstractCTMBakedModel baked = this;
        RenderType layer = MinecraftForgeClient.getRenderLayer();

        try {
	        if (Minecraft.getInstance().world != null && extraData.hasProperty(CTM_CONTEXT)) {
	            ProfileUtil.start("state_creation");
	            RenderContextList ctxList = extraData.getData(CTM_CONTEXT).getContextList(state, model);
	
	            Object2LongMap<ICTMTexture<?>> serialized = ctxList.serialized();
	            ProfileUtil.endAndStart("model_creation");
	            baked = modelcache.get(new State(state, serialized, parent), () -> createModel(state, model, ctxList, rand));
	            ProfileUtil.end();
	        } else if (state != null)  {
	            ProfileUtil.start("model_creation");
	            baked = modelcache.get(new State(state, null, getParent(rand)), () -> createModel(state, model, null, rand));
	            ProfileUtil.end();
	        }
        } catch (ExecutionException e) {
        	throw new RuntimeException(e);
        }

        ProfileUtil.start("quad_lookup");
        List<BakedQuad> ret;
        if (side != null && layer != null) {
            ret = baked.faceQuads.get(layer, side);
        } else if (side != null) {
            ret = baked.faceQuads.column(side).values().stream().flatMap(List::stream).collect(Collectors.toList());
        } else if (layer != null) {
            ret = baked.genQuads.get(layer);
        } else {
            ret = Lists.newArrayList(baked.genQuads.values());
        }
        ProfileUtil.end();

        ProfileUtil.end();
        return ret;
    }
    
    @Override
    public IModelData getModelData(ILightReader world, BlockPos pos, BlockState state, IModelData tileData) {
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
    public IBakedModel getParent(Random rand) {
        if (getParent() instanceof WeightedBakedModel) {
        	 List<WeightedBakedModel.WeightedModel> models = ((WeightedBakedModel)getParent()).models;
        	 return WeightedRandom.getRandomItem(models, Math.abs((int)rand.nextLong()) % ((WeightedBakedModel)getParent()).totalWeight).model;
        }
        return getParent();
    }
    
    @Override
    public @Nonnull ItemOverrideList getOverrides() {
        return overrides;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return parent.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return parent.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public @Nonnull TextureAtlasSprite getParticleTexture() {
        return this.parent.getParticleTexture();
    }

    @Override
    public @Nonnull ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }

    @Override
    public IBakedModel handlePerspective(ItemCameraTransforms.TransformType cameraTransformType, MatrixStack ms) {
    	parent.handlePerspective(cameraTransformType, ms);
        return this;
    }
    
    protected static final RenderType[] LAYERS = RenderType.getBlockRenderTypes().toArray(new RenderType[0]);
    
    protected abstract AbstractCTMBakedModel createModel(BlockState state, @Nonnull IModelCTM model, RenderContextList ctx, Random rand);

	@Override
	public boolean func_230044_c_() {
		return getParent().func_230044_c_();
	}

}
