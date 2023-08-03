package team.chisel.ctm.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer.Buffered;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.client.newctm.CTMLogicBakery;
import team.chisel.ctm.client.newctm.CustomCTMLogic;
import team.chisel.ctm.client.newctm.CTMLogicBakery.OutputFace;
import team.chisel.ctm.client.util.Quad;
import team.chisel.ctm.client.util.Submap;

public class NewCTMLogicTest {
    
//    private final NewCTMLogic logic = new NewCTMLogic(null, null, null, null);
    
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
    
    // Prebaked rules for optifine style CTM with outputs 1-47
    private static final String TEST_INPUT = "00000000 1\r\n"
            + "00000001    1\r\n"
            + "00000010    4\r\n"
            + "00000011    4\r\n"
            + "00000100    1\r\n"
            + "00000101    1\r\n"
            + "00000110    4\r\n"
            + "00000111    4\r\n"
            + "00001000    13\r\n"
            + "00001001    13\r\n"
            + "00001010    6\r\n"
            + "00001011    6\r\n"
            + "00001100    13\r\n"
            + "00001101    13\r\n"
            + "00001110    16\r\n"
            + "00001111    16\r\n"
            + "00010000    1\r\n"
            + "00010001    1\r\n"
            + "00010010    4\r\n"
            + "00010011    4\r\n"
            + "00010100    1\r\n"
            + "00010101    1\r\n"
            + "00010110    4\r\n"
            + "00010111    4\r\n"
            + "00011000    13\r\n"
            + "00011001    13\r\n"
            + "00011010    6\r\n"
            + "00011011    6\r\n"
            + "00011100    13\r\n"
            + "00011101    13\r\n"
            + "00011110    16\r\n"
            + "00011111    16\r\n"
            + "00100000    2\r\n"
            + "00100001    2\r\n"
            + "00100010    3\r\n"
            + "00100011    3\r\n"
            + "00100100    2\r\n"
            + "00100101    2\r\n"
            + "00100110    3\r\n"
            + "00100111    3\r\n"
            + "00101000    5\r\n"
            + "00101001    5\r\n"
            + "00101010    8\r\n"
            + "00101011    8\r\n"
            + "00101100    5\r\n"
            + "00101101    5\r\n"
            + "00101110    30\r\n"
            + "00101111    30\r\n"
            + "00110000    2\r\n"
            + "00110001    2\r\n"
            + "00110010    3\r\n"
            + "00110011    3\r\n"
            + "00110100    2\r\n"
            + "00110101    2\r\n"
            + "00110110    3\r\n"
            + "00110111    3\r\n"
            + "00111000    14\r\n"
            + "00111001    14\r\n"
            + "00111010    32\r\n"
            + "00111011    32\r\n"
            + "00111100    14\r\n"
            + "00111101    14\r\n"
            + "00111110    15\r\n"
            + "00111111    15\r\n"
            + "01000000    1\r\n"
            + "01000001    1\r\n"
            + "01000010    4\r\n"
            + "01000011    4\r\n"
            + "01000100    1\r\n"
            + "01000101    1\r\n"
            + "01000110    4\r\n"
            + "01000111    4\r\n"
            + "01001000    13\r\n"
            + "01001001    13\r\n"
            + "01001010    6\r\n"
            + "01001011    6\r\n"
            + "01001100    13\r\n"
            + "01001101    13\r\n"
            + "01001110    16\r\n"
            + "01001111    16\r\n"
            + "01010000    1\r\n"
            + "01010001    1\r\n"
            + "01010010    4\r\n"
            + "01010011    4\r\n"
            + "01010100    1\r\n"
            + "01010101    1\r\n"
            + "01010110    4\r\n"
            + "01010111    4\r\n"
            + "01011000    13\r\n"
            + "01011001    13\r\n"
            + "01011010    6\r\n"
            + "01011011    6\r\n"
            + "01011100    13\r\n"
            + "01011101    13\r\n"
            + "01011110    16\r\n"
            + "01011111    16\r\n"
            + "01100000    2\r\n"
            + "01100001    2\r\n"
            + "01100010    3\r\n"
            + "01100011    3\r\n"
            + "01100100    2\r\n"
            + "01100101    2\r\n"
            + "01100110    3\r\n"
            + "01100111    3\r\n"
            + "01101000    5\r\n"
            + "01101001    5\r\n"
            + "01101010    8\r\n"
            + "01101011    8\r\n"
            + "01101100    5\r\n"
            + "01101101    5\r\n"
            + "01101110    30\r\n"
            + "01101111    30\r\n"
            + "01110000    2\r\n"
            + "01110001    2\r\n"
            + "01110010    3\r\n"
            + "01110011    3\r\n"
            + "01110100    2\r\n"
            + "01110101    2\r\n"
            + "01110110    3\r\n"
            + "01110111    3\r\n"
            + "01111000    14\r\n"
            + "01111001    14\r\n"
            + "01111010    32\r\n"
            + "01111011    32\r\n"
            + "01111100    14\r\n"
            + "01111101    14\r\n"
            + "01111110    15\r\n"
            + "01111111    15\r\n"
            + "10000000    37\r\n"
            + "10000001    37\r\n"
            + "10000010    18\r\n"
            + "10000011    40\r\n"
            + "10000100    37\r\n"
            + "10000101    37\r\n"
            + "10000110    18\r\n"
            + "10000111    40\r\n"
            + "10001000    25\r\n"
            + "10001001    25\r\n"
            + "10001010    20\r\n"
            + "10001011    42\r\n"
            + "10001100    25\r\n"
            + "10001101    25\r\n"
            + "10001110    44\r\n"
            + "10001111    28\r\n"
            + "10010000    37\r\n"
            + "10010001    37\r\n"
            + "10010010    18\r\n"
            + "10010011    40\r\n"
            + "10010100    37\r\n"
            + "10010101    37\r\n"
            + "10010110    18\r\n"
            + "10010111    40\r\n"
            + "10011000    25\r\n"
            + "10011001    25\r\n"
            + "10011010    20\r\n"
            + "10011011    42\r\n"
            + "10011100    25\r\n"
            + "10011101    25\r\n"
            + "10011110    44\r\n"
            + "10011111    28\r\n"
            + "10100000    17\r\n"
            + "10100001    17\r\n"
            + "10100010    19\r\n"
            + "10100011    43\r\n"
            + "10100100    17\r\n"
            + "10100101    17\r\n"
            + "10100110    19\r\n"
            + "10100111    43\r\n"
            + "10101000    7\r\n"
            + "10101001    7\r\n"
            + "10101010    47\r\n"
            + "10101011    21\r\n"
            + "10101100    7\r\n"
            + "10101101    7\r\n"
            + "10101110    22\r\n"
            + "10101111    11\r\n"
            + "10110000    17\r\n"
            + "10110001    17\r\n"
            + "10110010    19\r\n"
            + "10110011    43\r\n"
            + "10110100    17\r\n"
            + "10110101    17\r\n"
            + "10110110    19\r\n"
            + "10110111    43\r\n"
            + "10111000    29\r\n"
            + "10111001    29\r\n"
            + "10111010    10\r\n"
            + "10111011    36\r\n"
            + "10111100    29\r\n"
            + "10111101    29\r\n"
            + "10111110    23\r\n"
            + "10111111    45\r\n"
            + "11000000    37\r\n"
            + "11000001    37\r\n"
            + "11000010    18\r\n"
            + "11000011    40\r\n"
            + "11000100    37\r\n"
            + "11000101    37\r\n"
            + "11000110    18\r\n"
            + "11000111    40\r\n"
            + "11001000    25\r\n"
            + "11001001    25\r\n"
            + "11001010    20\r\n"
            + "11001011    42\r\n"
            + "11001100    25\r\n"
            + "11001101    25\r\n"
            + "11001110    44\r\n"
            + "11001111    28\r\n"
            + "11010000    37\r\n"
            + "11010001    37\r\n"
            + "11010010    18\r\n"
            + "11010011    40\r\n"
            + "11010100    37\r\n"
            + "11010101    37\r\n"
            + "11010110    18\r\n"
            + "11010111    40\r\n"
            + "11011000    25\r\n"
            + "11011001    25\r\n"
            + "11011010    20\r\n"
            + "11011011    42\r\n"
            + "11011100    25\r\n"
            + "11011101    25\r\n"
            + "11011110    44\r\n"
            + "11011111    28\r\n"
            + "11100000    38\r\n"
            + "11100001    38\r\n"
            + "11100010    41\r\n"
            + "11100011    39\r\n"
            + "11100100    38\r\n"
            + "11100101    38\r\n"
            + "11100110    41\r\n"
            + "11100111    39\r\n"
            + "11101000    31\r\n"
            + "11101001    31\r\n"
            + "11101010    9\r\n"
            + "11101011    12\r\n"
            + "11101100    31\r\n"
            + "11101101    31\r\n"
            + "11101110    35\r\n"
            + "11101111    33\r\n"
            + "11110000    38\r\n"
            + "11110001    38\r\n"
            + "11110010    41\r\n"
            + "11110011    39\r\n"
            + "11110100    38\r\n"
            + "11110101    38\r\n"
            + "11110110    41\r\n"
            + "11110111    39\r\n"
            + "11111000    26\r\n"
            + "11111001    26\r\n"
            + "11111010    24\r\n"
            + "11111011    34\r\n"
            + "11111100    26\r\n"
            + "11111101    26\r\n"
            + "11111110    46\r\n"
            + "11111111    27";
    
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
        
        System.out.println(CTMLogicBakery.TEST_OF.asJsonExample());
    }

    private static CustomCTMLogic createTest() {
        return CTMLogicBakery.TEST_OF.bake();
//        String[] lines = TEST_INPUT.split("\r\n");
//        int[][] lookups = new int[256][];
//        for (int i = 0; i < lines.length; i++) {
//            lookups[i] = new int[] { Integer.parseInt(lines[i].substring(lines[i].length() - 2).trim()) - 1 };
//        }
//
//        return new NewCTMLogic(lookups, Arrays.stream(Submap.grid(12, 4)).flatMap(Arrays::stream).toArray(ISubmap[]::new), Dir.values(), new ConnectionCheck());
    }
}
