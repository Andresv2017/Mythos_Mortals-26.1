package net.darkblade.mythosmortals.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PeakSiting(int minY, int minProminence, int maxNeighbourRise, int maxCarve, float solidRatio, boolean debug) {

    public static final Codec<PeakSiting> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("min_y").forGetter(PeakSiting::minY),
        Codec.INT.optionalFieldOf("min_prominence", 25).forGetter(PeakSiting::minProminence),
        Codec.INT.optionalFieldOf("max_neighbour_rise", 8).forGetter(PeakSiting::maxNeighbourRise),
        Codec.INT.optionalFieldOf("max_carve", 16).forGetter(PeakSiting::maxCarve),
        Codec.FLOAT.optionalFieldOf("solid_ratio", 0.8F).forGetter(PeakSiting::solidRatio),
        Codec.BOOL.optionalFieldOf("debug", false).forGetter(PeakSiting::debug)
    ).apply(i, PeakSiting::new));

}
