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

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.math.Vector3f;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.api.util.NonnullType;

@ParametersAreNonnullByDefault
@ToString(of = { "vertPos", "vertUv" })
public class Quad {
    
    @Deprecated
    public static final ISubmap TOP_LEFT = Submap.X2[0][0];
    @Deprecated
    public static final ISubmap TOP_RIGHT = Submap.X2[0][1];
    @Deprecated
    public static final ISubmap BOTTOM_LEFT = Submap.X2[1][0];
    @Deprecated
    public static final ISubmap BOTTOM_RIGHT = Submap.X2[1][1];
    
    @Value
    public static class Vertex {
        Vector3f pos;
        Vec2 uvs;
    }

    private static final TextureAtlasSprite BASE = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(MissingTextureAtlasSprite.getLocation());
    
    @ToString
    public class UVs {
        
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

        public UVs transform(TextureAtlasSprite other, ISubmap submap) {
            UVs normal = normalize();
            submap = submap.normalize();

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
    	return vertPos[index % 4].copy();
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

    public Quad[] subdivide(int count) {
        if (count == 1) {
            return new Quad[] { this };
        } else if (count != 4) {
            throw new UnsupportedOperationException();
        }
        
        List<Quad> rects = Lists.newArrayList();

        Pair<Quad, Quad> firstDivide = divide(false);
        Pair<Quad, Quad> secondDivide = firstDivide.getLeft().divide(true);
        rects.add(secondDivide.getLeft());

        if (firstDivide.getRight() != null) {
            Pair<Quad, Quad> thirdDivide = firstDivide.getRight().divide(true);
            rects.add(thirdDivide.getLeft());
            rects.add(thirdDivide.getRight());
        } else {
            rects.add(null);
            rects.add(null);
        }

        rects.add(secondDivide.getRight());

        return rects.toArray(new Quad[rects.size()]);
    }
    
    @SuppressWarnings("null")
    private Pair<@NonnullType Quad, Quad> divide(boolean vertical) {
        float min, max;
        UVs uvs = getUvs().normalize();
        if (vertical) {
            min = uvs.minV;
            max = uvs.maxV;
        } else {
            min = uvs.minU;
            max = uvs.maxU;
        }
        if (min < 0.5 && max > 0.5) {
            UVs first = new UVs(vertical ? uvs.minU : 0.5f, vertical ? 0.5f : uvs.minV, uvs.maxU, uvs.maxV, uvs.getSprite());
            UVs second = new UVs(uvs.minU, uvs.minV, vertical ? uvs.maxU : 0.5f, vertical ? 0.5f : uvs.maxV, uvs.getSprite());
                        
            int firstIndex = 0;
            for (int i = 0; i < vertUv.length; i++) {
                if (vertUv[i].y == getUvs().minV && vertUv[i].x == getUvs().minU) {
                    firstIndex = i;
                    break;
                }
            }
            
            float f = (0.5f - min) / (max - min);

            Vector3f[] firstQuad = new Vector3f[4];
            Vector3f[] secondQuad = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                int idx = (firstIndex + i) % 4;
                firstQuad[i] = vertPos[idx].copy();
                secondQuad[i] = vertPos[idx].copy();
            }
            
            int i1 = 0;
            int i2 = vertical ? 1 : 3;
            int j1 = vertical ? 3 : 1;
            int j2 = 2;
            
            firstQuad[i1].setX(lerp(firstQuad[i1].x(), firstQuad[i2].x(), f));
            firstQuad[i1].setY(lerp(firstQuad[i1].y(), firstQuad[i2].y(), f));
            firstQuad[i1].setZ(lerp(firstQuad[i1].z(), firstQuad[i2].z(), f));
            firstQuad[j1].setX(lerp(firstQuad[j1].x(), firstQuad[j2].x(), f));
            firstQuad[j1].setY(lerp(firstQuad[j1].y(), firstQuad[j2].y(), f));
            firstQuad[j1].setZ(lerp(firstQuad[j1].z(), firstQuad[j2].z(), f));
            
            secondQuad[i2].setX(lerp(secondQuad[i1].x(), secondQuad[i2].x(), f));
            secondQuad[i2].setY(lerp(secondQuad[i1].y(), secondQuad[i2].y(), f));
            secondQuad[i2].setZ(lerp(secondQuad[i1].z(), secondQuad[i2].z(), f));
            secondQuad[j2].setX(lerp(secondQuad[j1].x(), secondQuad[j2].x(), f));
            secondQuad[j2].setY(lerp(secondQuad[j1].y(), secondQuad[j2].y(), f));
            secondQuad[j2].setZ(lerp(secondQuad[j1].z(), secondQuad[j2].z(), f));

            Quad q1 = new Quad(firstQuad, first.relativize(), builder, blocklight, skylight);
            Quad q2 = new Quad(secondQuad, second.relativize(), builder, blocklight, skylight);
            return Pair.of(q1, q2);
        } else {
            return Pair.of(this, null);
        }
    }
    
    public static float lerp(float a, float b, float f) {
        return (a * (1 - f)) + (b * f);
    }

    public static float normalize(float min, float max, float x) {
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
        var quad = new BakedQuad[1];
        var builder = new QuadBakingVertexConsumer(q -> quad[0] = q);
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

        return quad[0];
    }
    
    public Quad transformUVs(TextureAtlasSprite sprite) {
        return transformUVs(sprite, CTMLogic.FULL_TEXTURE.normalize());
    }
    
    public Quad transformUVs(TextureAtlasSprite sprite, ISubmap submap) {
        return new Quad(vertPos, getUvs().transform(sprite, submap), builder, blocklight, skylight);
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
