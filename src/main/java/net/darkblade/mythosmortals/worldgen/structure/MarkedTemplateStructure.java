package net.darkblade.mythosmortals.worldgen.structure;
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

    private static final int CHUNK_SCAN_STEP = 5;
    private static final int FOOTPRINT_SAMPLES = 5;
    private static final int CARVE_HEADROOM = 4;
    private static final int PROMINENCE_RADIUS = 40;
    private static final int PROMINENCE_SAMPLES = 8;
    private static final int EARLY_OUT_MARGIN = 16;
    private static final int[] BIOME_PROBE_OFFSETS = {0, 40, 80};
    private static final int GUARD_RING_GAP = 3;
    private final Identifier template;
    private final int depthBelowSurface;
    private final Optional<PeakSiting> peakSiting;
    private final boolean ignoreAir;
    private final int sink;
    private final boolean clearVegetation;
    private final boolean clearAbove;
    private final boolean fillBelow;
    private final int maxSlope;
    private final boolean avoidWater;
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

    private Optional<Structure.GenerationStub> findFlat(Structure.GenerationContext context, Vec3i size) {
        ChunkPos chunkPos = context.chunkPos();
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();

        int floorY;
        if (this.depthBelowSurface > 0) {
            int surface = getLowestY(context, size.getX(), size.getZ());
            floorY = surface - this.depthBelowSurface;

            int worldFloor = context.heightAccessor().getMinY() + 1;
            if (floorY < worldFloor || floorY + size.getY() >= surface) {
                return Optional.empty();
            }
        } else {
            int surface = surfaceAt(context, centerX, centerZ);

            if (this.maxSlope > 0 || this.avoidWater) {
                if (!probeBiome(context, centerX, centerZ, surface)) {
                    return Optional.empty();
                }

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

                int halfX = size.getX() / 2;
                int halfZ = size.getZ() / 2;
                int lowest = surface;
                int highest = surface;
                for (int sx = 0; sx < FOOTPRINT_SAMPLES; sx++) {
                    for (int sz = 0; sz < FOOTPRINT_SAMPLES; sz++) {
                        int x = centerX - halfX + (size.getX() - 1) * sx / (FOOTPRINT_SAMPLES - 1);
                        int z = centerZ - halfZ + (size.getZ() - 1) * sz / (FOOTPRINT_SAMPLES - 1);
                        int sample = surfaceAt(context, x, z);

                        if (this.avoidWater && groundAt(context, x, z) < sample) {
                            if (this.debug) {
                                LOGGER.info("[flat] {} RECHAZADO en {}: agua dentro de la huella ({},{})",
                                    this.template, chunkPos, x, z);
                            }
                            return Optional.empty();
                        }

                        lowest = Math.min(lowest, sample);
                        highest = Math.max(highest, sample);
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

            floorY = surface - this.sink;
        }

        return Optional.of(this.stub(context, size, centerX, centerZ, floorY, 0));
    }

    private Optional<Structure.GenerationStub> findPeak(Structure.GenerationContext context, Vec3i size, PeakSiting siting) {
        ChunkPos chunkPos = context.chunkPos();

        if (!probeBiome(context, chunkPos.getMiddleBlockX(), chunkPos.getMiddleBlockZ(), siting.minY())) {
            return Optional.empty();
        }

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

        if (peakY + EARLY_OUT_MARGIN < siting.minY()) {
            if (siting.debug()) {
                LOGGER.info("[peak] {} RECHAZADO en {}: terreno a y={}, muy por debajo de min_y={} (salida temprana)",
                    this.template, chunkPos, peakY, siting.minY());
            }
            return Optional.empty();
        }

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

    private Structure.GenerationStub stub(Structure.GenerationContext context, Vec3i size, int centerX, int centerZ, int floorY, int carveTop) {
        Rotation rotation = Rotation.getRandom(context.random());

        int groundY = this.clearAbove || this.fillBelow ? floorY + this.sink : Integer.MAX_VALUE;
        BlockPos origin = new BlockPos(centerX - size.getX() / 2, floorY, centerZ - size.getZ() / 2);

        return new Structure.GenerationStub(
            new BlockPos(centerX, floorY, centerZ),
            builder -> builder.addPiece(new MarkedStructurePiece(context.structureTemplateManager(), this.template, origin, rotation, carveTop, this.ignoreAir, this.clearVegetation, groundY, this.clearAbove, this.fillBelow))
        );
    }


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


    private static int groundAt(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(
            x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
    }

    @Override
    public StructureType<?> type() {
        return MythosMortalsRegistry.MARKED_TEMPLATE_STRUCTURE.get();
    }

}
