package net.darkblade.mythosmortals.worldgen.structure;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import com.mojang.logging.LogUtils;
import net.darkblade.deluxelib.entity.IArmoredEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class MarkedStructurePiece extends TemplateStructurePiece {

    private static final Logger LOGGER = LogUtils.getLogger();


    private final int carveTop;
    private final boolean ignoreAir;
    private final boolean clearVegetation;
    private final int groundY;
    private final boolean clearAbove;
    private final boolean fillBelow;
    private static final int CLEAR_HEADROOM = 10;
    private static final int FILL_LIMIT = 16;
    private static final int TREE_BAND = 5;
    private static final int TREE_LIMIT = 4096;
    private static final int SKIRT_WIDTH = 5;

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

                this.terraceSkirt(level, chunkBB);
            }
            if (this.clearAbove) {
                beforePlacing = this.sampleGround(level, area);
            }
        }

        super.postProcess(level, structureManager, generator, random, chunkBB, chunkPos, referencePos);

        if (beforePlacing != null) {
            this.carveUnderCover(level, area, beforePlacing);
        }
    }

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

    private void fillGround(WorldGenLevel level, BoundingBox area) {
        int bottom = Math.max(this.groundY - FILL_LIMIT, level.getMinY());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int filled = 0;
        int raised = 0;

        for (int x = area.minX(); x <= area.maxX(); x++) {
            for (int z = area.minZ(); z <= area.maxZ(); z++) {
                int top = groundTop(level, pos, x, z, this.groundY, bottom);
                if (top == Integer.MIN_VALUE || top >= this.groundY) {
                    continue;
                }

                pos.set(x, top, z);
                BlockState surface = level.getBlockState(pos);
                for (int y = top; y < this.groundY; y++) {
                    pos.set(x, y, z);
                    level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
                    filled++;
                }
                pos.set(x, this.groundY, z);
                level.setBlock(pos, surface, 2);
                raised++;
            }
        }

        if (filled > 0) {
            LOGGER.info("[fill] {}: rellenados {} bloques hasta y={} en {} columnas, en x[{}..{}] z[{}..{}]",
                this.templateName, filled, this.groundY, raised, area.minX(), area.maxX(), area.minZ(), area.maxZ());
        }
    }

    private void terraceSkirt(WorldGenLevel level, BoundingBox chunkBB) {
        BoundingBox footprint = this.getBoundingBox();
        int minX = Math.max(footprint.minX() - SKIRT_WIDTH, chunkBB.minX() - SKIRT_WIDTH);
        int maxX = Math.min(footprint.maxX() + SKIRT_WIDTH, chunkBB.maxX() + SKIRT_WIDTH);
        int minZ = Math.max(footprint.minZ() - SKIRT_WIDTH, chunkBB.minZ() - SKIRT_WIDTH);
        int maxZ = Math.min(footprint.maxZ() + SKIRT_WIDTH, chunkBB.maxZ() + SKIRT_WIDTH);

        int bottom = Math.max(this.groundY - FILL_LIMIT, level.getMinY());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int filled = 0;
        int stepped = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int outX = Math.max(Math.max(footprint.minX() - x, x - footprint.maxX()), 0);
                int outZ = Math.max(Math.max(footprint.minZ() - z, z - footprint.maxZ()), 0);
                int step = Math.max(outX, outZ);
                if (step == 0 || step > SKIRT_WIDTH) {
                    continue;
                }

                int target = this.groundY - step;
                int top = groundTop(level, pos, x, z, target, bottom);
                if (top == Integer.MIN_VALUE || top >= target) {
                    continue;
                }

                pos.set(x, top, z);
                BlockState surface = level.getBlockState(pos);
                for (int y = top; y < target; y++) {
                    pos.set(x, y, z);
                    level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
                    filled++;
                }
                pos.set(x, target, z);
                level.setBlock(pos, surface, 2);
                stepped++;
            }
        }

        if (stepped > 0) {
            LOGGER.info("[talud] {}: escalonadas {} columnas ({} bloques) alrededor de la huella, hasta {} de ancho",
                this.templateName, stepped, filled, SKIRT_WIDTH);
        }
    }

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

        int carved = 0;
        for (int x = area.minX() + 1; x < area.maxX(); x++) {
            for (int z = area.minZ() + 1; z < area.maxZ(); z++) {
                int i = (x - area.minX()) * width + (z - area.minZ());
                if (!covered[i] || !covered[i - width] || !covered[i + width] || !covered[i - 1] || !covered[i + 1]) {
                    continue;
                }

                pos.set(x, this.groundY + 1, z);
                if (!level.getBlockState(pos).isAir()) {
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
                    if (state.getFluidState().isEmpty() && !isVegetation(state)) {
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

    private void razeVegetation(WorldGenLevel level, BoundingBox chunkBB) {
        BoundingBox footprint = this.getBoundingBox();
        int minX = Math.max(footprint.minX(), chunkBB.minX());
        int maxX = Math.min(footprint.maxX(), chunkBB.maxX());
        int minZ = Math.max(footprint.minZ(), chunkBB.minZ());
        int maxZ = Math.min(footprint.maxZ(), chunkBB.maxZ());
        if (minX > maxX || minZ > maxZ) {
            return;
        }

        BoundingBox reach = new BoundingBox(
            Math.max(footprint.minX() - TREE_BAND, chunkBB.minX() - TREE_BAND), level.getMinY(),
            Math.max(footprint.minZ() - TREE_BAND, chunkBB.minZ() - TREE_BAND),
            Math.min(footprint.maxX() + TREE_BAND, chunkBB.maxX() + TREE_BAND), level.getMaxY(),
            Math.min(footprint.maxZ() + TREE_BAND, chunkBB.maxZ() + TREE_BAND));

        int from = footprint.minY();
        int to = Math.min(footprint.maxY() + CLEAR_HEADROOM, level.getMaxY());
        int removed = 0;
        int trees = 0;
        int beyond = 0;
        Set<Long> stumps = new HashSet<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = from; y <= to; y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (isTreePart(state)) {
                        int[] tally = this.fellTree(level, pos.immutable(), reach, footprint, stumps);
                        if (tally[0] > 0) {
                            trees++;
                            removed += tally[0];
                            beyond += tally[1];
                        }
                    } else if (isVegetation(state)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        removed++;
                    }
                }
            }
        }

        int regrown = this.regrassStumps(level, pos, stumps, footprint);

        if (removed > 0) {
            LOGGER.info("[veg] {}: arrasados {} bloques de vegetación entre y={} e y={} en x[{}..{}] z[{}..{}] ({} árboles, {} bloques perseguidos fuera de la huella, {} calvas recubiertas)",
                this.templateName, removed, from, to, minX, maxX, minZ, maxZ, trees, beyond, regrown);
        }
    }


    private int regrassStumps(WorldGenLevel level, BlockPos.MutableBlockPos pos, Set<Long> stumps, BoundingBox footprint) {
        int from = Math.min(footprint.maxY() + CLEAR_HEADROOM, level.getMaxY());
        int limit = Math.max(footprint.minY() - FILL_LIMIT, level.getMinY());
        int regrown = 0;

        for (long column : stumps) {
            int x = BlockPos.getX(column);
            int z = BlockPos.getZ(column);
            int top = groundTop(level, pos, x, z, from, limit);
            if (top == Integer.MIN_VALUE) {
                continue;
            }

            pos.set(x, top, z);
            if (!level.getBlockState(pos).is(Blocks.DIRT)) {
                continue;
            }
            pos.set(x, top + 1, z);
            BlockState above = level.getBlockState(pos);
            if (!above.isAir() && !isVegetation(above)) {
                continue;
            }

            pos.set(x, top, z);
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            regrown++;
        }
        return regrown;
    }


    private int[] fellTree(WorldGenLevel level, BlockPos seed, BoundingBox reach, BoundingBox footprint, Set<Long> stumps) {
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        pending.add(seed);
        seen.add(seed);

        int felled = 0;
        int beyond = 0;
        while (!pending.isEmpty() && felled < TREE_LIMIT) {
            BlockPos at = pending.poll();
            if (!isTreePart(level.getBlockState(at))) {
                continue;
            }

            level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
            felled++;
            stumps.add(BlockPos.asLong(at.getX(), 0, at.getZ()));
            if (at.getX() < footprint.minX() || at.getX() > footprint.maxX()
                || at.getZ() < footprint.minZ() || at.getZ() > footprint.maxZ()) {
                beyond++;
            }

            for (Direction dir : Direction.values()) {
                BlockPos next = at.relative(dir);
                if (reach.isInside(next) && seen.add(next)) {
                    pending.add(next);
                }
            }
        }

        if (felled >= TREE_LIMIT) {
            LOGGER.warn("[veg] {}: tope de {} bloques alcanzado talando desde {}; puede quedar copa suelta",
                this.templateName, TREE_LIMIT, seed);
        }
        return new int[] {felled, beyond};
    }

    private static boolean isTreePart(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
    }


    private static int groundTop(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int from, int limit) {
        for (int y = from; y > limit; y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty() || isVegetation(state)) {
                continue;
            }
            return y;
        }
        return Integer.MIN_VALUE;
    }


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


    private static final int HOME_MARGIN = 8;

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

        mob.setPersistenceRequired();
        placeWithoutClipping(mob, position, level);

        Vec3i size = this.template.getSize();
        mob.setHomeTo(position, Math.max(size.getX(), size.getZ()) + HOME_MARGIN);

        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(position), EntitySpawnReason.STRUCTURE, null);


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

    private static final int NUDGE_RADIUS = 2;


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


    private static void fillContainer(StructureMarkers.Marker marker, BlockPos position, ServerLevelAccessor level, RandomSource random) {
        BlockPos containerPos = position.below();
        if (level.getBlockEntity(containerPos) instanceof RandomizableContainer container) {
            container.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, marker.id()), random.nextLong());
        } else {
            LOGGER.warn("El marcador de cofre en {} no tiene ningún contenedor debajo", position);
        }
    }

}
