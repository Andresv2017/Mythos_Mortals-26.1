package net.darkblade.mythosmortals.content.structure;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import com.mojang.logging.LogUtils;
import net.darkblade.deluxelib.entity.IArmoredEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;

/**
 * La pieza de cualquier plantilla con marcadores: coloca el {@code .nbt} y procesa sus structure
 * blocks en modo Data.
 * <br><br>
 * No hay una pieza por estructura. {@code TemplateStructurePiece} ya guarda el nombre de la
 * plantilla en el NBT del chunk y la reconstruye al cargar, así que una sola clase —y un solo
 * {@code StructurePieceType} registrado— sirve para la arena, el nido y lo que venga después.
 * <br><br>
 * Todo el despacho de marcadores es de vanilla: {@code postProcess} recorre los structure blocks de
 * la plantilla y llama a {@link #handleDataMarker} sólo para los que están en modo {@code DATA}.
 */
public class MarkedStructurePiece extends TemplateStructurePiece {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Y hasta la que se limpia el terreno por encima de la estructura, o {@code 0} si no se talla.
     * Lo decide {@link MarkedTemplateStructure} al elegir el emplazamiento. */
    private final int carveTop;

    /** Si el aire de la plantilla se descarta en vez de colocarse. Lo decide el JSON de la
     * estructura; hace falta guardarlo porque los procesadores se reconstruyen al recargar. */
    private final boolean ignoreAir;

    /** Si se arrasa la vegetación de la huella antes de colocar la plantilla. */
    private final boolean clearVegetation;

    /** Y del mundo del suelo de la estructura: la capa que se pisa, la que la plantilla usa de
     * base. {@link Integer#MAX_VALUE} si no hay que tocar el terreno. La calcula
     * {@link MarkedTemplateStructure} al elegir el emplazamiento, porque es la única que sabe el
     * {@code sink}. Es el único número del que dependen las tres pasadas de terreno. */
    private final int groundY;

    /** Si se despeja el terreno por encima del suelo, y el del propio suelo bajo lo que la
     * plantilla cubra. */
    private final boolean clearAbove;

    /** Si se rellenan con tierra los huecos por debajo del suelo. */
    private final boolean fillBelow;

    /** Cuánto por encima de la estructura se sigue limpiando. Un abedul ronda los 7 de alto, así
     * que con esto se va también la copa de un árbol nacido dentro de la huella. */
    private static final int CLEAR_HEADROOM = 10;

    /** Hasta cuántos bloques por debajo del suelo se rellena. Con un filtro de pendiente razonable
     * los huecos son de dos o tres bloques; el tope está para que un barranco que se cuele en la
     * huella no acabe rellenado hasta la roca madre. */
    private static final int FILL_LIMIT = 16;

    public MarkedStructurePiece(StructureTemplateManager structureTemplateManager, Identifier template, BlockPos position, Rotation rotation, int carveTop, boolean ignoreAir, boolean clearVegetation, int groundY, boolean clearAbove, boolean fillBelow) {
        super(
            MythosMortalsRegistry.MARKED_STRUCTURE_PIECE.get(),
            0,
            structureTemplateManager,
            template,
            template.toString(),
            makeSettings(structureTemplateManager, template, rotation, ignoreAir),
            position
        );
        this.carveTop = carveTop;
        this.ignoreAir = ignoreAir;
        this.clearVegetation = clearVegetation;
        this.groundY = groundY;
        this.clearAbove = clearAbove;
        this.fillBelow = fillBelow;
    }

    /** Constructor de deserialización. Sin él la pieza no sobrevive a guardar y recargar el mundo. */
    public MarkedStructurePiece(StructureTemplateManager structureTemplateManager, CompoundTag tag) {
        super(
            MythosMortalsRegistry.MARKED_STRUCTURE_PIECE.get(),
            tag,
            structureTemplateManager,
            location -> makeSettings(structureTemplateManager, location,
                tag.read("Rot", Rotation.LEGACY_CODEC).orElseThrow(), tag.getBooleanOr("IgnoreAir", false))
        );
        this.carveTop = tag.getIntOr("CarveTop", 0);
        this.ignoreAir = tag.getBooleanOr("IgnoreAir", false);
        this.clearVegetation = tag.getBooleanOr("ClearVegetation", false);
        this.groundY = tag.getIntOr("GroundY", Integer.MAX_VALUE);
        this.clearAbove = tag.getBooleanOr("ClearAbove", false);
        this.fillBelow = tag.getBooleanOr("FillBelow", false);
    }

