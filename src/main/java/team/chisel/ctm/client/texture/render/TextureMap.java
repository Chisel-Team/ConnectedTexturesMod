package team.chisel.ctm.client.texture.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

import lombok.Getter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextGrid;
import team.chisel.ctm.client.texture.ctx.TextureContextGrid.Point2i;
import team.chisel.ctm.client.texture.ctx.TextureContextPosition;
import team.chisel.ctm.client.texture.type.TextureTypeMap;
import team.chisel.ctm.client.util.Quad;
import team.chisel.ctm.client.util.Submap;

public class TextureMap extends AbstractTexture<TextureTypeMap> {

    public enum MapType {
        RANDOM {

            @Override
            protected List<BakedQuad> transformQuad(TextureMap tex, BakedQuad quad, @Nullable ITextureContext context, int quadGoal) {

                Point2i textureCoords = context == null ? new Point2i(1, 1) : ((TextureContextGrid)context).getTextureCoords(quad.getDirection());
                
                float intervalX = 1f / tex.getXSize();
                float intervalY = 1f / tex.getYSize();
                
                float maxU = textureCoords.getX() * intervalX;
                float maxV = textureCoords.getY() * intervalY;
                ISubmap uvs = Submap.fromUnitScale(intervalX, intervalY, maxU - intervalX, maxV - intervalY);

                Quad q = tex.makeQuad(quad, context).setFullbright(tex.fullbright);
                
                // TODO move this code somewhere else, it's copied from below
                if (quadGoal != 4) {
                    return Collections.singletonList(q.transformUVs(tex.sprites[0], uvs).setFullbright(tex.fullbright).rebake());
                } else {
                    Quad[] quads = q.subdivide(4);

                    for (int i = 0; i < quads.length; i++) {
                        if (quads[i] != null) {
                            quads[i] = quads[i].transformUVs(tex.sprites[0], uvs);
                        }
                    }
                    return Arrays.stream(quads).filter(Objects::nonNull).map(Quad::rebake).toList();
                }
            }
            
            @Override
            public ITextureContext getContext(@NotNull BlockPos pos, @NotNull TextureMap tex) {
                return new TextureContextGrid.Random(pos, tex, true);
            }
        },
        PATTERNED {

            @Override
            protected List<BakedQuad> transformQuad(TextureMap tex, BakedQuad quad, @Nullable ITextureContext context, int quadGoal) {
                
                Point2i textureCoords = context == null ? new Point2i(0, 0) : ((TextureContextGrid)context).getTextureCoords(quad.getDirection());
                
                float intervalU = 1f / tex.xSize;
                float intervalV = 1f / tex.ySize;

                // throw new RuntimeException(index % variationSize+" and "+index/variationSize);
                float minU = intervalU * textureCoords.getX();
                float minV = intervalV * textureCoords.getY();

                ISubmap submap = Submap.fromUnitScale(intervalU, intervalV, minU, minV);

                Quad q = tex.makeQuad(quad, context).setFullbright(tex.fullbright);
                if (quadGoal != 4) {
                    return Collections.singletonList(q.transformUVs(tex.sprites[0], submap).rebake());
                } else {
                    Quad[] quads = q.subdivide(4);

                    for (int i = 0; i < quads.length; i++) {
                        if (quads[i] != null) {
                            quads[i] = quads[i].transformUVs(tex.sprites[0], submap);
                        }
                    }
                    return Arrays.stream(quads).filter(Objects::nonNull).map(Quad::rebake).toList();
                }
            }
            
            @Override
            public ITextureContext getContext(@NotNull BlockPos pos, @NotNull TextureMap tex) {
                return new TextureContextGrid.Patterned(pos, tex, true);
            }
        };

        protected abstract List<BakedQuad> transformQuad(TextureMap tex, BakedQuad quad, @Nullable ITextureContext context, int quadGoal);
        
        @NotNull
        public ITextureContext getContext(@NotNull BlockPos pos, @NotNull TextureMap tex) {
            return new TextureContextPosition(pos);
        }
    }

    @Getter
    private final int xSize;
    @Getter
    private final int ySize;
    @Getter
    private final int xOffset;
    @Getter
    private final int yOffset;

    private final MapType map;

    public TextureMap(TextureTypeMap type, TextureInfo info, MapType map) {
        super(type, info);

        this.map = map;

        if (info.getInfo().isPresent()) {
            JsonObject object = info.getInfo().get();
            if (object.has("width") && object.has("height")) {
                Preconditions.checkArgument(object.get("width").isJsonPrimitive() && object.get("width").getAsJsonPrimitive().isNumber(), "width must be a number!");
                Preconditions.checkArgument(object.get("height").isJsonPrimitive() && object.get("height").getAsJsonPrimitive().isNumber(), "height must be a number!");

                this.xSize = object.get("width").getAsInt();
                this.ySize = object.get("height").getAsInt();

            } else if (object.has("size")) {
                Preconditions.checkArgument(object.get("size").isJsonPrimitive() && object.get("size").getAsJsonPrimitive().isNumber(), "size must be a number!");

                this.xSize = object.get("size").getAsInt();
                this.ySize = object.get("size").getAsInt();
            } else {
                xSize = ySize = 2;
            }

            if (object.has("x_offset")) {
                Preconditions.checkArgument(object.get("x_offset").isJsonPrimitive() && object.get("x_offset").getAsJsonPrimitive().isNumber(), "x_offset must be a number!");

                this.xOffset = object.get("x_offset").getAsInt();
            } else {
                this.xOffset = 0;
            }

            if (object.has("y_offset")) {
                Preconditions.checkArgument(object.get("y_offset").isJsonPrimitive() && object.get("y_offset").getAsJsonPrimitive().isNumber(), "y_offset must be a number!");

                this.yOffset = object.get("y_offset").getAsInt();
            } else {
                this.yOffset = 0;
            }
        } else {
            xOffset = yOffset = 0;
            xSize = ySize = 2;
        }

        Preconditions.checkArgument(xSize > 0 && ySize > 0, "Cannot have a dimension of 0!");
    }

    @Override
    public List<BakedQuad> transformQuad(BakedQuad quad, ITextureContext context, int quadGoal) {
        return map.transformQuad(this, quad, context, quadGoal);
    }
}
