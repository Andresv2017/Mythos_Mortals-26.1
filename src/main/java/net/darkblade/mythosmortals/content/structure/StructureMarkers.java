package net.darkblade.mythosmortals.content.structure;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class StructureMarkers {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String MOB = "mob";

    public static final String CHEST = "chest";

    public static final String ARMORED = "armored";

    private StructureMarkers() {
        ;;
    }

    public record Marker(String verb, Identifier id, Set<String> flags) {
        public boolean hasFlag(String flag) {
            return this.flags.contains(flag);
        }
    }

    public static Optional<Marker> parse(String marker, BlockPos position) {
        int separator = marker.indexOf(':');
        if (separator < 0) {
            LOGGER.warn("Marcador sin verbo en {}: '{}' (se esperaba 'verbo:payload')", position, marker);
            return Optional.empty();
        }

        String verb = marker.substring(0, separator);
        String payload = marker.substring(separator + 1);

        String[] parts = payload.split("\\+");
        Identifier id = Identifier.tryParse(parts[0]);
        if (id == null) {
            LOGGER.warn("Payload '{}' no es un identificador válido, en el marcador de {}", parts[0], position);
            return Optional.empty();
        }

        Set<String> flags = new HashSet<>();
        for (int i = 1; i < parts.length; i++) {
            flags.add(parts[i].toLowerCase(Locale.ROOT));
        }

        return Optional.of(new Marker(verb, id, Set.copyOf(flags)));
    }

}
