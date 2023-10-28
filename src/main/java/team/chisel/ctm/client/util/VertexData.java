package team.chisel.ctm.client.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class VertexData {

    private double posX, posY, posZ;
    private float normalX, normalY, normalZ;

    //Store int representations of the colors so that we don't go between ints and doubles when unpacking and repacking a vertex
    private int red, green, blue, alpha;

    // 0 to 16
    private float texU, texV;
    // 0 to 0xF0
    private int overlayU, overlayV;
    // 0 to 0xF0
    private int lightU, lightV;

    private Map<VertexFormatElement, int[]> miscData = new HashMap<>();

    public Vec3 getPos() {
        return new Vec3(posX, posY, posZ);
    }

    public Vec2 getUV() {
        return new Vec2(texU, texV);
    }

    public int getBlockLight() {
        return lightU >> 4;
    }

    public int getSkyLight() {
        return lightV >> 4;
    }

    public VertexData color(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        return this;
    }

    public VertexData pos(double x, double y, double z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        return this;
    }

    public VertexData normal(float x, float y, float z) {
        this.normalX = x;
        this.normalY = y;
        this.normalZ = z;
        return this;
    }

    public VertexData texRaw(float u, float v) {
        texU = u;
        texV = v;
        return this;
    }

    public VertexData overlay(int u, int v) {
        overlayU = u;
        overlayV = v;
        return this;
    }

    public VertexData lightRaw(int u, int v) {
        lightU = u;
        lightV = v;
        return this;
    }

    public VertexData light(int u, int v) {
        return lightRaw(u << 4, v << 4);
    }

    public VertexData misc(VertexFormatElement element, int... data) {
        miscData.put(element, data);
        return this;
    }

    public VertexData copy(boolean deepCopy) {
        Map<VertexFormatElement, int[]> miscCopy;
        if (deepCopy) {
            //Deep copy the misc data
            miscCopy = new HashMap<>();
            for (Map.Entry<VertexFormatElement, int[]> entry : miscData.entrySet()) {
                miscCopy.put(entry.getKey(), Arrays.copyOf(entry.getValue(), entry.getValue().length));
            }
        } else {
            miscCopy = miscData;
        }
        return new VertexData(posX, posY, posZ, normalX, normalY, normalZ, red, green, blue, alpha, texU, texV, overlayU, overlayV, lightU, lightV, miscCopy);
    }

    public void write(VertexConsumer consumer) {
        consumer.vertex(posX, posY, posZ);
        consumer.color(red, green, blue, alpha);
        consumer.uv(texU, texV);
        consumer.overlayCoords(overlayU, overlayV);
        consumer.uv2(lightU, lightV);
        consumer.normal(normalX, normalY, normalZ);
        for (Map.Entry<VertexFormatElement, int[]> entry : miscData.entrySet()) {
            consumer.misc(entry.getKey(), entry.getValue());
        }
        consumer.endVertex();
    }
}