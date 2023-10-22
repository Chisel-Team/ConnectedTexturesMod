package team.chisel.ctm.client.util;

import static net.minecraftforge.client.model.IQuadTransformer.COLOR;
import static net.minecraftforge.client.model.IQuadTransformer.POSITION;
import static net.minecraftforge.client.model.IQuadTransformer.STRIDE;
import static net.minecraftforge.client.model.IQuadTransformer.UV0;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.joml.Vector3f;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import net.minecraft.Util;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.api.util.NonnullType;

@ParametersAreNonnullByDefault
@ToString(of = { "vertPos", "vertUv" })
public class Quad {
    
    @Deprecated
    public static final ISubmap TOP_LEFT = Submap.fromPixelScale(7.8f, 7.8f, 0, 0);
    @Deprecated
    public static final ISubmap TOP_RIGHT = Submap.fromPixelScale(7.8f, 7.8f, 8.2f, 0);
    @Deprecated
    public static final ISubmap BOTTOM_LEFT = Submap.fromPixelScale(7.8f, 7.8f, 0, 8.2f);
    @Deprecated
    public static final ISubmap BOTTOM_RIGHT = Submap.fromPixelScale(7.8f, 7.8f, 8.2f, 8.2f);
    
    @Value
    public static class Vertex {
        Vector3f pos;
        Vec2 uvs;
    }

    private static final TextureAtlasSprite BASE = null;//Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(MissingTextureAtlasSprite.getLocation());
    
    @ToString
    public class UVs implements ISubmap {
        
        @Getter
        private float minU, minV, maxU, maxV;
        
        @Getter
        private final TextureAtlasSprite sprite;
        
        private final Vec2[] data;
        
        private UVs(Vec2... data) {
            this(BASE, data);
        }
        
        private UVs(TextureAtlasSprite sprite, Vec2... data) {
            this.data = data;
            this.sprite = sprite;
            
            float minU = Float.MAX_VALUE;
            float minV = Float.MAX_VALUE;
            float maxU = 0, maxV = 0;
            for (Vec2 v : data) {
                minU = Math.min(minU, v.x);
                minV = Math.min(minV, v.y);
                maxU = Math.max(maxU, v.x);
                maxV = Math.max(maxV, v.y);
            }
            this.minU = minU;
            this.minV = minV;
            this.maxU = maxU;
            this.maxV = maxV;
        }

        public UVs(float minU, float minV, float maxU, float maxV, TextureAtlasSprite sprite) {
            this.minU = minU;
            this.minV = minV;
            this.maxU = maxU;
            this.maxV = maxV;
            this.sprite = sprite;
            this.data = vectorize();
        }

        public UVs(ISubmap submap, TextureAtlasSprite sprite) {
            this(submap.getXOffset(), submap.getYOffset(), submap.getXOffset() + submap.getWidth(), submap.getYOffset() + submap.getHeight(), sprite);
        }

        public UVs transform(TextureAtlasSprite other, ISubmap submap) {
            UVs normal = normalize();
            submap = submap.unitScale();

            float width = normal.maxU - normal.minU;
            float height = normal.maxV - normal.minV;

            float minU = submap.getXOffset();
            float minV = submap.getYOffset();
            minU += normal.minU * submap.getWidth();
            minV += normal.minV * submap.getHeight();

            float maxU = minU + (width * submap.getWidth());
            float maxV = minV + (height * submap.getHeight());

            // TODO this is horrid
            return new UVs(other, 
                    new Vec2(data[0].x == this.minU ? minU : maxU, data[0].y == this.minV ? minV : maxV), 
                    new Vec2(data[1].x == this.minU ? minU : maxU, data[1].y == this.minV ? minV : maxV), 
                    new Vec2(data[2].x == this.minU ? minU : maxU, data[2].y == this.minV ? minV : maxV), 
                    new Vec2(data[3].x == this.minU ? minU : maxU, data[3].y == this.minV ? minV : maxV))
                    .relativize();
        }

        UVs normalizeQuadrant() {
            UVs normal = normalize();

            int quadrant = normal.getQuadrant();
            float minUInterp = quadrant == 1 || quadrant == 2 ? 0.5f : 0; 
            float minVInterp = quadrant < 2 ? 0.5f : 0; 
            float maxUInterp = quadrant == 0 || quadrant == 3 ? 0.5f : 1;
            float maxVInterp = quadrant > 1 ? 0.5f : 1;
            
            normal = new UVs(sprite, normalize(new Vec2(minUInterp, minVInterp), new Vec2(maxUInterp, maxVInterp), normal.vectorize()));
            return normal.relativize();
        }
        
