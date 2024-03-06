package team.chisel.ctm.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.Arrays;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.api.util.NonnullType;

@ParametersAreNonnullByDefault
@ToString(of = { "vertices"})
public class Quad {
    
    @Deprecated
    public static final ISubmap TOP_LEFT = Submap.fromPixelScale(7.8f, 7.8f, 0, 0);
    @Deprecated
    public static final ISubmap TOP_RIGHT = Submap.fromPixelScale(7.8f, 7.8f, 8.2f, 0);
    @Deprecated
    public static final ISubmap BOTTOM_LEFT = Submap.fromPixelScale(7.8f, 7.8f, 0, 8.2f);
    @Deprecated
    public static final ISubmap BOTTOM_RIGHT = Submap.fromPixelScale(7.8f, 7.8f, 8.2f, 8.2f);

    public record Vertex(Vector3f pos, Vec2 uvs) {
    }

    @ToString
    public static class UVs implements ISubmap {

        @Getter
        private float minU, minV, maxU, maxV;
        
        @Getter
        private final TextureAtlasSprite sprite;
        
        private final Vec2[] data;

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
                return maxV <= 0.5f ? 3 : 0;
            }
            return maxV <= 0.5f ? 2 : 1;
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

    private final VertexData[] vertices;

    // Technically nonfinal, but treated as such except in constructor
    @Getter
    private UVs uvs;
    
    private final Builder builder;

    private Quad(VertexData[] vertices, Builder builder, TextureAtlasSprite sprite) {
        this.vertices = vertices;
        this.builder = builder;
        Vec2[] uvs = new Vec2[this.vertices.length];
        for (int i = 0; i < uvs.length; i++) {
            uvs[i] = this.vertices[i].getUV();
        }
        this.uvs = new UVs(sprite, uvs);
    }

    private VertexData[] copyVertices() {
        VertexData[] verticesCopy = new VertexData[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            //Note: As we know all our vertices are made from unpacking quads we can just do a shallow copy instead of
            // having to create a full copy of any misc data we may have
            verticesCopy[i] = vertices[i].copy(false);
        }
        return verticesCopy;
    }
    
    public Vector3f getVert(int index) {
        VertexData vertex = vertices[index % vertices.length];
        return new Vector3f((float) vertex.getPosX(), (float) vertex.getPosY(), (float) vertex.getPosZ());
    }
    
    public Quad withVert(int index, Vector3f vert) {
        Preconditions.checkElementIndex(index, vertices.length, "Vertex index out of range!");
        VertexData[] newVertices = copyVertices();
        newVertices[index].pos(vert.x, vert.y, vert.z);
        return new Quad(newVertices, builder, getUvs().getSprite());
    }
    
    public Vec2 getUv(int index) {
        return vertices[index % vertices.length].getUV();
    }
    
