package team.chisel.ctm.client.newctm.json;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Rule(String output, int from, Optional<String> at, List<String> connected, List<String> unconnected) {
    public static final Codec<Rule> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("output").forGetter(Rule::output),
            Codec.INT.optionalFieldOf("from", 0).forGetter(Rule::from),
            Codec.STRING.optionalFieldOf("at").forGetter(Rule::at),
            Codec.STRING.listOf().optionalFieldOf("connected", List.of()).forGetter(Rule::connected),
            Codec.STRING.listOf().optionalFieldOf("unconnected", List.of()).forGetter(Rule::unconnected))
            .apply(i, Rule::new));
}
