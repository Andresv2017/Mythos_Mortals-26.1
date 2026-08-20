package net.darkblade.mythosmortals.content.structure;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Una estructura de pieza única a partir de una plantilla con marcadores. Qué plantilla, a qué
 * profundidad, y si va buscando cimas son campos del JSON, así que añadir una estructura nueva es
 * un archivo de datos y no una clase más:
 * <pre>
 *   { "type": "deluxelib:marked_template", "template": "deluxelib:minotaur_arena",
 *     "depth_below_surface": 35, ... }
 * </pre>
 * <br>
 * No extiende {@code SinglePieceStructure}, que es la que uno esperaría reutilizar: su
 * {@code PieceConstructor} sólo recibe {@code (random, x, z)} y nunca el
 * {@code StructureTemplateManager}, así que no puede construir una pieza de plantilla. Por eso los
 * que la usan en vanilla —tesoro enterrado, cabaña de bruja, pirámide— construyen sus piezas bloque
 * a bloque en Java.
 */
public class MarkedTemplateStructure extends Structure {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final MapCodec<MarkedTemplateStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        settingsCodec(i),
        Identifier.CODEC.fieldOf("template").forGetter(s -> s.template),
        Codec.INT.optionalFieldOf("depth_below_surface", 0).forGetter(s -> s.depthBelowSurface),
        PeakSiting.CODEC.optionalFieldOf("peak_siting").forGetter(s -> s.peakSiting),
        Codec.BOOL.optionalFieldOf("ignore_air", false).forGetter(s -> s.ignoreAir),
        Codec.INT.optionalFieldOf("sink", 0).forGetter(s -> s.sink),
        Codec.BOOL.optionalFieldOf("clear_vegetation", false).forGetter(s -> s.clearVegetation),
        Codec.BOOL.optionalFieldOf("clear_above", false).forGetter(s -> s.clearAbove),
        Codec.BOOL.optionalFieldOf("fill_below", false).forGetter(s -> s.fillBelow),
        Codec.INT.optionalFieldOf("max_slope", 0).forGetter(s -> s.maxSlope),
        Codec.BOOL.optionalFieldOf("avoid_water", false).forGetter(s -> s.avoidWater),
        Codec.BOOL.optionalFieldOf("debug", false).forGetter(s -> s.debug)
    ).apply(i, MarkedTemplateStructure::new));

    /** Paso de la rejilla con la que se busca la columna más alta dentro del chunk. */
    private static final int CHUNK_SCAN_STEP = 5;

    /** Sondeos por lado sobre la huella. Vanilla usa 4 columnas en total en {@code getLowestY};
     * 25 es seis veces más caro pero es lo mínimo para saber si hay meseta o sólo una aguja. */
    private static final int FOOTPRINT_SAMPLES = 5;

    /** Margen que se limpia por encima de la cima muestreada, por si entre sondeo y sondeo quedaba
     * algún saliente más alto. */
    private static final int CARVE_HEADROOM = 4;

    /** Radio del anillo con el que se mide la prominencia, y cuántos puntos se sondean en él. */
    private static final int PROMINENCE_RADIUS = 40;
    private static final int PROMINENCE_SAMPLES = 8;

    /** Cuánto puede subir el terreno entre la columna más alta del chunk y el borde de la huella.
     * Se usa para descartar pronto sin sondear la huella entera; con un margen generoso, la salida
     * temprana sólo se dispara donde no había ninguna posibilidad. */
    private static final int EARLY_OUT_MARGIN = 16;

    /** Alturas, relativas a {@code min_y}, a las que se sondea el bioma antes de tocar el terreno.
     * Varias porque el bioma es tridimensional: sobre una montaña, a ras de {@code min_y} todavía
     * puede haber grove o meadow y sólo más arriba aparece el pico. */
    private static final int[] BIOME_PROBE_OFFSETS = {0, 40, 80};

    /** A cuántos bloques del borde de la huella se sondea el anillo de guarda. Pegado al borde
     * daría el mismo terreno que ya mide la huella; muy lejos mediría monte que no molesta. */
    private static final int GUARD_RING_GAP = 3;

    private final Identifier template;

    /** Cuánto por debajo del terreno se entierra el suelo de la estructura. {@code 0} la deja
     * apoyada en la superficie. */
    private final int depthBelowSurface;

    private final Optional<PeakSiting> peakSiting;

    /** Si el aire de la plantilla se descarta al colocarla. Para unas ruinas dispersas, donde casi
     * todo el volumen es aire, colocarlo significa excavar la caja entera en el terreno. */
    private final boolean ignoreAir;

    /**
     * Cuántas capas de la plantilla quedan por debajo de la superficie, para las que se apoyan en
     * el suelo. Con {@code 0} la capa {@code y=0} sustituye al bloque de superficie y la estructura
     * queda a ras; con {@code 1} esa capa queda enterrada y es la {@code y=1} la que aflora.
     * <br><br>
     * No es lo mismo que {@link #depthBelowSurface}, que entierra la estructura <b>entera</b> y
     * descarta el sitio si no cabe bajo el terreno. Esto sólo la hunde unos bloques.
     */
    private final int sink;

    /** Si se arrasa la vegetación de la huella antes de colocar la plantilla. Con
     * {@code terrain_adaptation: none} nadie limpia el terreno, así que un árbol nacido dentro
     * de la huella atraviesa la estructura. */
    private final boolean clearVegetation;

    /** Si se despeja el terreno que quede por encima del suelo de la estructura, a una única
     * altura para toda la huella. Es lo que impide que la tierra se meta dentro de las ruinas
     * donde la plantilla no pone nada y {@code ignore_air} deja el mundo intacto. */
    private final boolean clearAbove;

    /**
     * Si se rellenan con tierra los huecos que queden por debajo del suelo de la estructura. El
     * emplazamiento mide la altura en el centro de la huella, así que el terreno que baje hacia un
     * lado deja un escalón que el despeje no puede tapar, porque el despeje sólo quita.
     */
    private final boolean fillBelow;

    /**
     * Desnivel máximo tolerado, en bloques, entre el punto más alto y el más bajo del terreno bajo
     * la huella. {@code 0} desactiva la comprobación. Sin esto una construcción plana de veintitantos
     * bloques de lado acaba en la primera ladera que le toque y se come media colina.
     */
    private final int maxSlope;

    /**
     * Si se descarta el sitio cuando hay agua sobre alguno de los puntos sondeados. Hace falta
     * porque la altura de superficie que usa vanilla para emplazar ({@code WORLD_SURFACE_WG}) mide
     * con el predicado <i>no es aire</i>, y el agua no es aire: sobre un lago devuelve la lámina,
     * que además de estar en el sitio equivocado es perfectamente plana y engaña a {@link #maxSlope}.
     */
    private final boolean avoidWater;

    /** Vuelca al log cada decisión de emplazamiento en superficie, aceptada o rechazada. */
    private final boolean debug;

    public MarkedTemplateStructure(Structure.StructureSettings settings, Identifier template, int depthBelowSurface, Optional<PeakSiting> peakSiting, boolean ignoreAir, int sink, boolean clearVegetation, boolean clearAbove, boolean fillBelow, int maxSlope, boolean avoidWater, boolean debug) {
        super(settings);
        this.template = template;
        this.depthBelowSurface = depthBelowSurface;
        this.peakSiting = peakSiting;
        this.ignoreAir = ignoreAir;
        this.sink = sink;
        this.clearVegetation = clearVegetation;
        this.clearAbove = clearAbove;
        this.fillBelow = fillBelow;
        this.maxSlope = maxSlope;
        this.avoidWater = avoidWater;
        this.debug = debug;
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        Vec3i size = context.structureTemplateManager().getOrCreate(this.template).getSize();
        return this.peakSiting.isPresent()
            ? this.findPeak(context, size, this.peakSiting.get())
            : this.findFlat(context, size);
    }

    // -----------------------------------------------------------------------
    // Emplazamiento normal: centro del chunk, enterrado o en superficie
    // -----------------------------------------------------------------------
    private Optional<Structure.GenerationStub> findFlat(Structure.GenerationContext context, Vec3i size) {
        ChunkPos chunkPos = context.chunkPos();
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();

        int floorY;
        if (this.depthBelowSurface > 0) {
            // Enterrada: se ancla a la esquina más baja de la huella, que es la conservadora. Con la
            // altura del centro, una estructura bajo una ladera podría asomar por el lado bajo.
            int surface = getLowestY(context, size.getX(), size.getZ());
            floorY = surface - this.depthBelowSurface;

            // Que quepa entera entre el fondo del mundo y el terreno; si no, se salta esta posición
            // en vez de colocar algo medio asomado o recortado por abajo.
            int worldFloor = context.heightAccessor().getMinY() + 1;
            if (floorY < worldFloor || floorY + size.getY() >= surface) {
                return Optional.empty();
            }
        } else {
            int surface = surfaceAt(context, centerX, centerZ);

            // Descartar las laderas antes de nada. Se sondea un anillo de ocho puntos en el borde de
            // la huella; si entre el más alto y el más bajo hay más desnivel del tolerado, este sitio
            // no vale y ya lo intentará en el siguiente.
            if (this.maxSlope > 0 || this.avoidWater) {
                // Mismo truco que en las cimas: vanilla filtra el bioma DESPUÉS de elegir el sitio,
                // así que sin esto un chunk de océano paga los ocho sondeos del anillo para acabar
                // descartado igual. Sondear el bioma cuesta una fracción de una columna de ruido.
                if (!probeBiome(context, centerX, centerZ, surface)) {
                    return Optional.empty();
                }

                // El agua, en el centro y antes que nada: es el descarte más barato de los dos y el
                // que más candidatos se lleva por delante en un bioma con lagos.
                if (this.avoidWater) {
                    int bottom = groundAt(context, centerX, centerZ);
                    if (bottom < surface) {
                        if (this.debug) {
                            LOGGER.info("[flat] {} RECHAZADO en {}: agua en el centro (fondo y={}, lámina y={})",
                                this.template, chunkPos, bottom, surface);
                        }
                        return Optional.empty();
                    }
                }

                int span = Math.max(size.getX(), size.getZ()) / 2;
                int lowest = surface;
                int highest = surface;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) {
                            continue;
                        }
                        int ringX = centerX + dx * span;
                        int ringZ = centerZ + dz * span;
                        int corner = surfaceAt(context, ringX, ringZ);

                        // Media estructura en tierra y la otra media metida en el lago es justo lo
                        // que sale si sólo se mira el centro, así que el anillo también cuenta.
                        if (this.avoidWater && groundAt(context, ringX, ringZ) < corner) {
                            if (this.debug) {
                                LOGGER.info("[flat] {} RECHAZADO en {}: agua en el borde de la huella ({},{})",
                                    this.template, chunkPos, ringX, ringZ);
                            }
                            return Optional.empty();
                        }

                        lowest = Math.min(lowest, corner);
                        highest = Math.max(highest, corner);
                    }
                }

                int slope = highest - lowest;
                if (this.maxSlope > 0 && slope > this.maxSlope) {
                    if (this.debug) {
                        LOGGER.info("[flat] {} RECHAZADO en {}: desnivel {} > {} (terreno entre y={} e y={})",
                            this.template, chunkPos, slope, this.maxSlope, lowest, highest);
                    }
                    return Optional.empty();
                }
                if (this.debug) {
                    LOGGER.info("[flat] {} ACEPTADO en {}: desnivel {}, suelo y={}, centro {},{}",
                        this.template, chunkPos, slope, surface - this.sink, centerX, centerZ);
                }
            }

            // surfaceAt devuelve la Y del bloque sólido más alto, así que con sink=0 la capa y=0 de
            // la plantilla cae justo encima de él y lo sustituye. Cada unidad de sink baja eso un
            // bloque más, que es como se entierran los cimientos de unas ruinas.
            floorY = surface - this.sink;
        }

        return Optional.of(this.stub(context, size, centerX, centerZ, floorY, 0));
    }

    // -----------------------------------------------------------------------
    // Emplazamiento en cima: buscar el punto alto, y rebajarlo hasta que quepa
    // -----------------------------------------------------------------------
    private Optional<Structure.GenerationStub> findPeak(Structure.GenerationContext context, Vec3i size, PeakSiting siting) {
        ChunkPos chunkPos = context.chunkPos();

        // 1. Pre-filtro de bioma, antes de tocar el terreno. Vanilla valida el bioma DESPUÉS
        //    —findGenerationPoint(...).filter(isValidBiome)— así que sin esto se sondea el terreno
        //    del mundo entero para luego tirar el resultado. Una muestra de clima cuesta una
        //    fracción de un sondeo de altura, que calcula una columna de ruido completa.
        //
        //    Es una aproximación: se sondea el centro del chunk a alturas fijas, no la posición
        //    exacta que acabará teniendo la estructura. Puede perder alguna cima en el borde de un
        //    bioma. Nunca puede aceptar de más: el filtro de vanilla sigue corriendo al final.
        if (!probeBiome(context, chunkPos.getMiddleBlockX(), chunkPos.getMiddleBlockZ(), siting.minY())) {
            return Optional.empty();
        }

        // 2. La columna más alta del chunk. Plantar en el centro del chunk sin mirar es la razón de
        //    que, en una cordillera, la mitad de los candidatos caigan en un valle.
        int peakX = chunkPos.getMiddleBlockX();
        int peakZ = chunkPos.getMiddleBlockZ();
        int peakY = Integer.MIN_VALUE;
        for (int offsetX = 1; offsetX < 16; offsetX += CHUNK_SCAN_STEP) {
            for (int offsetZ = 1; offsetZ < 16; offsetZ += CHUNK_SCAN_STEP) {
                int x = chunkPos.getMinBlockX() + offsetX;
                int z = chunkPos.getMinBlockZ() + offsetZ;
                int y = surfaceAt(context, x, z);
                if (y > peakY) {
                    peakY = y;
                    peakX = x;
                    peakZ = z;
                }
            }
        }

        // 3. Salida temprana. findGenerationPoint corre ANTES que la validación de bioma —vanilla
        //    hace findGenerationPoint(...).filter(isValidBiome)— así que esto se ejecuta en cada
        //    chunk candidato del mundo, océanos incluidos. Descartar aquí, con 9 sondeos hechos y
        //    sin tocar los 33 que quedan, es de donde sale casi todo el ahorro.
        if (peakY + EARLY_OUT_MARGIN < siting.minY()) {
            if (siting.debug()) {
                LOGGER.info("[peak] {} RECHAZADO en {}: terreno a y={}, muy por debajo de min_y={} (salida temprana)",
                    this.template, chunkPos, peakY, siting.minY());
            }
            return Optional.empty();
        }

        // 4. Prominencia: cuánto sobresale la cima sobre lo que tiene alrededor. Es lo que separa un
        //    pico de un prado alto, y de paso evita tallar en mitad de una ladera, que deja un
        //    socavón cuadrado. Va antes que el muestreo de la huella porque es más barato.
        long ringTotal = 0;
        for (int i = 0; i < PROMINENCE_SAMPLES; i++) {
            double angle = 2.0 * Math.PI * i / PROMINENCE_SAMPLES;
            int x = peakX + (int) Math.round(Math.cos(angle) * PROMINENCE_RADIUS);
            int z = peakZ + (int) Math.round(Math.sin(angle) * PROMINENCE_RADIUS);
            ringTotal += surfaceAt(context, x, z);
        }
        int ringMean = (int) (ringTotal / PROMINENCE_SAMPLES);
        int prominence = peakY - ringMean;

        if (prominence < siting.minProminence()) {
            if (siting.debug()) {
                LOGGER.info("[peak] {} RECHAZADO en {}: prominencia {} < {} (cima y={}, entorno y={})",
                    this.template, chunkPos, prominence, siting.minProminence(), peakY, ringMean);
            }
            return Optional.empty();
        }

        // 5. Perfil del terreno sobre la huella, centrada en esa columna.
        int[] heights = new int[FOOTPRINT_SAMPLES * FOOTPRINT_SAMPLES];
        int halfX = size.getX() / 2;
        int halfZ = size.getZ() / 2;
        int highest = Integer.MIN_VALUE;
        int index = 0;
        for (int sx = 0; sx < FOOTPRINT_SAMPLES; sx++) {
            for (int sz = 0; sz < FOOTPRINT_SAMPLES; sz++) {
                int x = peakX - halfX + (size.getX() - 1) * sx / (FOOTPRINT_SAMPLES - 1);
                int z = peakZ - halfZ + (size.getZ() - 1) * sz / (FOOTPRINT_SAMPLES - 1);
                int y = surfaceAt(context, x, z);
                heights[index++] = y;
                highest = Math.max(highest, y);
            }
        }

        // 6. Bajar desde la cima hasta encontrar una altura con roca suficiente bajo la huella.
        int required = (int) Math.ceil(siting.solidRatio() * heights.length);
        int floorY = Integer.MIN_VALUE;
        int solidAtFloor = 0;
        for (int y = highest; y >= highest - siting.maxCarve(); y--) {
            int solid = 0;
            for (int h : heights) {
                if (h >= y) {
                    solid++;
                }
            }
            if (solid >= required) {
                floorY = y;
                solidAtFloor = solid;
                break;
            }
        }

        if (floorY == Integer.MIN_VALUE) {
            if (siting.debug()) {
                LOGGER.info("[peak] {} RECHAZADO en {}: sin solidez del {}% en {} bloques de corte desde y={} (pico en {},{})",
                    this.template, chunkPos, Math.round(siting.solidRatio() * 100), siting.maxCarve(), highest, peakX, peakZ);
            }
            return Optional.empty();
        }

        if (floorY < siting.minY()) {
            if (siting.debug()) {
                LOGGER.info("[peak] {} RECHAZADO en {}: plataforma a y={} por debajo de min_y={} (pico en {},{} a y={})",
                    this.template, chunkPos, floorY, siting.minY(), peakX, peakZ, highest);
            }
            return Optional.empty();
        }

        // 7. Anillo de guarda: qué tan alto está el terreno justo fuera de la huella. El corte se
        //    detiene en el borde, así que un muro que asome un metro más allá se queda de pie
        //    pegado al nido. Va el último porque sólo lo pagan los candidatos que ya pasaron todo.
        int neighbourRise = guardRingTop(context, size, peakX, peakZ) - floorY;
        if (neighbourRise > siting.maxNeighbourRise()) {
            if (siting.debug()) {
                LOGGER.info("[peak] {} RECHAZADO en {}: el entorno sube {} sobre la plataforma, máximo {} (plataforma y={}, pico en {},{})",
                    this.template, chunkPos, neighbourRise, siting.maxNeighbourRise(), floorY, peakX, peakZ);
            }
            return Optional.empty();
        }

        if (siting.debug()) {
            LOGGER.info("[peak] {} ACEPTADO en {}: plataforma y={} bajo pico y={} (corte {}), prominencia {}, solidez {}/{}, entorno {} sobre la plataforma, centro {},{}",
                this.template, chunkPos, floorY, highest, highest - floorY, prominence, solidAtFloor, heights.length, neighbourRise, peakX, peakZ);
        }

        return Optional.of(this.stub(context, size, peakX, peakZ, floorY, highest + CARVE_HEADROOM));
    }

    // -----------------------------------------------------------------------

    private Structure.GenerationStub stub(Structure.GenerationContext context, Vec3i size, int centerX, int centerZ, int floorY, int carveTop) {
        Rotation rotation = Rotation.getRandom(context.random());

        // El suelo real de la estructura: la capa que se pisa. floorY ya lleva el sink restado, así
        // que sumárselo devuelve la superficie original, que es donde apoya la plantilla.
        int groundY = this.clearAbove || this.fillBelow ? floorY + this.sink : Integer.MAX_VALUE;
        BlockPos origin = new BlockPos(centerX - size.getX() / 2, floorY, centerZ - size.getZ() / 2);

        // La posición del stub es la que vanilla usa para validar el bioma, y lo hace *con su Y*
        // (Structure#isValidBiome muestrea con QuartPos.fromBlock(startPos.getY())). Para una
        // estructura enterrada eso significa que el tag tiene que contemplar biomas de cueva.
        return new Structure.GenerationStub(
            new BlockPos(centerX, floorY, centerZ),
            builder -> builder.addPiece(new MarkedStructurePiece(context.structureTemplateManager(), this.template, origin, rotation, carveTop, this.ignoreAir, this.clearVegetation, groundY, this.clearAbove, this.fillBelow))
        );
    }

    /**
     * Si el bioma de esta columna, a alguna de las alturas de sondeo, es de los que admite la
     * estructura. Réplica deliberada de {@code Structure#isValidBiome}, que es privado: usa el
     * mismo {@code BiomeSource} y el mismo predicado, para que lo que se acepte aquí no lo tire
     * después el filtro de vanilla por muestrear de otra forma.
     */
    private static boolean probeBiome(Structure.GenerationContext context, int x, int z, int minY) {
        int quartX = QuartPos.fromBlock(x);
        int quartZ = QuartPos.fromBlock(z);
        int ceiling = context.heightAccessor().getMaxY();
        for (int offset : BIOME_PROBE_OFFSETS) {
            int quartY = QuartPos.fromBlock(Math.min(minY + offset, ceiling));
            if (context.validBiome().test(
                    context.chunkGenerator().getBiomeSource().getNoiseBiome(quartX, quartY, quartZ, context.randomState().sampler()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * La columna más alta de un anillo de ocho puntos —cuatro lados y cuatro esquinas— alrededor de
     * la huella. Se usa {@code max} de los dos lados de la plantilla porque la rotación se sortea
     * después: así el anillo cae fuera de la huella con cualquiera de las cuatro.
     */
    private static int guardRingTop(Structure.GenerationContext context, Vec3i size, int centerX, int centerZ) {
        int radius = Math.max(size.getX(), size.getZ()) / 2 + GUARD_RING_GAP;
        int highest = Integer.MIN_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                highest = Math.max(highest, surfaceAt(context, centerX + dx * radius, centerZ + dz * radius));
            }
        }
        return highest;
    }

    private static int surfaceAt(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(
            x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
    }

    /**
     * La Y del último bloque que <b>frena</b> al caer, o sea el suelo de verdad.
     * <br><br>
     * Es lo mismo que {@link #surfaceAt} salvo por el predicado: {@code WORLD_SURFACE_WG} usa
     * <i>no es aire</i>, que el agua cumple, y {@code OCEAN_FLOOR_WG} usa {@code blocksMotion()},
     * que no. Sobre tierra seca las dos devuelven el mismo número; donde no coinciden, la diferencia
     * es exactamente la columna de fluido — que es como se detecta el agua sin leer un solo bloque
     * del mundo, que a estas alturas de la generación todavía no existe.
     */
    private static int groundAt(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(
            x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
    }

    @Override
    public StructureType<?> type() {
        return MythosMortalsRegistry.MARKED_TEMPLATE_STRUCTURE.get();
    }

}