    public Quad withUv(int index, Vec2 uv) {
        Preconditions.checkElementIndex(index, vertices.length, "UV index out of range!");
        VertexData[] newVertices = copyVertices();
        newVertices[index].texRaw(uv.x, uv.y);
        return new Quad(newVertices, builder, getUvs().getSprite());
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
        for (int i = 0; i < vertices.length; i++) {
            VertexData vertex = vertices[i];
            if (vertex.getTexV() == getUvs().minV && vertex.getTexU() == getUvs().minU) {
                firstIndex = i;
                break;
            }
        }

        Vec3[] positions = new Vec3[vertices.length];
        float[][] uvs = new float[vertices.length][];
        for (int i = 0; i < vertices.length; i++) {
            int idx = (firstIndex + i) % vertices.length;
            VertexData vertex = vertices[idx];
            positions[i] = vertex.getPos();
            uvs[i] = new float[] { vertex.getTexU(), vertex.getTexV() };
        }

        var origin = positions[0];
        var n1 = positions[1].subtract(origin);
        var n2 = positions[2].subtract(origin);
        var normalVec = n1.cross(n2).normalize();
        Direction normal = Direction.fromDelta((int) normalVec.x, (int) normalVec.y, (int) normalVec.z);
        TextureAtlasSprite sprite = getUvs().getSprite();

        var xy = new double[positions.length][2];
        var newXy = new double[positions.length][2];
        for (int i = 0; i < positions.length; i++) {
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

        VertexData[] newVertices = copyVertices();
        newVertices[0].texRaw(lerp(uvs[0][0], uvs[3][0], u0Interp), lerp(uvs[0][1], uvs[1][1], v0Interp));
        newVertices[1].texRaw(lerp(uvs[1][0], uvs[2][0], u1Interp), lerp(uvs[1][1], uvs[0][1], v1Interp));
        newVertices[2].texRaw(lerp(uvs[2][0], uvs[1][0], u2Interp), lerp(uvs[2][1], uvs[3][1], v2Interp));
        newVertices[3].texRaw(lerp(uvs[3][0], uvs[0][0], u3Interp),  lerp(uvs[3][1], uvs[2][1], v3Interp));

        for (int i = 0; i < newVertices.length; i++) {
            VertexData newVertex = newVertices[i];
            switch (normal.getAxis()) {
                case Y -> newVertex.pos(newXy[i][0], newVertex.getPosY(), newXy[i][1]);
                case Z -> newVertex.pos(newXy[i][0], newXy[i][1], newVertex.getPosZ());
                case X -> newVertex.pos(newVertex.getPosX(), newXy[i][1], newXy[i][0]);
            }
        }

        return new Quad(newVertices, builder, sprite);
    }

    public static float lerp(float a, float b, float f) {
        return (a * (1 - f)) + (b * f);
    }

    public static float normalize(float min, float max, float x) {
        if (min == max) return 0.5f;
        return Mth.inverseLerp(x, min, max);
    }

    private static float normalize(double min, double max, double x) {
        if (min == max) return 0.5f;
        return (float) Mth.inverseLerp(x, min, max);
    }

    public Quad rotate(int amount) {
        VertexData[] vertexCopy = copyVertices();
        TextureAtlasSprite s = getUvs().getSprite();
        for (VertexData vertex : vertexCopy) {
            Vec2 normalized = new Vec2(normalize(s.getU0(), s.getU1(), vertex.getTexU()), normalize(s.getV0(), s.getV1(), vertex.getTexV()));
            switch (amount) {
                case 1 -> vertex.texRaw(normalized.y, 1 - normalized.x);
                case 2 -> vertex.texRaw(1 - normalized.x, 1 - normalized.y);
                case 3 -> vertex.texRaw(1 - normalized.y, normalized.x);
                default -> vertex.texRaw(normalized.x, normalized.y);
            }
            vertex.texRaw(lerp(s.getU0(), s.getU1(), vertex.getTexU()), lerp(s.getV0(), s.getV1(), vertex.getTexV()));
        }
        return new Quad(vertexCopy, builder, s);
    }

    public Quad derotate() {
        int start = 0;
        for (int i = 0; i < vertices.length; i++) {
            VertexData vertex = vertices[i];
            if (vertex.getTexU() <= getUvs().minU && vertex.getTexV() <= getUvs().minV) {
                start = i;
                break;
            }
        }

        VertexData[] vertexCopy = copyVertices();
        for (int i = 0; i < vertices.length; i++) {
            VertexData vertex = vertices[(i + start) % vertices.length];
            vertexCopy[i].texRaw(vertex.getTexU(), vertex.getTexV());
        }
        return new Quad(vertexCopy, builder, getUvs().getSprite());
    }

    public Quad setLight(int blockLight, int skyLight) {
        VertexData[] vertexCopy = copyVertices();
        for (VertexData vertexData : vertexCopy) {
            //Only increase the light of the vertex, never decrease it if it is already natively emissive
            vertexData.light(Math.max(vertexData.getBlockLight(), blockLight), Math.max(vertexData.getSkyLight(), skyLight));
        }
        return new Quad(vertexCopy, builder, getUvs().getSprite());
    }
    
    @SuppressWarnings("null")
    public BakedQuad rebake() {
        var builder = new QuadBakingVertexConsumer.Buffered();
        builder.setDirection(this.builder.quadOrientation);
        builder.setTintIndex(this.builder.quadTint);
        builder.setShade(this.builder.applyDiffuseLighting);
        builder.setHasAmbientOcclusion(this.builder.applyAmbientOcclusion);
        builder.setSprite(this.uvs.getSprite());
        for (VertexData vertex : vertices) {
            vertex.write(builder);
        }

        return builder.getQuad();
    }
    
    public Quad transformUVs(TextureAtlasSprite sprite) {
        return transformUVs(sprite, CTMLogic.FULL_TEXTURE.pixelScale());
    }
    
    public Quad transformUVs(TextureAtlasSprite sprite, ISubmap submap) {
        return withUVs(getUvs().transform(sprite, submap));
    }
    
    public Quad setUVs(TextureAtlasSprite sprite, ISubmap submap) {
        return withUVs(sample(sprite, submap));
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
        return withUVs(getUvs().normalizeQuadrant());
    }

    private Quad withUVs(UVs uvs) {
        VertexData[] vertexCopy = copyVertices();
        Vec2[] vectorizedUVs = uvs.vectorize();
        for (int i = 0; i < vertexCopy.length; i++) {
            vertexCopy[i].texRaw(vectorizedUVs[i].x, vectorizedUVs[i].y);
        }
        return new Quad(vertexCopy, builder, uvs.getSprite());
    }

    @Deprecated
    public Quad setFullbright(boolean fullbright) {
        return fullbright ? setLight(15, 15) : this;
    }
    
    public static Quad from(BakedQuad baked) {
        Builder b = new Builder(baked.getSprite());
        b.copyFrom(baked);
        return b.build();
    }
    
    @RequiredArgsConstructor
    public static class Builder implements VertexConsumer {

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

        private final VertexData[] vertices = new VertexData[4];
        private VertexData vertex = new VertexData();
        private int vertexIndex = 0;

        public void copyFrom(BakedQuad baked) {
            setQuadTint(baked.getTintIndex());
            setQuadOrientation(baked.getDirection());
            setApplyDiffuseLighting(baked.isShade());
            setApplyAmbientOcclusion(baked.hasAmbientOcclusion());
            putBulkData(new PoseStack().last(), baked, 1, 1, 1, 1, 0, OverlayTexture.NO_OVERLAY, true);
        }

        public Quad build() {
            return new Quad(vertices, this, getSprite());
        }

        @NotNull
        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            vertex.pos(x, y, z);
            return this;
        }

        @NotNull
        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            vertex.color(red, green, blue, alpha);
            return this;
        }

        @NotNull
        @Override
        public VertexConsumer uv(float u, float v) {
            vertex.texRaw(u, v);
            return this;
        }

        @NotNull
        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            vertex.overlay(u, v);
            return this;
        }

        @NotNull
        @Override
        public VertexConsumer uv2(int u, int v) {
            vertex.lightRaw(u, v);
            return this;
        }

        @NotNull
        @Override
        public VertexConsumer normal(float x, float y, float z) {
            vertex.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            if (vertexIndex < vertices.length) {
                vertices[vertexIndex++] = vertex;
                vertex = new VertexData();
            }
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            //We don't support having a default color
        }

        @Override
        public void unsetDefaultColor() {
            //We don't support having a default color
        }

        @Override
        public VertexConsumer misc(VertexFormatElement element, int... rawData) {
            vertex.misc(element, Arrays.copyOf(rawData, rawData.length));
            return this;
        }
    }
}
