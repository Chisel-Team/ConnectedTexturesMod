package team.chisel.ctm.client.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MultimapBuilder;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.model.pipeline.BakedQuadBuilder;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import org.apache.commons.lang3.tuple.Pair;
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
        Vector2f uvs;
    }

    private static final TextureAtlasSprite BASE = Minecraft.getInstance().getAtlasSpriteGetter(AtlasTexture.LOCATION_BLOCKS_TEXTURE).apply(MissingTextureSprite.getLocation());
    
    @ToString
    public class UVs {
        
        @Getter
        private float minU, minV, maxU, maxV;
        
        @Getter
        private final TextureAtlasSprite sprite;
        
        private final Vector2f[] data;
        
        private UVs(Vector2f... data) {
            this(BASE, data);
        }
        
        private UVs(TextureAtlasSprite sprite, Vector2f... data) {
            this.data = data;
            this.sprite = sprite;
            
            float minU = Float.MAX_VALUE;
            float minV = Float.MAX_VALUE;
            float maxU = 0, maxV = 0;
            for (Vector2f v : data) {
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
                    new Vector2f(data[0].x == this.minU ? minU : maxU, data[0].y == this.minV ? minV : maxV), 
                    new Vector2f(data[1].x == this.minU ? minU : maxU, data[1].y == this.minV ? minV : maxV), 
                    new Vector2f(data[2].x == this.minU ? minU : maxU, data[2].y == this.minV ? minV : maxV), 
                    new Vector2f(data[3].x == this.minU ? minU : maxU, data[3].y == this.minV ? minV : maxV))
                    .relativize();
        }

        UVs normalizeQuadrant() {
            UVs normal = normalize();

            int quadrant = normal.getQuadrant();
            float minUInterp = quadrant == 1 || quadrant == 2 ? 0.5f : 0; 
            float minVInterp = quadrant < 2 ? 0.5f : 0; 
            float maxUInterp = quadrant == 0 || quadrant == 3 ? 0.5f : 1;
            float maxVInterp = quadrant > 1 ? 0.5f : 1;
            
            normal = new UVs(sprite, normalize(new Vector2f(minUInterp, minVInterp), new Vector2f(maxUInterp, maxVInterp), normal.vectorize()));
            return normal.relativize();
        }
        
        public UVs normalize() {
            Vector2f min = new Vector2f(sprite.getMinU(), sprite.getMinV());
            Vector2f max = new Vector2f(sprite.getMaxU(), sprite.getMaxV());
            return new UVs(sprite, normalize(min, max, data));
        }

        public UVs relativize() {
            return relativize(sprite);
        }

        public UVs relativize(TextureAtlasSprite sprite) {
            Vector2f min = new Vector2f(sprite.getMinU(), sprite.getMinV());
            Vector2f max = new Vector2f(sprite.getMaxU(), sprite.getMaxV());
            return new UVs(sprite, lerp(min, max, data));
        }

        @SuppressWarnings("null")
        public Vector2f[] vectorize() {
            return data == null ? new Vector2f[]{ new Vector2f(minU, minV), new Vector2f(minU, maxV), new Vector2f(maxU, maxV), new Vector2f(maxU, minV) } : data;
        }
        
        private Vector2f[] normalize(Vector2f min, Vector2f max, @NonnullType Vector2f... vecs) {
            Vector2f[] ret = new Vector2f[vecs.length];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = normalize(min, max, vecs[i]);
            }
            return ret;
        }
        
        private Vector2f normalize(Vector2f min, Vector2f max, Vector2f vec) {
            return new Vector2f(Quad.normalize(min.x, max.x, vec.x), Quad.normalize(min.y, max.y, vec.y));
        }
        
        private Vector2f[] lerp(Vector2f min, Vector2f max, @NonnullType Vector2f... vecs) {
            Vector2f[] ret = new Vector2f[vecs.length];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = lerp(min, max, vecs[i]);
            }
            return ret;
        }
        
        private Vector2f lerp(Vector2f min, Vector2f max, Vector2f vec) {
            return new Vector2f(Quad.lerp(min.x, max.x, vec.x), Quad.lerp(min.y, max.y, vec.y));
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
    private final Vector2f[] vertUv;
        
    // Technically nonfinal, but treated as such except in constructor
    @Getter
    private UVs uvs;
    
    private final Builder builder;

    private final int blocklight, skylight;
    
    private Quad(Vector3f[] verts, Vector2f[] uvs, Builder builder, TextureAtlasSprite sprite) {
        this(verts, uvs, builder, sprite, 0, 0);
    }

    @Deprecated
    private Quad(Vector3f[] verts, Vector2f[] uvs, Builder builder, TextureAtlasSprite sprite, boolean fullbright) {
        this(verts, uvs, builder, sprite, fullbright ? 15 : 0, fullbright ? 15 : 0);
    }
    
    private Quad(Vector3f[] verts, Vector2f[] uvs, Builder builder, TextureAtlasSprite sprite, int blocklight, int skylight) {
        this.vertPos = verts;
        this.vertUv = uvs;
        this.builder = builder;
        this.uvs = new UVs(sprite, uvs);
        this.blocklight = blocklight;
        this.skylight = skylight;
    }
    
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
        return new Quad(newverts, getUvs(), builder);
    }
    
    public Vector2f getUv(int index) {
    	return new Vector2f(vertUv[index % 4].x, vertUv[index % 4].y);
    }
    
    public Quad withUv(int index, Vector2f uv) {
        Preconditions.checkElementIndex(index, 4, "UV index out of range!");
        Vector2f[] newuvs = new Vector2f[4];
        System.arraycopy(getUvs().vectorize(), 0, newuvs, 0, newuvs.length);
        newuvs[index] = uv;
        return new Quad(vertPos, new UVs(newuvs), builder);
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
            
            firstQuad[i1].setX(lerp(firstQuad[i1].getX(), firstQuad[i2].getX(), f));
            firstQuad[i1].setY(lerp(firstQuad[i1].getY(), firstQuad[i2].getY(), f));
            firstQuad[i1].setZ(lerp(firstQuad[i1].getZ(), firstQuad[i2].getZ(), f));
            firstQuad[j1].setX(lerp(firstQuad[j1].getX(), firstQuad[j2].getX(), f));
            firstQuad[j1].setY(lerp(firstQuad[j1].getY(), firstQuad[j2].getY(), f));
            firstQuad[j1].setZ(lerp(firstQuad[j1].getZ(), firstQuad[j2].getZ(), f));
            
            secondQuad[i2].setX(lerp(secondQuad[i1].getX(), secondQuad[i2].getX(), f));
            secondQuad[i2].setY(lerp(secondQuad[i1].getY(), secondQuad[i2].getY(), f));
            secondQuad[i2].setZ(lerp(secondQuad[i1].getZ(), secondQuad[i2].getZ(), f));
            secondQuad[j2].setX(lerp(secondQuad[j1].getX(), secondQuad[j2].getX(), f));
            secondQuad[j2].setY(lerp(secondQuad[j1].getY(), secondQuad[j2].getY(), f));
            secondQuad[j2].setZ(lerp(secondQuad[j1].getZ(), secondQuad[j2].getZ(), f));

            Quad q1 = new Quad(firstQuad, first.relativize(), builder, blocklight, skylight);
            Quad q2 = new Quad(secondQuad, second.relativize(), builder, blocklight, skylight);
            return Pair.of(q1, q2);
        } else {
            return Pair.of(this, null);
        }
    }
    
    public static float lerp(float a, float b, float f) {
        float ret = (a * (1 - f)) + (b * f);
        return ret;
    }

    public static float normalize(float min, float max, float x) {
        float ret = (x - min) / (max - min);
        return ret;
    }
    
    public Quad rotate(int amount) {
        Vector2f[] uvs = new Vector2f[4];

        TextureAtlasSprite s = getUvs().getSprite();

        for (int i = 0; i < 4; i++) {
            Vector2f normalized = new Vector2f(normalize(s.getMinU(), s.getMaxU(), vertUv[i].x), normalize(s.getMinV(), s.getMaxV(), vertUv[i].y));
            Vector2f uv;
            switch (amount) {
            case 1:
                uv = new Vector2f(normalized.y, 1 - normalized.x);
                break;
            case 2:
                uv = new Vector2f(1 - normalized.x, 1 - normalized.y);
                break;
            case 3:
                uv = new Vector2f(1 - normalized.y, normalized.x);
                break;
            default:
                uv = new Vector2f(normalized.x, normalized.y);
                break;
            }
            uvs[i] = uv;
        }
        
        for (int i = 0; i < uvs.length; i++) {
            uvs[i] = new Vector2f(lerp(s.getMinU(), s.getMaxU(), uvs[i].x), lerp(s.getMinV(), s.getMaxV(), uvs[i].y));
        }

        Quad ret = new Quad(vertPos, uvs, builder, getUvs().getSprite(), blocklight, skylight);
        return ret;
    }

    public Quad derotate() {
        int start = 0;
        for (int i = 0; i < 4; i++) {
            if (vertUv[i].x <= getUvs().minU && vertUv[i].y <= getUvs().minV) {
                start = i;
                break;
            }
        }
        
        Vector2f[] uvs = new Vector2f[4];
        for (int i = 0; i < 4; i++) {
            uvs[i] = vertUv[(i + start) % 4];
        }
        return new Quad(vertPos, uvs, builder, getUvs().getSprite(), blocklight, skylight);
    }

    public Quad setLight(int blocklight, int skylight) {
        return new Quad(this.vertPos, uvs, builder, blocklight, skylight);
    }
    
    @SuppressWarnings("null")
    public BakedQuad rebake() {
        @Nonnull VertexFormat format = this.builder.vertexFormat;
        
        BakedQuadBuilder builder = new BakedQuadBuilder();
        builder.setQuadOrientation(this.builder.quadOrientation);
        builder.setQuadTint(this.builder.quadTint);
        builder.setApplyDiffuseLighting(this.builder.applyDiffuseLighting);
        builder.setTexture(this.uvs.getSprite());

        for (int v = 0; v < 4; v++) {
            for (int i = 0; i < format.getElements().size(); i++) {
                VertexFormatElement ele = format.getElements().get(i);
                switch (ele.getUsage()) {
                case POSITION:
                    Vector3f p = vertPos[v];
                    builder.put(i, p.getX(), p.getY(), p.getZ(), 1);
                    break;
                /*case COLOR:
                    builder.put(i, 35, 162, 204); Pretty things
                    break;*/
                case UV:
                    if (ele.getIndex() == 2) {
                        //Stuff for fullbright
                        builder.put(i, ((float) blocklight * 0x20) / 0xFFFF, ((float) skylight * 0x20) / 0xFFFF);
                        break;
                    } else if (ele.getIndex() == 0) {
                        Vector2f uv = vertUv[v];
                        builder.put(i, uv.x, uv.y);
                        break;
                    }
                    // fallthrough
                default:
                    builder.put(i, this.builder.data.get(ele).get(v));
                }
            }
        }

        return builder.build();
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
        Builder b = new Builder(DefaultVertexFormats.BLOCK, baked.func_187508_a());
        baked.pipe(b);
        return b.build();
    }
    
    @RequiredArgsConstructor
    public static class Builder implements IVertexConsumer {

        @Getter
        private final VertexFormat vertexFormat;
        @Getter
        private final TextureAtlasSprite sprite;

        @Setter
        private int quadTint = -1;

        @Setter
        private Direction quadOrientation;

        @Setter
        private boolean applyDiffuseLighting;
        
        private ListMultimap<VertexFormatElement, float[]> data = MultimapBuilder.hashKeys().arrayListValues().build();
        
        @Override
        public void put(int element, @Nullable float... data) {
            if (data == null) return;
            float[] copy = new float[data.length];
            System.arraycopy(data, 0, copy, 0, data.length);
            VertexFormatElement ele = vertexFormat.getElements().get(element);
            this.data.put(ele, copy);
        }
        
        public Quad build() {
            Vector3f[] verts = fromData(data.get(DefaultVertexFormats.POSITION_3F), 3); 
            Vector2f[] uvs = fromData(data.get(DefaultVertexFormats.TEX_2F), 2);
            return new Quad(verts, uvs, this, getSprite());
        }

        @SuppressWarnings("unchecked")
        private <T> T[] fromData(List<float[]> data, int size) {
            Object[] ret = size == 2 ? new Vector2f[data.size()] : new Vector3f[data.size()];
            for (int i = 0; i < data.size(); i++) {
                ret[i] = size == 2 ? new Vector2f(data.get(i)[0], data.get(i)[1]) : new Vector3f(data.get(i)[0], data.get(i)[1], data.get(i)[2]);
            }
            return (T[]) ret;
        }
        
        //@Override //soft override, only exists in new forge versions
        public void setTexture(@Nullable TextureAtlasSprite texture) {}
    }
}