    /**
     * Pivote en el centro de la huella: así la caja resultante es la misma en las cuatro rotaciones
     * y las posiciones dentro de la plantilla son predecibles.
     * <br><br>
     * {@code BlockIgnoreProcessor.STRUCTURE_BLOCK} evita que los marcadores se coloquen como bloque
     * real. Ninguno de los dos procesadores afecta al despacho de marcadores: {@code filterBlocks}
     * lee la paleta de la plantilla y no consulta {@code settings.getProcessors()}.
     * <br><br>
     * Con {@code ignoreAir} se descarta además el aire. Un edificio hueco —la arena, el nido— tiene
     * que colocarlo para vaciar su interior. Unas ruinas dispersas no: son un puñado de bloques
     * dentro de una caja casi vacía, y colocar ese aire excavaría la caja entera en el terreno.
     */
    private static StructurePlaceSettings makeSettings(StructureTemplateManager structureTemplateManager, Identifier template, Rotation rotation, boolean ignoreAir) {
        Vec3i size = structureTemplateManager.getOrCreate(template).getSize();
        return new StructurePlaceSettings()
            .setRotation(rotation)
            .setMirror(Mirror.NONE)
            .setRotationPivot(new BlockPos(size.getX() / 2, 0, size.getZ() / 2))
            .addProcessor(ignoreAir ? BlockIgnoreProcessor.STRUCTURE_AND_AIR : BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
        tag.putInt("CarveTop", this.carveTop);
        tag.putBoolean("IgnoreAir", this.ignoreAir);
        tag.putBoolean("ClearVegetation", this.clearVegetation);
        tag.putInt("GroundY", this.groundY);
        tag.putBoolean("ClearAbove", this.clearAbove);
        tag.putBoolean("FillBelow", this.fillBelow);
    }

    /**
     * Prepara el terreno, coloca la plantilla y remata el interior. El orden importa y no es
     * intercambiable:
     * <ol>
     *   <li>{@link #carvePeak} rebaja la cima. Sin esto una estructura emplazada con
     *       {@code peak_siting} quedaría dentro de la roca: la altura se elige buscando piedra bajo
     *       la huella, así que por encima también la hay.</li>
     *   <li>{@link #razeVegetation} quita árboles y matojos, que si no atraviesan los muros.</li>
     *   <li>{@link #clearAboveBlocks} baja el terreno hasta el suelo de la estructura.</li>
     *   <li>{@link #fillGround} tapa los hoyos que queden justo debajo. Va después del despeje
     *       porque el despeje deja al descubierto el fondo de los hoyos que hay que rellenar.</li>
     *   <li>{@link #sampleGround} fotografía el suelo, antes de colocar nada.</li>
     *   <li>La plantilla, vía {@code super}.</li>
     *   <li>{@link #carveUnderCover} vacía el suelo bajo lo que la plantilla haya techado. Tiene que
     *       ir al final, porque hasta que la plantilla no está colocada no se sabe qué columnas tapa.</li>
     * </ol>
     * Corre una vez por chunk, igual que la colocación de bloques, así que todas las pasadas se
     * recortan a la parte de la huella que cae en {@code chunkBB}.
     */
    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
        if (this.carveTop > 0) {
            this.carvePeak(level, chunkBB);
        }
        if (this.clearVegetation) {
            this.razeVegetation(level, chunkBB);
        }

        BoundingBox area = this.groundY == Integer.MAX_VALUE ? null : this.footprintIn(chunkBB);
        BlockState[] beforePlacing = null;
        if (area != null) {
            if (this.clearAbove) {
                this.clearAboveBlocks(level, area);
            }
            if (this.fillBelow) {
                this.fillGround(level, area);
            }
            if (this.clearAbove) {
                // Hay que fotografiar el suelo ANTES de colocar, porque después no hay forma de
                // distinguir un bloque de la plantilla de uno del terreno mirando el mundo.
                beforePlacing = this.sampleGround(level, area);
            }
        }

        super.postProcess(level, structureManager, generator, random, chunkBB, chunkPos, referencePos);

        if (beforePlacing != null) {
            this.carveUnderCover(level, area, beforePlacing);
        }
    }

