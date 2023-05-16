package team.chisel.ctm.client.newctm.json;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Direction;

public record Position(String id, List<Direction> directions) {
    public static final Codec<Position> CODEC = RecordCodecBuilder.create(i ->
        i.group(Codec.STRING.fieldOf("id").forGetter(Position::id),
                Direction.CODEC.listOf().fieldOf("directions").forGetter(Position::directions))
            .apply(i, Position::new));
}
