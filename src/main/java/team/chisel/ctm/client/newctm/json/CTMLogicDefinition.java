package team.chisel.ctm.client.newctm.json;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CTMLogicDefinition(List<Position> positions, Map<String, MultiSubmap> submaps, Map<String, MultiSubmap> faces, List<Rule> rules) {
    public static final Codec<CTMLogicDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Position.CODEC.listOf().fieldOf("positions").forGetter(CTMLogicDefinition::positions),
            Codec.unboundedMap(Codec.STRING, SubmapCodecs.CODEC)
                .fieldOf("submaps").forGetter(CTMLogicDefinition::submaps),
            Codec.unboundedMap(Codec.STRING, SubmapCodecs.CODEC)
                .optionalFieldOf("faces", Map.of()).forGetter(CTMLogicDefinition::faces),
            Rule.CODEC.listOf().fieldOf("rules").forGetter(CTMLogicDefinition::rules))
        .apply(i, CTMLogicDefinition::new));
}
