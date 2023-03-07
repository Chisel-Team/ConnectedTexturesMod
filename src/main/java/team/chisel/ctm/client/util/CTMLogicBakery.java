package team.chisel.ctm.client.util;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.IntFunction;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonWriter;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import team.chisel.ctm.api.texture.ISubmap;

@RequiredArgsConstructor
public class CTMLogicBakery {
    
    @RequiredArgsConstructor
    private enum Trinary {
        FALSE(false, 0),
        TRUE(true, 1),
        DONT_CARE(false, 0),
        ;
        
        public final boolean val;
        public final int bit;
    }
    
    private @Value class DesiredState {
        
        private final Trinary[] input;
        private final int output;
        
        public DesiredState(int size, int output) {
            this.input = new Trinary[size];
            Arrays.fill(this.input, Trinary.DONT_CARE);
            this.output = output;
        }
        
        public DesiredState with(int bit, Trinary in) {
            this.input[bit] = in;
            return this;
        } 
        
        public boolean test(int state) {
            for (int i = 0; i < input.length; i++) {
                Trinary req = input[i];
                boolean bit = ((state >> i) & 1) == 1;
                if (req != Trinary.DONT_CARE && bit != (req == Trinary.TRUE)) {
                    return false;
                }
            }
            return true;
        }

        @SneakyThrows
        public String asJson(LocalDirection[] values) {
            var buf = new StringWriter();
            JsonWriter writer = new JsonWriter(buf);
            writer.beginObject();
            {
                writer.name("output").value(output);
                List<LocalDirection> connected = new ArrayList<>();
                List<LocalDirection> unconnected = new ArrayList<>();
                for (int i = 0; i < input.length; i++) {
                    if (input[i] == Trinary.TRUE) {
                        connected.add(values[input.length - 1 - i]);
                    } else if (input[i] == Trinary.FALSE) {
                        unconnected.add(values[input.length - 1 - i]);
                    }
                }
                writer.name("connected");
                writer.beginArray();
                for (var d : connected) {
                    writer.value(d.name());
                }
                writer.endArray();
                
                writer.name("unconnected");
                writer.beginArray();
                for (var d : unconnected) {
                    writer.value(d.name());
                }
                writer.endArray();
            }
            writer.endObject();
            writer.flush();
            writer.close();
            return buf.toString();
        }
    }
    
    @Value
    public class OutputFace {
        
        ISubmap uvs;
        ISubmap face;
    }
    
    private int size;
    private final Int2ObjectMap<LocalDirection> bitmap = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<OutputFace> outputs = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<DesiredState> rules = new Int2ObjectOpenHashMap<>();

    public CTMLogicBakery input(int bit, LocalDirection dir) {
        bitmap.put(bit, dir);
        if (bit >= size) {
            size = bit + 1;
        }
        return this;
    }
    
    private int curRule = -1;
    public CTMLogicBakery output(int submap, ISubmap texture) {
        return output(submap, texture, Submap.X1);
    }
    
    public CTMLogicBakery output(int submap, ISubmap texture, ISubmap at) {
        this.curRule = submap;
        this.outputs.put(submap, new OutputFace(texture, at));
        return this;
    }
    
    public CTMLogicBakery when(int bit, boolean is) {
        Preconditions.checkArgument(bit < size, "bit out of range");
        rules.putIfAbsent(curRule, new DesiredState(size, curRule));
        rules.compute(curRule, (i, s) -> s.with(bit, is ? Trinary.TRUE : Trinary.FALSE));
        return this;
    }
    
    public CTMLogicBakery when(String pattern) {
        Preconditions.checkArgument(pattern.length() == size, "pattern length");
        for (int i = pattern.length() - 1; i >= 0; i--) {
            char bit = pattern.charAt(i);
            if (bit == '0' || bit == '1') {
                when(pattern.length() - 1 - i, bit == '1');
            }
        }
        return this;
    }
    
