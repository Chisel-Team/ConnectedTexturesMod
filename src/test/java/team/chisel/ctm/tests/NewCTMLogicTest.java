package team.chisel.ctm.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer.Buffered;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.client.newctm.CustomCTMLogic;
import team.chisel.ctm.client.newctm.CTMLogicBakery.OutputFace;
import team.chisel.ctm.client.newctm.json.CTMDefinitionManager;
import team.chisel.ctm.client.util.Quad;
import team.chisel.ctm.client.util.Submap;

public class NewCTMLogicTest {
        
    @Test
    void submaps() {
        assertArrayEquals(Submap.X2, Submap.grid(2, 2));
        assertArrayEquals(Submap.X3, Submap.grid(3, 3));
        assertArrayEquals(Submap.X4, Submap.grid(4, 4));
        System.out.println(Arrays.deepToString(Submap.grid(12, 4)));
    }
    
    @Test
    void quad() {
        QuadBakingVertexConsumer.Buffered builder = new Buffered();
        builder.vertex(0, 0, 0).uv(0, 0).normal(0, 1, 0).endVertex();
        builder.vertex(1, 0, 0).uv(1, 0).normal(0, 1, 0).endVertex();
        builder.vertex(1, 0, 1).uv(1, 1).normal(0, 1, 0).endVertex();
        builder.vertex(0, 0, 1).uv(0, 1).normal(0, 1, 0).endVertex();
        
        Quad q = Quad.from(builder.getQuad());
        Quad quarter = q.subsect(Submap.fromPixelScale(8, 8, 0, 0));
        Quad half = q.subsect(Submap.fromPixelScale(8, 16, 0, 0));
        Quad center = q.subsect(Submap.fromPixelScale(8, 8, 4, 4));
        
        assertNotNull(quarter);
        assertNotNull(half);
        assertNotNull(center);
    }

    @BeforeAll
    static void bootstrap() {
        SharedConstants.CHECK_DATA_FIXER_SCHEMA = false;
        Bootstrap.bootStrap();
    }

    @Test
    void connection() {
        var test = createTest();
        
        assertArrayEquals(new int[] { 0 }, test.lookups[0]);
        assertArrayEquals(new int[] { 7 }, test.lookups[42]);
        assertArrayEquals(new int[] { 26 }, test.lookups[255]);
        assertArrayEquals(new int[] { 46 }, test.lookups[170]);
        
        // Reference tile IDs for test: https://optifine.readthedocs.io/_images/ctm.webp
        ISubmap[] reference = Arrays.stream(Submap.grid(12, 4)).flatMap(Arrays::stream).toArray(ISubmap[]::new);

        // Make sure generated tiles match expected values
        assertEquals(Submap.fromPixelScale(16f / 12, 16f / 4, 0, 0), reference[0]);
        assertEquals(Submap.fromPixelScale(16f / 12, 16f / 4, (16f / 12) * 10, (16f / 4) * 2), reference[34]);
        
        var world = new TestBlockGetter();
        OutputFace[] output;
        world.addBlock(BlockPos.ZERO, Blocks.STONE.defaultBlockState());
        // Simple case of one connection upwards
        world.addBlock(BlockPos.ZERO.above(), Blocks.STONE.defaultBlockState());
        output = test.getSubmaps(world, BlockPos.ZERO, Direction.EAST);
        assertEquals(1, output.length);
        assertEquals(reference[36], output[0].getUvs());
        // Add a diagonal connection that should not affect the result
        world.addBlock(BlockPos.ZERO.above().north(), Blocks.STONE.defaultBlockState());
        output = test.getSubmaps(world, BlockPos.ZERO, Direction.EAST);
        assertEquals(1, output.length);
        assertEquals(reference[36], output[0].getUvs());
        // Add a sideways connection to make it a L shape with no inner corner
        world.addBlock(BlockPos.ZERO.north(), Blocks.STONE.defaultBlockState());
        output = test.getSubmaps(world, BlockPos.ZERO, Direction.EAST);
        assertEquals(1, output.length);
        assertEquals(reference[37], output[0].getUvs());
        // Remove the diagonal connection to test inner corner
        world.removeBlock(BlockPos.ZERO.above().north());
        output = test.getSubmaps(world, BlockPos.ZERO, Direction.EAST);
        assertEquals(1, output.length);
        assertEquals(reference[16], output[0].getUvs());
    }

    private static CustomCTMLogic createTest() {
        var is = NewCTMLogicTest.class.getResourceAsStream("/assets/ctm/ctm_logic/optifine_full.json");
        try (var r = new BufferedReader(new InputStreamReader(is))) {
            return CTMDefinitionManager.fromReader(r);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
