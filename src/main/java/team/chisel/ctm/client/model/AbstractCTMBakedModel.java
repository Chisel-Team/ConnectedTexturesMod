package team.chisel.ctm.client.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.model.WeightedBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.model.TRSRTransformation;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.util.RenderContextList;
import team.chisel.ctm.client.asm.CTMCoreMethods;
import team.chisel.ctm.client.state.CTMExtendedState;
import team.chisel.ctm.client.util.ProfileUtil;

@RequiredArgsConstructor
public abstract class AbstractCTMBakedModel implements IBakedModel {

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
        public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, World world, EntityLivingBase entity) {
            Block block = null;
            if (stack.getItem() instanceof ItemBlock) {
                block = ((ItemBlock) stack.getItem()).getBlock();
            }
            final IBlockState state = block == null ? null : block.getDefaultState();
            ModelResourceLocation mrl = ModelUtil.getMesh(stack);
            if (mrl == null) {
                // this must be a missing/invalid model
                return Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getMissingModel();
            }
            return itemcache.get(mrl, () -> createModel(state, model, null, 0));
        }
    }
    
    @Getter 
    @RequiredArgsConstructor 
    @ToString
    private static class State {
        private final @Nonnull IBlockState cleanState;
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

    protected final ListMultimap<BlockRenderLayer, BakedQuad> genQuads = MultimapBuilder.enumKeys(BlockRenderLayer.class).arrayListValues().build();
    protected final Table<BlockRenderLayer, EnumFacing, List<BakedQuad>> faceQuads = Tables.newCustomTable(Maps.newEnumMap(BlockRenderLayer.class), () -> Maps.newEnumMap(EnumFacing.class));

    @Override
    @SneakyThrows
    public @Nonnull List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {        
        if (CTMCoreMethods.renderingDamageModel.get()) {
            return parent.getQuads(state, side, rand);
        }
        
        IBakedModel parent = getParent(rand);

        ProfileUtil.start("ctm_models");
        
        AbstractCTMBakedModel baked = this;
        BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();

        if (Minecraft.getInstance().world != null && state instanceof CTMExtendedState) {
            ProfileUtil.start("state_creation");
            CTMExtendedState ext = (CTMExtendedState) state;
            RenderContextList ctxList = ext.getContextList(ext.getClean(), model);

            Object2LongMap<ICTMTexture<?>> serialized = ctxList.serialized();
            ProfileUtil.endAndStart("model_creation");
            baked = modelcache.get(new State(ext.getClean(), serialized, parent), () -> createModel(state, model, ctxList, rand));
            ProfileUtil.end();
        } else if (state != null)  {
            ProfileUtil.start("model_creation");
            baked = modelcache.get(new State(state, null, getParent(rand)), () -> createModel(state, model, null, rand));
            ProfileUtil.end();
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

    /**
     * Random sensitive parent, will proxy to {@link WeightedBakedModel} if possible.
     */
    @Nonnull
    public IBakedModel getParent(long rand) {
        if (getParent() instanceof WeightedBakedModel) {
            return ((WeightedBakedModel)parent).getRandomModel(rand);
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

    private static @Nonnull TRSRTransformation get(float tx, float ty, float tz, float ax, float ay, float az, float s) {
        return new TRSRTransformation(
            new Vector3f(tx / 16, ty / 16, tz / 16),
            TRSRTransformation.quatFromXYZDegrees(new Vector3f(ax, ay, az)),
            new Vector3f(s, s, s),
            null);
    }
        
    public static final Map<TransformType, TRSRTransformation> TRANSFORMS = ImmutableMap.<TransformType, TRSRTransformation>builder()
            .put(TransformType.GUI,                         get(0, 0, 0, 30, 45, 0, 0.625f))
            .put(TransformType.THIRD_PERSON_RIGHT_HAND,     get(0, 2.5f, 0, 75, 45, 0, 0.375f))
            .put(TransformType.THIRD_PERSON_LEFT_HAND,      get(0, 2.5f, 0, 75, 45, 0, 0.375f))
            .put(TransformType.FIRST_PERSON_RIGHT_HAND,     get(0, 0, 0, 0, 45, 0, 0.4f))
            .put(TransformType.FIRST_PERSON_LEFT_HAND,      get(0, 0, 0, 0, 225, 0, 0.4f))
            .put(TransformType.GROUND,                      get(0, 2, 0, 0, 0, 0, 0.25f))
            .put(TransformType.FIXED,                       get(0, 0, 0, 0, 0, 0, 0.5f))
            .build();
    
    public static final TRSRTransformation DEFAULT_TRANSFORM = get(0, 0, 0, 0, 0, 0, 1);

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        return Pair.of(this, TRANSFORMS.getOrDefault(cameraTransformType, DEFAULT_TRANSFORM).getMatrix());
    }
    
    protected static final BlockRenderLayer[] LAYERS = BlockRenderLayer.values();
    
    protected abstract AbstractCTMBakedModel createModel(IBlockState state, @Nonnull IModelCTM model, RenderContextList ctx, long rand);

}