        public UVs normalize() {
            Vec2 min = new Vec2(sprite.getU0(), sprite.getV0());
            Vec2 max = new Vec2(sprite.getU1(), sprite.getV1());
            return new UVs(sprite, normalize(min, max, data));
        }

        public UVs relativize() {
            return relativize(sprite);
        }

        public UVs relativize(TextureAtlasSprite sprite) {
            Vec2 min = new Vec2(sprite.getU0(), sprite.getV0());
            Vec2 max = new Vec2(sprite.getU1(), sprite.getV1());
            return new UVs(sprite, lerp(min, max, data));
        }

        @SuppressWarnings("null")
        public Vec2[] vectorize() {
            return data == null ? new Vec2[]{ new Vec2(minU, minV), new Vec2(minU, maxV), new Vec2(maxU, maxV), new Vec2(maxU, minV) } : data;
        }
        
        private Vec2[] normalize(Vec2 min, Vec2 max, @NonnullType Vec2... vecs) {
            Vec2[] ret = new Vec2[vecs.length];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = normalize(min, max, vecs[i]);
            }
            return ret;
        }
        
        private Vec2 normalize(Vec2 min, Vec2 max, Vec2 vec) {
            return new Vec2(Quad.normalize(min.x, max.x, vec.x), Quad.normalize(min.y, max.y, vec.y));
        }
        
