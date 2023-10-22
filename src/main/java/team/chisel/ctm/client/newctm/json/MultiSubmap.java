package team.chisel.ctm.client.newctm.json;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.client.util.Submap;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class MultiSubmap {
    
    public abstract Iterable<Pair<String, ISubmap>> forName(String baseName);
    
    public static class Single extends MultiSubmap implements ISubmap {
        
        @Delegate
        private final ISubmap submap;
        
        public Single(float width, float height, float offsetX, float offsetY) {
            this.submap = Submap.fromUnitScale(width, height, offsetX, offsetY);
        }
        
        @Override
        public Iterable<Pair<String, ISubmap>> forName(String baseName) {
            return List.of(Pair.of(baseName, submap));
        }
    }
    
    public static class Grid extends MultiSubmap {
        
        private final List<Pair<String, ISubmap>> submaps = new ArrayList<>();
        @Getter
        private final int width, height;

        protected Grid(int width, int height) {
            this.width = width;
            this.height = height;
            var grid = Submap.grid(width, height);
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[i].length; j++) {
                    submaps.add(Pair.of(j + "," + i, grid[i][j]));
                }
            }
        }
        
        @Override
        public Iterable<Pair<String, ISubmap>> forName(String baseName) {
            return submaps.stream().map(p -> Pair.of(baseName + p.getLeft(), p.getRight())).toList();
        }
    }
}
