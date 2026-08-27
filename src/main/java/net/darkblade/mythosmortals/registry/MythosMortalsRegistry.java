package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.deluxelib.spawn.DeluxeBiomeSpawns;
import net.darkblade.mythosmortals.entity.pegasus.network.PegasusDashServerPacket;
import net.darkblade.mythosmortals.entity.owl.network.OwlAttackServerPacket;
import net.darkblade.mythosmortals.entity.owl.network.OwlMarkServerPacket;
import net.darkblade.mythosmortals.entity.owl.network.OwlOrderAttackServerPacket;
import net.darkblade.mythosmortals.entity.owl.network.OwlSonicAttackServerPacket;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.bus.api.IEventBus;

public final class MythosMortalsRegistry {

    public static void register(IEventBus bus) {
        MythosMortalsEntities.ENTITY_TYPES.register(bus);
        MythosMortalsMenus.MENU_TYPES.register(bus);
        MythosMortalsBlocks.BLOCKS.register(bus);
        MythosMortalsBlockEntities.BLOCK_ENTITY_TYPES.register(bus);
        MythosMortalsParticles.PARTICLE_TYPES.register(bus);
        MythosMortalsSounds.SOUND_EVENTS.register(bus);
        MythosMortalsEffects.MOB_EFFECTS.register(bus);
        MythosMortalsDataComponents.DATA_COMPONENTS.register(bus);
        MythosMortalsRecipes.RECIPE_SERIALIZERS.register(bus);
        MythosMortalsStructures.STRUCTURE_TYPES.register(bus);
        MythosMortalsStructures.STRUCTURE_PIECES.register(bus);
    }


    public static void registerGameplay() {
        DeluxeBiomeSpawns.builder(() -> MythosMortalsEntities.ATHENIAN.get(), MobCategory.MONSTER)
            .spawnRate(8, 1, 2)
            .biomes(Biomes.PLAINS)
            .submit();

        DeluxeBiomeSpawns.builder(() -> MythosMortalsEntities.SPARTAN.get(), MobCategory.MONSTER)
            .spawnRate(8, 1, 2)
            .biomes(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU)
            .submit();

        DeluxeBiomeSpawns.builder(() -> MythosMortalsEntities.ARPY.get(), MobCategory.MONSTER)
            .spawnRate(6, 1, 2)
            .biomes(Biomes.JAGGED_PEAKS, Biomes.STONY_PEAKS, Biomes.WINDSWEPT_HILLS)
            .submit();

        // Rare and solitary, and only up where the air is thin — the spawn rule on top of this also
        // demands open sky above Y 100, so it never turns up in a cave under a mountain.
        DeluxeBiomeSpawns.builder(() -> MythosMortalsEntities.PEGASUS.get(), MobCategory.CREATURE)
            .spawnRate(5, 1, 1)
            .biomes(Biomes.MEADOW, Biomes.GROVE, Biomes.JAGGED_PEAKS,
                Biomes.STONY_PEAKS, Biomes.WINDSWEPT_HILLS)
            .submit();
    }


    public static void registerPackets() {
        MythosMortals.NETWORK.regPacket(OwlAttackServerPacket.class);
        MythosMortals.NETWORK.regPacket(OwlSonicAttackServerPacket.class);
        MythosMortals.NETWORK.regPacket(OwlMarkServerPacket.class);
        MythosMortals.NETWORK.regPacket(OwlOrderAttackServerPacket.class);
        MythosMortals.NETWORK.regPacket(PegasusDashServerPacket.class);
    }


    private MythosMortalsRegistry() {}
}
