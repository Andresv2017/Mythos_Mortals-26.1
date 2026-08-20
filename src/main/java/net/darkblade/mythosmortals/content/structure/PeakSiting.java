package net.darkblade.mythosmortals.content.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Ajustes de "ponme esto en la cima de una montaña", para las estructuras que los declaran en su
 * JSON bajo {@code peak_siting}. Si el bloque no está, la estructura se coloca en el centro del
 * chunk sin tallar nada, que es el comportamiento normal.
 *
 * <pre>
 *   "peak_siting": { "min_y": 100, "min_prominence": 25, "max_neighbour_rise": 8,
 *                    "max_carve": 16, "solid_ratio": 0.8, "debug": false }
 * </pre>
 *
 * @param minY altura mínima de la plataforma resultante. Es sólo una red de seguridad contra picos
 *             bajos: el que decide si algo es una cima es {@code minProminence}
 * @param minProminence cuánto tiene que sobresalir la cima sobre la media del terreno de alrededor.
 *                      Sin esto, un prado alto pasa el filtro de altura y no es un pico — y además
 *                      tallar en mitad de una ladera deja un socavón cuadrado bastante feo
 * @param maxNeighbourRise cuánto puede subir el terreno del anillo que rodea la huella por encima
 *                         de la plataforma. El corte se detiene en el borde de la huella, así que
 *                         lo que asome justo fuera se queda de pie: son los muros y agujas que
 *                         quedan pegados al borde. Descartar el sitio es la única forma de
 *                         evitarlos sin tallar más monte del que hace falta. <b>Es el parámetro
 *                         que hay que aflojar si las cimas válidas salen demasiado raras</b>
 * @param maxCarve cuántos bloques se puede rebajar desde la cima antes de rendirse
 * @param solidRatio qué fracción de la huella tiene que ser roca para dar la altura por buena.
 *                   Por debajo de 1 se le permite al nido volar por algún borde, que además de
 *                   multiplicar los sitios válidos queda mejor
 * @param debug vuelca al log cada decisión de emplazamiento, aceptada o rechazada, con sus números
 */
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