    public NewCTMLogic bake() {
        int max = 1 << size;
        int[][] lookups = new int[max][];
        for (int state = 0; state < max; state++) {
            for (var e : rules.int2ObjectEntrySet()) {
                if (e.getValue().test(state)) {
                    if (lookups[state] == null) {
                        lookups[state] = new int[] { e.getIntKey() };
                    } else {
                        lookups[state] = ArrayUtils.add(lookups[state], e.getIntKey());
                    }
                }
            }
        }
        return new NewCTMLogic(lookups, asSortedArray(outputs, OutputFace[]::new), asSortedArray(bitmap, LocalDirection[]::new), new ConnectionCheck());
    }
    
    private <T> T[] asSortedArray(Int2ObjectMap<T> indexedMap, IntFunction<T[]> ctor) {
        return indexedMap.int2ObjectEntrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e1.getIntKey(), e2.getIntKey()))
            .map(Entry::getValue)
            .toArray(ctor);
    }
    
    @SneakyThrows
    public String asJsonExample() {
        var buf = new StringWriter();
        JsonWriter writer = new JsonWriter(buf);
        writer.setIndent("  ");
        writer.beginObject();
        {
            LocalDirection[] orderedPositions = asSortedArray(bitmap, LocalDirection[]::new);
            writer.name("positions");
            writer.beginArray();
            {
                for (LocalDirection dir : orderedPositions) {
                    writer.jsonValue(dir.asJson());
                }
            }
            writer.endArray();
            writer.name("submaps");
            writer.beginObject();
            {
                writer.name("type").value("grid");
                writer.name("width").value(12);
                writer.name("height").value(4);
            }
            writer.endObject();
            writer.name("rules");
            writer.beginArray();
            {
                for (var e : rules.int2ObjectEntrySet().stream().sorted((e1, e2) -> Integer.compare(e1.getIntKey(), e2.getIntKey())).toList()) {
                    writer.jsonValue(e.getValue().asJson(orderedPositions));
                }
            }
            writer.endArray();
        }
        writer.endObject();
        writer.flush();
        writer.close();
        return buf.toString();
    }
    
    private static final ISubmap[][] OF_FORMAT = Submap.grid(12, 4);
    public static CTMLogicBakery TEST_OF = new CTMLogicBakery()
            .input(0, Dir.TOP) // LSB
            .input(1, Dir.TOP_RIGHT)
            .input(2, Dir.RIGHT)
            .input(3, Dir.BOTTOM_RIGHT)
            .input(4, Dir.BOTTOM)
            .input(5, Dir.BOTTOM_LEFT)
            .input(6, Dir.LEFT)
            .input(7, Dir.TOP_LEFT) // MSB
            .output(0, OF_FORMAT[0][0]).when("0X0X0X0X")
            .output(1, OF_FORMAT[0][1]).when("0X1X0X0X")
            .output(2, OF_FORMAT[0][2]).when("0X1X0X1X")
            .output(3, OF_FORMAT[0][3]).when("0X0X0X1X")
            .output(4, OF_FORMAT[0][4]).when("0X101X0X")
            .output(5, OF_FORMAT[0][5]).when("0X0X101X")
            .output(6, OF_FORMAT[0][6]).when("10101X0X")
            .output(7, OF_FORMAT[0][7]).when("0X10101X")
            .output(8, OF_FORMAT[0][8]).when("11101010")
            .output(9, OF_FORMAT[0][9]).when("10111010")
            .output(10, OF_FORMAT[0][10]).when("10101111")
            .output(11, OF_FORMAT[0][11]).when("11101011")
            .output(12, OF_FORMAT[1][0]).when("0X0X1X0X")
            .output(13, OF_FORMAT[1][1]).when("0X111X0X")
            .output(14, OF_FORMAT[1][2]).when("0X11111X")
            .output(15, OF_FORMAT[1][3]).when("0X0X111X")
            .output(16, OF_FORMAT[1][4]).when("101X0X0X")
            .output(17, OF_FORMAT[1][5]).when("1X0X0X10")
            .output(18, OF_FORMAT[1][6]).when("101X0X10")
            .output(19, OF_FORMAT[1][7]).when("1X0X1010")
            .output(20, OF_FORMAT[1][8]).when("10101011")
            .output(21, OF_FORMAT[1][9]).when("10101110")
            .output(22, OF_FORMAT[1][10]).when("10111110")
            .output(23, OF_FORMAT[1][11]).when("11111010")
            .output(24, OF_FORMAT[2][0]).when("1X0X1X0X")
            .output(25, OF_FORMAT[2][1]).when("11111X0X")
            .output(26, OF_FORMAT[2][2]).when("11111111")
            .output(27, OF_FORMAT[2][3]).when("1X0X1111")
            .output(28, OF_FORMAT[2][4]).when("10111X0X")
            .output(29, OF_FORMAT[2][5]).when("0X10111X")
            .output(30, OF_FORMAT[2][6]).when("11101X0X")
            .output(31, OF_FORMAT[2][7]).when("0X11101X")
            .output(32, OF_FORMAT[2][8]).when("11101111")
            .output(33, OF_FORMAT[2][9]).when("11111011")
            .output(34, OF_FORMAT[2][10]).when("11101110")
            .output(35, OF_FORMAT[2][11]).when("10111011")
            .output(36, OF_FORMAT[3][0]).when("1X0X0X0X")
            .output(37, OF_FORMAT[3][1]).when("111X0X0X")
            .output(38, OF_FORMAT[3][2]).when("111X0X11")
            .output(39, OF_FORMAT[3][3]).when("1X0X0X11")
            .output(40, OF_FORMAT[3][4]).when("111X0X10")
            .output(41, OF_FORMAT[3][5]).when("1X0X1011")
            .output(42, OF_FORMAT[3][6]).when("101X0X11")
            .output(43, OF_FORMAT[3][7]).when("1X0X1110")
            .output(44, OF_FORMAT[3][8]).when("10111111")
            .output(45, OF_FORMAT[3][9]).when("11111110")
            .output(46, OF_FORMAT[3][10]).when("10101010");
    
    private static final ISubmap[][] CTM_FORMAT = Submap.X4;
    private static final ISubmap[][] CTM_QUADS = Submap.X2;
    public static final CTMLogicBakery TEST_CTM = new CTMLogicBakery()
            .input(0, Dir.TOP) // LSB
            .input(1, Dir.TOP_RIGHT)
            .input(2, Dir.RIGHT)
            .input(3, Dir.BOTTOM_RIGHT)
            .input(4, Dir.BOTTOM)
            .input(5, Dir.BOTTOM_LEFT)
            .input(6, Dir.LEFT)
            .input(7, Dir.TOP_LEFT) // MSB
            .output(0, CTM_FORMAT[0][0], CTM_QUADS[0][0])
            .output(1, CTM_FORMAT[0][1], CTM_QUADS[0][1])
            .output(2, CTM_FORMAT[0][2], CTM_QUADS[0][0])
            .output(3, CTM_FORMAT[0][3], CTM_QUADS[0][1])
            .output(4, CTM_FORMAT[1][0], CTM_QUADS[1][0])
            .output(5, CTM_FORMAT[1][1], CTM_QUADS[1][1])
            .output(6, CTM_FORMAT[1][2], CTM_QUADS[1][0])
            .output(7, CTM_FORMAT[1][3], CTM_QUADS[1][1])
            .output(8, CTM_FORMAT[2][0], CTM_QUADS[0][0])
            .output(9, CTM_FORMAT[2][1], CTM_QUADS[0][1])
            .output(10,CTM_FORMAT[2][2], CTM_QUADS[0][0])
            .output(11,CTM_FORMAT[2][3], CTM_QUADS[0][1])
            .output(12,CTM_FORMAT[3][0], CTM_QUADS[1][0])
            .output(13,CTM_FORMAT[3][1], CTM_QUADS[1][1])
            .output(14,CTM_FORMAT[3][2], CTM_QUADS[1][0])
            .output(15,CTM_FORMAT[3][3], CTM_QUADS[1][1]);
}
