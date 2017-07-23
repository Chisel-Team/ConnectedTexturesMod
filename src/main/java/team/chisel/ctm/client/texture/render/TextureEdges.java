package team.chisel.ctm.client.texture.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.init.Blocks;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import team.chisel.ctm.CTM;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextCTM;
import team.chisel.ctm.client.texture.type.TextureTypeEdges;
import team.chisel.ctm.client.texture.type.TextureTypeEdges.CTMLogicEdges;
import team.chisel.ctm.client.util.Quad;
import team.chisel.ctm.client.util.Submap;

public class TextureEdges extends TextureCTM<TextureTypeEdges> {

    @Getter
    private final Set<Block> connectTo = new HashSet<>();
    
    public TextureEdges(TextureTypeEdges type, TextureInfo info) {
        super(type, info);
        JsonArray connectiondata = info.getInfo().map(obj -> JsonUtils.getJsonArray(obj, "connectTo")).orElse(new JsonArray());
        connectiondata.forEach(e -> {
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(e.getAsString()));
                if (block != Blocks.AIR) {
                    connectTo.add(block);
                    return;
                }
            }
            CTM.logger.warn("Could not find block {} for connection type, skipping...", e);
        });
    }
    
    @Override
    public List<BakedQuad> transformQuad(BakedQuad bq, ITextureContext context, int quadGoal) {
        Quad quad = makeQuad(bq, context);
        if (context == null) {
            return Collections.singletonList(quad.transformUVs(sprites[1], Submap.X2[0][0]).rebake());
        }
        
        CTMLogicEdges logic = (CTMLogicEdges) ((TextureContextCTM)context).getCTM(bq.getFace());
        if (logic.isObscured()) {
            return Arrays.stream(quad.transformUVs(sprites[2]).subdivide(4)).filter(Objects::nonNull).map(q -> q.rebake()).collect(Collectors.toList());
        }

        return super.transformQuad(bq, context, quadGoal);
    }
}