        private Vec2[] lerp(Vec2 min, Vec2 max, @NonnullType Vec2... vecs) {
            Vec2[] ret = new Vec2[vecs.length];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = lerp(min, max, vecs[i]);
            }
            return ret;
        }
        
        private Vec2 lerp(Vec2 min, Vec2 max, Vec2 vec) {
            return new Vec2(Quad.lerp(min.x, max.x, vec.x), Quad.lerp(min.y, max.y, vec.y));
        }
        
        public int getQuadrant() {
            if (maxU <= 0.5f) {
                if (maxV <= 0.5f) {
                    return 3;
                } else {
                    return 0;
                }
            } else {
                if (maxV <= 0.5f) {
                    return 2;
                } else {
                    return 1;
                }
            }
        }

        @Override
        public float getYOffset() {
            return minV;
        }

        @Override
        public float getXOffset() {
            return minU;
        }

        @Override
        public float getWidth() {
            return maxU - minU;
        }

        @Override
        public float getHeight() {
            return maxV - minV;
        }
        
        @Override
        public ISubmap unitScale() {
            return this;
        }
        
        @Override
        public ISubmap pixelScale() {
             return new SubmapRescaled(this, PIXELS_PER_UNIT, true);
        }
    }

    private final Vector3f[] vertPos;
    private final Vec2[] vertUv;
        
    // Technically nonfinal, but treated as such except in constructor
    @Getter
    private UVs uvs;
    
    private final Builder builder;

    private final int blocklight, skylight;
    
    private Quad(Vector3f[] verts, Vec2[] uvs, Builder builder, TextureAtlasSprite sprite) {
        this(verts, uvs, builder, sprite, 0, 0);
    }

    @Deprecated
    private Quad(Vector3f[] verts, Vec2[] uvs, Builder builder, TextureAtlasSprite sprite, boolean fullbright) {
        this(verts, uvs, builder, sprite, fullbright ? 15 : 0, fullbright ? 15 : 0);
    }
    
    private Quad(Vector3f[] verts, Vec2[] uvs, Builder builder, TextureAtlasSprite sprite, int blocklight, int skylight) {
        this.vertPos = verts;
        this.vertUv = uvs;
        this.builder = builder;
        this.uvs = new UVs(sprite, uvs);
        this.blocklight = blocklight;
        this.skylight = skylight;
    }
    
    @Deprecated
    private Quad(Vector3f[] verts, UVs uvs, Builder builder) {
        this(verts, uvs.vectorize(), builder, uvs.getSprite());
    }

    @Deprecated
    private Quad(Vector3f[] verts, UVs uvs, Builder builder, boolean fullbright) {
        this(verts, uvs.vectorize(), builder, uvs.getSprite(), fullbright);
    }
    
    private Quad(Vector3f[] verts, UVs uvs, Builder builder, int blocklight, int skylight) {
        this(verts, uvs.vectorize(), builder, uvs.getSprite(), blocklight, skylight);
    }
    
    public Vector3f getVert(int index) {
    	return new Vector3f(vertPos[index % 4]);
    }
    
    public Quad withVert(int index, Vector3f vert) {
        Preconditions.checkElementIndex(index, 4, "Vertex index out of range!");
        Vector3f[] newverts = new Vector3f[4];
        System.arraycopy(vertPos, 0, newverts, 0, newverts.length);
        newverts[index] = vert;
        return new Quad(newverts, getUvs(), builder, blocklight, skylight);
    }
    
    public Vec2 getUv(int index) {
    	return new Vec2(vertUv[index % 4].x, vertUv[index % 4].y);
    }
    
    public Quad withUv(int index, Vec2 uv) {
        Preconditions.checkElementIndex(index, 4, "UV index out of range!");
        Vec2[] newuvs = new Vec2[4];
        System.arraycopy(getUvs().vectorize(), 0, newuvs, 0, newuvs.length);
        newuvs[index] = uv;
        return new Quad(vertPos, new UVs(newuvs), builder, blocklight, skylight);
    }

    public void compute() {

    }

    @Deprecated
    public Quad[] subdivide(int count) {
        if (count == 1) {
            return new Quad[] { this };
        } else if (count != 4) {
            throw new UnsupportedOperationException();
        }
        
        return subsectAll(Submap.X2);
//        return subsectAll(new ISubmap[] { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT });
    }
    
    public Quad[] subsectAll(ISubmap[][] submaps) {
        var stride = submaps[0].length;
        var ret = new Quad[submaps.length * stride];
        for (int i = 0; i < submaps.length; i++) {
            System.arraycopy(subsectAll(submaps[i]), 0, ret, i * stride, stride);
        }
        return ret;
    }

    public Quad[] subsectAll(ISubmap[] submaps) {
        var ret = new Quad[submaps.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = subsect(submaps[i]);
        }
        return ret;
    }

    public Quad subsect(ISubmap submap) {

        int firstIndex = 0;
        for (int i = 0; i < vertUv.length; i++) {
            if (vertUv[i].y == getUvs().minV && vertUv[i].x == getUvs().minU) {
                firstIndex = i;
                break;
            }
        }
    
        Vector3f[] positions = new Vector3f[4];
        float[][] uvs = new float[4][];
        for (int i = 0; i < 4; i++) {
            int idx = (firstIndex + i) % 4;
            positions[i] = new Vector3f(vertPos[idx]);
            uvs[i] = new float[] { vertUv[idx].x, vertUv[idx].y };
        }
        
        var origin = new Vec3(positions[0]);
        var n1 = new Vec3(positions[1]).subtract(origin);
        var n2 = new Vec3(positions[2]).subtract(origin);
        var normalVec = n1.cross(n2).normalize();
        Direction normal = Direction.fromDelta((int) normalVec.x, (int) normalVec.y, (int) normalVec.z);
        TextureAtlasSprite sprite = getUvs().getSprite();
        
        var xy = new float[4][2];
        var newXy = new float[4][2];
        for (int i = 0; i < 4; i++) {
            switch (normal.getAxis()) {
                case Y -> {
                    xy[i][0] = positions[i].x;
                    xy[i][1] = positions[i].z;
                }
                case Z -> {
                    xy[i][0] = positions[i].x;
                    xy[i][1] = positions[i].y;
                }
                case X -> {
                    xy[i][0] = positions[i].z;
                    xy[i][1] = positions[i].y;
                }
            }
        }
        
        if (normal.getAxis() != Axis.Y) {
            submap = submap.flipY();
        }
        if (normal == Direction.EAST || normal == Direction.NORTH) {
            submap = submap.flipX();
        }
        
        submap = submap.unitScale();
        
        if (normal.getAxis() == Axis.Y || normal == Direction.SOUTH || normal == Direction.WEST) {
            // Relative X is the same sign for DOWN, UP, SOUTH, and WEST
            newXy[0][0] = Math.max(xy[0][0], submap.getXOffset());                      // DUSW
            newXy[1][0] = Math.max(xy[1][0], submap.getXOffset());                      // DUSW
            newXy[2][0] = Math.min(xy[2][0], submap.getXOffset() + submap.getWidth());  // DUSW
            newXy[3][0] = Math.min(xy[3][0], submap.getXOffset() + submap.getWidth());  // DUSW
        } else {
            // Flip relative X for NORTH and EAST
            newXy[0][0] = Math.min(xy[0][0], submap.getXOffset() + submap.getWidth());  // NE
            newXy[1][0] = Math.min(xy[1][0], submap.getXOffset() + submap.getWidth());  // NE
            newXy[2][0] = Math.max(xy[2][0], submap.getXOffset());                      // NE
            newXy[3][0] = Math.max(xy[3][0], submap.getXOffset());                      // NE
        }
        
        if (normal != Direction.UP) {
            // Relative Y is the same sign for all but UP
            newXy[0][1] = Math.min(xy[0][1], submap.getYOffset() + submap.getHeight()); // DNSWE
            newXy[1][1] = Math.max(xy[1][1], submap.getYOffset());                      // DNSWE
            newXy[2][1] = Math.max(xy[2][1], submap.getYOffset());                      // DNSWE
            newXy[3][1] = Math.min(xy[3][1], submap.getYOffset() + submap.getHeight()); // DNSWE
        } else {
            // Flip relative Y for UP
            newXy[0][1] = Math.max(xy[0][1], submap.getYOffset());                      // U
            newXy[1][1] = Math.min(xy[1][1], submap.getYOffset() + submap.getHeight()); // U
            newXy[2][1] = Math.min(xy[2][1], submap.getYOffset() + submap.getHeight()); // U
            newXy[3][1] = Math.max(xy[3][1], submap.getYOffset());                      // U
        }
        
        float u0Interp = normalize(xy[0][0], xy[3][0], newXy[0][0]);
        float v0Interp = normalize(xy[0][1], xy[1][1], newXy[0][1]);
        float u1Interp = normalize(xy[1][0], xy[2][0], newXy[1][0]);
        float v1Interp = normalize(xy[1][1], xy[0][1], newXy[1][1]);
        float u2Interp = normalize(xy[2][0], xy[1][0], newXy[2][0]);
        float v2Interp = normalize(xy[2][1], xy[3][1], newXy[2][1]);
        float u3Interp = normalize(xy[3][0], xy[0][0], newXy[3][0]);
        float v3Interp = normalize(xy[3][1], xy[2][1], newXy[3][1]);
    
        float u0 = lerp(uvs[0][0], uvs[3][0], u0Interp);
        float v0 = lerp(uvs[0][1], uvs[1][1], v0Interp);
        float u1 = lerp(uvs[1][0], uvs[2][0], u1Interp);
        float v1 = lerp(uvs[1][1], uvs[0][1], v1Interp);
        float u2 = lerp(uvs[2][0], uvs[1][0], u2Interp);
        float v2 = lerp(uvs[2][1], uvs[3][1], v2Interp);
        float u3 = lerp(uvs[3][0], uvs[0][0], u3Interp);
        float v3 = lerp(uvs[3][1], uvs[2][1], v3Interp);
    
        var newUvs = new UVs(sprite, new Vec2(u0, v0), new Vec2(u1, v1), new Vec2(u2, v2), new Vec2(u3, v3));
        
        Vector3f[] newPos = new Vector3f[4];
        for (int i = 0; i < 4; i++) {
            newPos[i] = new Vector3f(positions[i]);
            switch (normal.getAxis()) {
                case Y -> {
                    newPos[i].x = newXy[i][0];
                    newPos[i].z = newXy[i][1];
                }
                case Z -> {
                    newPos[i].x = newXy[i][0];
                    newPos[i].y = newXy[i][1];
                }
                case X -> {
                    newPos[i].z = newXy[i][0];
                    newPos[i].y = newXy[i][1];
                }
            }
        }
        
        return new Quad(newPos, newUvs, builder, blocklight, skylight);
    }

    public static float lerp(float a, float b, float f) {
        return (a * (1 - f)) + (b * f);
    }

    public static float normalize(float min, float max, float x) {
        if (min == max) return 0.5f;
        return (x - min) / (max - min);
    }
    
    public Quad rotate(int amount) {
        Vec2[] uvs = new Vec2[4];

        TextureAtlasSprite s = getUvs().getSprite();

        for (int i = 0; i < 4; i++) {
            Vec2 normalized = new Vec2(normalize(s.getU0(), s.getU1(), vertUv[i].x), normalize(s.getV0(), s.getV1(), vertUv[i].y));
            Vec2 uv;
            switch (amount) {
            case 1:
                uv = new Vec2(normalized.y, 1 - normalized.x);
                break;
            case 2:
                uv = new Vec2(1 - normalized.x, 1 - normalized.y);
                break;
            case 3:
                uv = new Vec2(1 - normalized.y, normalized.x);
                break;
            default:
                uv = new Vec2(normalized.x, normalized.y);
                break;
            }
            uvs[i] = uv;
        }
        
        for (int i = 0; i < uvs.length; i++) {
            uvs[i] = new Vec2(lerp(s.getU0(), s.getU1(), uvs[i].x), lerp(s.getV0(), s.getV1(), uvs[i].y));
        }

        return new Quad(vertPos, uvs, builder, getUvs().getSprite(), blocklight, skylight);
    }

    public Quad derotate() {
        int start = 0;
        for (int i = 0; i < 4; i++) {
            if (vertUv[i].x <= getUvs().minU && vertUv[i].y <= getUvs().minV) {
                start = i;
                break;
            }
        }
        
        Vec2[] uvs = new Vec2[4];
        for (int i = 0; i < 4; i++) {
            uvs[i] = vertUv[(i + start) % 4];
        }
        return new Quad(vertPos, uvs, builder, getUvs().getSprite(), blocklight, skylight);
    }

    public Quad setLight(int blocklight, int skylight) {
        return new Quad(this.vertPos, uvs, builder, Math.max(this.blocklight, blocklight), Math.max(this.skylight, skylight));
    }
    
    @SuppressWarnings("null")
    public BakedQuad rebake() {
        var builder = new QuadBakingVertexConsumer.Buffered();
        builder.setDirection(this.builder.quadOrientation);
        builder.setTintIndex(this.builder.quadTint);
        builder.setShade(this.builder.applyDiffuseLighting);
        builder.setHasAmbientOcclusion(this.builder.applyAmbientOcclusion);
        builder.setSprite(this.uvs.getSprite());
        var format = DefaultVertexFormat.BLOCK;
        
        for (int v = 0; v < 4; v++) {
            for (int i = 0; i < format.getElements().size(); i++) {
                VertexFormatElement ele = format.getElements().get(i);
                switch (ele.getUsage()) {
                case POSITION:
                    Vector3f p = vertPos[v];
                    builder.vertex(p.x(), p.y(), p.z());
                    break;
                /*case COLOR:
                    builder.put(i, 35, 162, 204); Pretty things
                    break;*/
                case UV:
                    if (ele.getIndex() == 2) {
                        //Stuff for fullbright
                        builder.uv2(blocklight * 0x10, skylight * 0x10);
                        break;
                    } else if (ele.getIndex() == 0) {
                        Vec2 uv = vertUv[v];
                        builder.uv(uv.x, uv.y);
                        break;
                    }
                    // fallthrough
                default:
                    builder.misc(ele, this.builder.packedByElement.get(ele)[v]);
                }
            }
            builder.endVertex();
        }

        return builder.getQuad();
    }
    
    public Quad transformUVs(TextureAtlasSprite sprite) {
        return transformUVs(sprite, CTMLogic.FULL_TEXTURE.pixelScale());
    }
    
    public Quad transformUVs(TextureAtlasSprite sprite, ISubmap submap) {
        return new Quad(vertPos, getUvs().transform(sprite, submap), builder, blocklight, skylight);
    }
    
    public Quad setUVs(TextureAtlasSprite sprite, ISubmap submap) {
        return new Quad(vertPos, sample(sprite, submap), builder, blocklight, skylight);
    }
    
    private UVs sample(TextureAtlasSprite sprite, ISubmap submap) { 
        submap = submap.unitScale();
        float width = sprite.getU1() - sprite.getU0();
        float height = sprite.getV1() - sprite.getV0();
        return new UVs(Submap.raw(
                width * submap.getWidth(), height * submap.getHeight(),
                lerp(sprite.getU0(), sprite.getU1(), submap.getXOffset()),
                lerp(sprite.getV0(), sprite.getV1(), submap.getYOffset())), sprite);
    }

    public Quad grow() {
        return new Quad(vertPos, getUvs().normalizeQuadrant(), builder, blocklight, skylight);
    }

    @Deprecated
    public Quad setFullbright(boolean fullbright){
        if (this.blocklight == 15 != fullbright || this.skylight == 15 != fullbright) {
            return new Quad(vertPos, getUvs(), builder, fullbright);
        } else {
            return this;
        }
    }
    
    public static Quad from(BakedQuad baked) {
        Builder b = new Builder(baked.getSprite());
        b.copyFrom(baked);
        return b.build();
    }
    
    @RequiredArgsConstructor
    public static class Builder {
        
        private final Map<VertexFormatElement, Integer> ELEMENT_OFFSETS = Util.make(new IdentityHashMap<>(), map -> {
            int i = 0;
            for (var element : DefaultVertexFormat.BLOCK.getElements())
                map.put(element, DefaultVertexFormat.BLOCK.getOffset(i++) / 4); // Int offset
        });
        
        @Getter
        private final TextureAtlasSprite sprite;

        @Setter
        private int quadTint = -1;

        @Setter
        private Direction quadOrientation;

        @Setter
        private boolean applyDiffuseLighting;

        @Setter
        private boolean applyAmbientOcclusion;
        
        private final float[][] positions = new float[4][];
        private final float[][] uvs = new float[4][];
        private final int[][] colors = new int[4][];
        
        private Map<VertexFormatElement, int[][]> packedByElement = new HashMap<>();
                
        public void copyFrom(BakedQuad baked) {
            setQuadTint(baked.getTintIndex());
            setQuadOrientation(baked.getDirection());
            setApplyDiffuseLighting(baked.isShade());
            setApplyAmbientOcclusion(baked.hasAmbientOcclusion());
            var vertices = baked.getVertices();
            for (int i = 0; i < 4; i++) {
                int offset = i * STRIDE;
                this.positions[i] = new float[] {
                    Float.intBitsToFloat(vertices[offset + POSITION]),
                    Float.intBitsToFloat(vertices[offset + POSITION + 1]),
                    Float.intBitsToFloat(vertices[offset + POSITION + 2]),
                    0
                };
                int packedColor = vertices[offset + COLOR];
                this.colors[i] = new int[] {
                    packedColor & 0xFF,
                    (packedColor << 8) & 0xFF,
                    (packedColor << 16) & 0xFF,
                    (packedColor << 24) & 0xFF
                };
                this.uvs[i] = new float[] {
                   Float.intBitsToFloat(vertices[offset + UV0]),
                   Float.intBitsToFloat(vertices[offset + UV0 + 1])
                };
            }
            for (var e : ELEMENT_OFFSETS.entrySet()) {
                var offset = e.getValue();
                int[][] data = new int[4][e.getKey().getByteSize() / 4];
                for (int v = 0; v < 4; v++) {
                    for (int i = 0; i < data[v].length; i++) {
                        data[v][i] = vertices[v * STRIDE + offset + i];
                    }
                }
                this.packedByElement.put(e.getKey(), data);//new int[] { vertices[0 * STRIDE + offset], vertices[1 * STRIDE + offset], vertices[2 * STRIDE + offset], vertices[3 * STRIDE + offset]});
            }
        }

        public Quad build() {
            Vector3f[] verts = new Vector3f[4];
            Vec2[] uvs = new Vec2[4];
            for (int i = 0; i < verts.length; i++) {
                verts[i] = new Vector3f(this.positions[i][0], this.positions[i][1], this.positions[i][2]);
                uvs[i] = new Vec2(this.uvs[i][0], this.uvs[i][1]);
            }
            // TODO need to extract light data here
            return new Quad(verts, uvs, this, getSprite());
        }

        @SuppressWarnings("unchecked")
        private <T> T[] fromData(List<float[]> data, int size) {
            Object[] ret = size == 2 ? new Vec2[data.size()] : new Vector3f[data.size()];
            for (int i = 0; i < data.size(); i++) {
                ret[i] = size == 2 ? new Vec2(data.get(i)[0], data.get(i)[1]) : new Vector3f(data.get(i)[0], data.get(i)[1], data.get(i)[2]);
            }
            return (T[]) ret;
        }
        
        //@Override //soft override, only exists in new forge versions
        public void setTexture(@Nullable TextureAtlasSprite texture) {}
    }
}