    /**
     * La parte de la huella que cae en este chunk, o {@code null} si no cae ninguna. Las tres
     * pasadas de terreno tienen que recorrer exactamente las mismas columnas para que la foto del
     * suelo y el tallado posterior se indexen igual.
     */
    private BoundingBox footprintIn(BoundingBox chunkBB) {
        BoundingBox footprint = this.getBoundingBox();
        int minX = Math.max(footprint.minX(), chunkBB.minX());
        int maxX = Math.min(footprint.maxX(), chunkBB.maxX());
        int minZ = Math.max(footprint.minZ(), chunkBB.minZ());
        int maxZ = Math.min(footprint.maxZ(), chunkBB.maxZ());
        if (minX > maxX || minZ > maxZ) {
            return null;
        }
        return new BoundingBox(minX, footprint.minY(), minZ, maxX, footprint.maxY(), maxZ);
    }

    /**
     * Rellena con tierra los huecos que queden bajo el suelo de la estructura, para que no se quede
     * nada colgando cuando el terreno baja hacia un lado. El emplazamiento mide la altura en el
     * centro de la huella, así que el despeje —que sólo quita— no puede tapar un escalón hacia abajo.
     * <br><br>
     * Se rellena hasta <b>una capa por debajo</b> del suelo, no hasta el suelo. Rematar arriba
     * levanta la explanada un bloque sobre el terreno de justo al lado y deja un escalón alrededor
     * de toda la huella, que se ve peor que el desnivel que venía a arreglar. Así se tapan los hoyos
     * de verdad —los que dejarían un edificio en el aire— y el relieve suave se queda como estaba.
     */
    private void fillGround(WorldGenLevel level, BoundingBox area) {
        int top = this.groundY - 1;
        int bottom = Math.max(top - FILL_LIMIT, level.getMinY());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int filled = 0;

        for (int x = area.minX(); x <= area.maxX(); x++) {
            for (int z = area.minZ(); z <= area.maxZ(); z++) {
                for (int y = top; y > bottom; y--) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && state.getFluidState().isEmpty()) {
                        break;
                    }
                    level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
                    filled++;
                }
            }
        }

        if (filled > 0) {
            LOGGER.info("[fill] {}: rellenados {} bloques hasta y={} en x[{}..{}] z[{}..{}]",
                this.templateName, filled, top, area.minX(), area.maxX(), area.minZ(), area.maxZ());
        }
    }

    /** El estado de cada columna a la altura del suelo, en el orden que recorre {@link #carveUnderCover}. */
    private BlockState[] sampleGround(WorldGenLevel level, BoundingBox area) {
        int width = area.maxZ() - area.minZ() + 1;
        BlockState[] states = new BlockState[(area.maxX() - area.minX() + 1) * width];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = area.minX(); x <= area.maxX(); x++) {
            for (int z = area.minZ(); z <= area.maxZ(); z++) {
                pos.set(x, this.groundY, z);
                states[(x - area.minX()) * width + (z - area.minZ())] = level.getBlockState(pos);
            }
        }
        return states;
    }

    /**
     * Vacía el terreno que quede a la altura del suelo <b>dentro</b> de los edificios.
     * <br><br>
     * El despeje de arriba se para una capa por encima del suelo a propósito, porque esa capa es el
     * césped de la explanada y quitarlo entero deja un descampado de tierra. Pero dentro de un
     * edificio esa misma capa es tierra metida donde no debe.
     * <br><br>
     * Lo que distingue un caso del otro es estar <b>rodeado</b>, no estar tapado. Con sólo mirar si
     * hay algo de la plantilla por encima, un alero o un banderín que sobresalga del muro convierte
     * en «interior» la franja de fuera y abre una zanja alrededor de todo el edificio. Exigir que
     * las cuatro columnas vecinas estén también tapadas deja fuera esa franja —por el lado de
     * fuera no hay nada— y deja fuera también los vanos de entrada, que es lo que se quiere.
     * <br><br>
     * Y no se toca una columna donde la plantilla haya puesto su propio suelo, que es justo lo que
     * detecta comparar con la foto de antes de colocar: si el bloque cambió, lo puso la plantilla.
     * Los marcadores Data no cuentan como techo — {@code handleDataMarker} los deja en aire antes de
     * llegar aquí—, así que un mob marcado sigue apareciendo de pie sobre la hierba.
     */
    private void carveUnderCover(WorldGenLevel level, BoundingBox area, BlockState[] beforePlacing) {
        int width = area.maxZ() - area.minZ() + 1;
        int depth = area.maxX() - area.minX() + 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        boolean[] covered = new boolean[depth * width];
        for (int x = area.minX(); x <= area.maxX(); x++) {
            for (int z = area.minZ(); z <= area.maxZ(); z++) {
                boolean any = false;
                for (int y = this.groundY + 1; y <= area.maxY() && !any; y++) {
                    pos.set(x, y, z);
                    any = !level.getBlockState(pos).isAir();
                }
                covered[(x - area.minX()) * width + (z - area.minZ())] = any;
            }
        }

        // El borde se salta entero: una columna del borde no tiene los cuatro vecinos disponibles,
        // y en el borde de chunk los que faltan pertenecen a un trozo de estructura que todavía no
        // se ha colocado. Quedarse corto deja como mucho un bloque de hierba dentro de un edificio;
        // pasarse abre una zanja.
        int carved = 0;
        for (int x = area.minX() + 1; x < area.maxX(); x++) {
            for (int z = area.minZ() + 1; z < area.maxZ(); z++) {
                int i = (x - area.minX()) * width + (z - area.minZ());
                if (!covered[i] || !covered[i - width] || !covered[i + width] || !covered[i - 1] || !covered[i + 1]) {
                    continue;
                }

                pos.set(x, this.groundY, z);
                BlockState ground = level.getBlockState(pos);
                if (ground.isAir() || ground != beforePlacing[i]) {
                    continue;
                }

                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                carved++;
            }
        }

        if (carved > 0) {
            LOGGER.info("[interior] {}: vaciados {} bloques de terreno a y={} en x[{}..{}] z[{}..{}]",
                this.templateName, carved, this.groundY, area.minX(), area.maxX(), area.minZ(), area.maxZ());
        }
    }

    /**
     * Baja el terreno de la huella hasta el suelo de la estructura.
     * <br><br>
     * Con {@code ignore_air} el aire de la plantilla no se coloca, así que donde la plantilla no
     * pone nada se queda el terreno del mundo. En suelo llano eso es aire y da igual, pero en cuanto
     * el terreno sube un poco, la tierra se mete dentro de las ruinas y las tapa.
     * <br><br>
     * El corte empieza una capa por encima del suelo y no baja más, así que el césped de la
     * explanada sobrevive entero. Lo que quede a la altura del suelo pero dentro de un edificio lo
     * resuelve después {@link #carveUnderCover}, que para entonces ya sabe qué columnas están
     * techadas.
     * <br><br>
     * Cortar una columna deja al aire lo que hubiera <b>debajo</b> de su superficie, que es tierra:
     * rebajar una explanada con dos palmos de desnivel la convierte entera en un descampado. Por eso
     * el bloque más alto que se quita se vuelve a poner abajo, a la altura del suelo. Se reutiliza
     * el de esa misma columna en vez de poner césped a secas para que la estructura también sirva
     * en arena, en nieve o en lo que traiga el bioma.
     * <br><br>
     * Una única altura para toda la huella, no una por columna. Tomar como referencia el bloque más
     * alto de cada columna deja el interior de un edificio lleno de tierra —la referencia acaba
     * siendo el tejado— y tomar el más bajo se lleva por delante la capa de césped, porque con
     * {@code sink} esa capa cae por debajo de la superficie.
     */
    private void clearAboveBlocks(WorldGenLevel level, BoundingBox area) {
        int ceiling = Math.min(area.maxY() + CLEAR_HEADROOM, level.getMaxY());
        int removed = 0;
        int recapped = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = area.minX(); x <= area.maxX(); x++) {
            for (int z = area.minZ(); z <= area.maxZ(); z++) {
                BlockState surface = null;

                for (int y = this.groundY + 1; y <= ceiling; y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    if (state.getFluidState().isEmpty()) {
                        // Se recorre hacia arriba, así que la última asignación es el bloque más
                        // alto de la columna: el que estaba haciendo de superficie.
                        surface = state;
                    }
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    removed++;
                }

                if (surface != null) {
                    pos.set(x, this.groundY, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, surface, 2);
                        recapped++;
                    }
                }
            }
        }

        if (removed > 0) {
            LOGGER.info("[clear] {}: despejados {} bloques entre y={} e y={}, {} superficies repuestas, en x[{}..{}] z[{}..{}]",
                this.templateName, removed, this.groundY + 1, ceiling, recapped, area.minX(), area.maxX(), area.minZ(), area.maxZ());
        }
    }

    /**
     * Arrasa la vegetación de la huella antes de colocar la plantilla.
     * <br><br>
     * Hace falta cuando la estructura no excava su hueco. Con {@code terrain_adaptation: none} y
     * {@code ignore_air} nadie limpia el terreno, así que un árbol nacido dentro de la huella
     * atraviesa las ruinas. Se limpia hasta {@value #CLEAR_HEADROOM} por encima para llevarse
     * también la copa.
     * <br><br>
     * Sólo quita vegetación: la tierra y la piedra se quedan, que es lo que sostiene la estructura.
     */
    private void razeVegetation(WorldGenLevel level, BoundingBox chunkBB) {
        BoundingBox footprint = this.getBoundingBox();
        int minX = Math.max(footprint.minX(), chunkBB.minX());
        int maxX = Math.min(footprint.maxX(), chunkBB.maxX());
        int minZ = Math.max(footprint.minZ(), chunkBB.minZ());
        int maxZ = Math.min(footprint.maxZ(), chunkBB.maxZ());
        if (minX > maxX || minZ > maxZ) {
            return;
        }

        int from = footprint.minY();
        int to = Math.min(footprint.maxY() + CLEAR_HEADROOM, level.getMaxY());
        int removed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = from; y <= to; y++) {
                    pos.set(x, y, z);
                    if (isVegetation(level.getBlockState(pos))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        removed++;
                    }
                }
            }
        }

        if (removed > 0) {
            LOGGER.info("[veg] {}: arrasados {} bloques de vegetación entre y={} e y={} en x[{}..{}] z[{}..{}]",
                this.templateName, removed, from, to, minX, maxX, minZ, maxZ);
        }
    }

    /**
     * El aire no cuenta —ya está limpio— y los fluidos tampoco: vaciar el agua de un lago dejaría
     * un socavón peor que el árbol.
     */
    private static boolean isVegetation(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.is(BlockTags.LOGS)
            || state.is(BlockTags.LEAVES)
            || state.is(BlockTags.SAPLINGS)
            || state.is(BlockTags.FLOWERS)
            || state.is(BlockTags.REPLACEABLE);
    }

    private void carvePeak(WorldGenLevel level, BoundingBox chunkBB) {
        BoundingBox footprint = this.getBoundingBox();
        int minX = Math.max(footprint.minX(), chunkBB.minX());
        int maxX = Math.min(footprint.maxX(), chunkBB.maxX());
        int minZ = Math.max(footprint.minZ(), chunkBB.minZ());
        int maxZ = Math.min(footprint.maxZ(), chunkBB.maxZ());
        if (minX > maxX || minZ > maxZ) {
            return;
        }

        int from = footprint.maxY() + 1;
        int to = Math.min(this.carveTop, level.getMaxY());
        int removed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = from; y <= to; y++) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        removed++;
                    }
                }
            }
        }

        if (removed > 0) {
            LOGGER.info("[peak] {}: rebajados {} bloques entre y={} e y={} en x[{}..{}] z[{}..{}]",
                this.templateName, removed, from, to, minX, maxX, minZ, maxZ);
        }
    }

    /**
     * La posición llega ya rotada y trasladada al mundo, así que no hay que deshacer la rotación a
     * mano.
     * <br><br>
     * El marcador se borra <b>siempre</b>, incluso si lo que pedía falló: un id mal escrito no debe
     * dejar un structure block a la vista dentro de la estructura.
     */
    @Override
    protected void handleDataMarker(String markerId, BlockPos position, ServerLevelAccessor level, RandomSource random, BoundingBox chunkBB) {
        StructureMarkers.parse(markerId, position).ifPresent(marker -> {
            switch (marker.verb()) {
                case StructureMarkers.MOB -> this.spawnMob(marker, position, level);
                case StructureMarkers.CHEST -> fillContainer(marker, position, level, random);
                default -> LOGGER.warn("Verbo desconocido '{}' en el marcador de {}", marker.verb(), position);
            }
        });
        level.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
    }

    /**
     * Cuánto margen sobre la huella de la plantilla se le da al mob para moverse. La huella sola
     * sería demasiado justa para un volador: el radio de hogar se mide en 3D, así que la altura de
     * vuelo se come parte del margen horizontal.
     */
    private static final int HOME_MARGIN = 8;

    /** Mismo orden que {@code OceanRuinPieces} para su marcador {@code drowned}. */
    private void spawnMob(StructureMarkers.Marker marker, BlockPos position, ServerLevelAccessor level) {
        EntityType<?> type = EntityType.byString(marker.id().toString()).orElse(null);
        if (type == null) {
            LOGGER.warn("Entidad desconocida '{}' en el marcador de {}", marker.id(), position);
            return;
        }

        Entity entity = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (!(entity instanceof Mob mob)) {
            LOGGER.warn("El marcador de {} pide '{}', que no es un Mob; no se spawnea nada", position, marker.id());
            return;
        }

        // Sin esto, los mobs de MobCategory.MONSTER despawnean al descargarse el chunk de una
        // estructura recién generada a la que el jugador todavía no ha llegado.
        mob.setPersistenceRequired();
        placeWithoutClipping(mob, position, level);

        // Atarlo a su estructura. Sin esto las arpías derivan: su vuelo es un paseo aleatorio que
        // parte de la posición actual, así que se alejan del nido y no vuelven. El hogar se
        // serializa solo (home_pos / home_radius), o sea que sobrevive a recargar el chunk.
        Vec3i size = this.template.getSize();
        mob.setHomeTo(position, Math.max(size.getX(), size.getZ()) + HOME_MARGIN);

        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(position), EntitySpawnReason.STRUCTURE, null);

        // Después de finalizeSpawn: la variante blindada sube el máximo de vida y se cura hasta él,
        // y no queremos que finalizeSpawn pise esa curación.
        if (marker.hasFlag(StructureMarkers.ARMORED)) {
            if (mob instanceof IArmoredEntity armored) {
                armored.setArmored(true);
            } else {
                LOGGER.warn("El marcador de {} pide la flag '{}' sobre '{}', que no es IArmoredEntity",
                    position, StructureMarkers.ARMORED, marker.id());
            }
        }

        level.addFreshEntityWithPassengers(mob);
    }

    /** Cuánto puede desplazarse un mob desde su marcador para no quedar empotrado. */
    private static final int NUDGE_RADIUS = 2;

    /**
     * Coloca el mob en su marcador, o en la casilla libre más cercana si ahí no cabe.
     * <br><br>
     * Hace falta porque un marcador ocupa un bloque y los mobs no: la arpía mide 1.4 de ancho, así
     * que centrada en su casilla sobresale 0,2 hacia cada columna vecina y cualquier decoración
     * pegada al marcador le entra en la caja. Si el bloque que se le mete dentro cae a la altura de
     * los ojos, el mob se asfixia nada más generarse.
     * <br><br>
     * Sólo se busca hacia arriba: hacia abajo está el suelo de la estructura.
     */
    private static void placeWithoutClipping(Mob mob, BlockPos position, ServerLevelAccessor level) {
        mob.snapTo(position, 0.0F, 0.0F);
        if (level.noCollision(mob)) {
            return;
        }

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(
                position.offset(-NUDGE_RADIUS, 0, -NUDGE_RADIUS),
                position.offset(NUDGE_RADIUS, NUDGE_RADIUS, NUDGE_RADIUS))) {
            mob.snapTo(candidate, 0.0F, 0.0F);
            if (!level.noCollision(mob)) {
                continue;
            }
            double distance = candidate.distSqr(position);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate.immutable();
            }
        }

        if (best == null) {
            LOGGER.warn("El mob del marcador de {} no cabe en {} bloques a la redonda; se deja empotrado", position, NUDGE_RADIUS);
            mob.snapTo(position, 0.0F, 0.0F);
            return;
        }

        mob.snapTo(best, 0.0F, 0.0F);
    }

    /**
     * El contenedor va <b>debajo</b> del marcador, como en el iglú. Así el cofre es un bloque más de
     * la plantilla: rota con la estructura, conserva su orientación, y puede ser un barril o un
     * cofre doble sin que este código sepa nada de ello.
     */
    private static void fillContainer(StructureMarkers.Marker marker, BlockPos position, ServerLevelAccessor level, RandomSource random) {
        BlockPos containerPos = position.below();
        if (level.getBlockEntity(containerPos) instanceof RandomizableContainer container) {
            container.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, marker.id()), random.nextLong());
        } else {
            LOGGER.warn("El marcador de cofre en {} no tiene ningún contenedor debajo", position);
        }
    }

}
