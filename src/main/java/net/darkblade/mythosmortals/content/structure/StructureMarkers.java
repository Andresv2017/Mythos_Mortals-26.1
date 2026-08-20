package net.darkblade.mythosmortals.content.structure;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Parser de los marcadores escritos en los structure blocks en modo Data.
 * <br><br>
 * Formato: {@code verbo:payload}, cortado por el <b>primer</b> {@code :}. El payload es un
 * {@link Identifier} al que se le pueden pegar flags separadas por {@code +}:
 * <pre>
 *   mob:deluxelib:arpy
 *   mob:deluxelib:arpy+armored
 *   chest:deluxelib:chests/arpy_nest
 * </pre>
 * Nada de espacios: el campo del structure block es texto libre y los espacios probablemente
 * sobrevivirían, pero separando por {@code :} y {@code +} no hay que preguntárselo. El {@code +} en
 * concreto no es un carácter legal en un {@code Identifier}, así que nunca se confunde con el id.
 * <br><br>
 * Está aparte de {@link MarkedStructurePiece} porque es lógica pura, sin mundo. Ningún camino de
 * aquí lanza: un marcador mal escrito deja la estructura sin ese mob o sin ese botín, nunca un
 * chunk corrupto. La posición se pasa sólo para que el log diga qué marcador hay que arreglar.
 */
public final class StructureMarkers {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Spawnea una entidad. Payload: el id de la entidad. */
    public static final String MOB = "mob";

    /** Pone una loot table al contenedor que hay <b>debajo</b>. Payload: el id de la tabla. */
    public static final String CHEST = "chest";

    /** Flag del verbo {@code mob}: la variante con armadura de un {@code IArmoredEntity}. */
    public static final String ARMORED = "armored";

    private StructureMarkers() {
        ;;
    }

    /**
     * Un marcador ya parseado.
     *
     * @param verb {@link #MOB}, {@link #CHEST}, o cualquier otra cosa que el llamante rechazará
     * @param id el payload como identificador
     * @param flags las flags pegadas con {@code +}, en minúsculas y sin orden
     */
    public record Marker(String verb, Identifier id, Set<String> flags) {
        public boolean hasFlag(String flag) {
            return this.flags.contains(flag);
        }
    }

    /**
     * @return vacío si el marcador no tiene verbo o si el payload no es un identificador válido
     */
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
