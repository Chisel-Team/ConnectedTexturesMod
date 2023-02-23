package team.chisel.ctm.client.util;

import java.util.Arrays;

import org.apache.commons.lang3.NotImplementedException;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
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
                if (bit != (req == Trinary.TRUE)) {
                    return false;
                }
            }
            return true;
        }
    }
    
    private int size;
    private final Int2ObjectMap<LocalDirection> bitmap = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<ISubmap> outputs = new Int2ObjectOpenHashMap<>();
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
        this.curRule = submap;
        this.outputs.put(submap, texture);
        return this;
    }
    
    public CTMLogicBakery when(int bit, boolean is) {
        rules.putIfAbsent(curRule, new DesiredState(size, curRule));
        rules.compute(curRule, (i, s) -> s.with(bit, is ? Trinary.TRUE : Trinary.FALSE));
        return this;
    }
    
    public NewCTMLogic bake() {
        throw new NotImplementedException();
    }
    
    public static CTMLogicBakery TEST = new CTMLogicBakery()
            .input(0, Dir.TOP)
            .input(1, Dir.TOP_RIGHT)
            .input(2, Dir.RIGHT)
            .input(3, Dir.BOTTOM_RIGHT)
            .input(4, Dir.BOTTOM)
            .input(5, Dir.BOTTOM_LEFT)
            .input(6, Dir.LEFT)
            .input(7, Dir.TOP_LEFT)
            .output(0, Submap.X1)
                .when(0, false).when(2, false).when(4, false).when(6, false);
                
}
