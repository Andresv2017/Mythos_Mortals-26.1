package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.entity.pegasus.PegasusEntity;
import net.darkblade.mythosmortals.entity.arpy.ArpyEntity;
import net.darkblade.mythosmortals.entity.athenian.AthenianEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.owl.OwlEntity;
import net.darkblade.mythosmortals.entity.spartan.SpartanEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.darkblade.mythosmortals.core.MythosMortals;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = MythosMortals.MODID)
public final class MythosMortalsCommonEvents {

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(MythosMortalsEntities.ATHENIAN.get(), AthenianEntity.createAttributes().build());
        event.put(MythosMortalsEntities.ARPY.get(), ArpyEntity.createAttributes().build());
        event.put(MythosMortalsEntities.SPARTAN.get(), SpartanEntity.createAttributes().build());
        event.put(MythosMortalsEntities.MINOTAUR.get(), MinotaurEntity.createAttributes().build());
        event.put(MythosMortalsEntities.OWL.get(), OwlEntity.createAttributes().build());
        event.put(MythosMortalsEntities.PEGASUS.get(), PegasusEntity.createAttributes().build());
    }


    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // AND on top of the biome list: high ground with the sky overhead, so a pegasus never
        // generates inside a cave system that happens to run under a mountain biome.
        event.register(MythosMortalsEntities.PEGASUS.get(), SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            PegasusEntity::checkPegasusSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND);

        // The athenian has had a biome spawn entry since it was written, but no placement — the
        // server logged an error about it the moment the spawn datagen started producing its
        // biome modifier. Solid ground and nothing else, so this only silences the error rather
        // than quietly imposing a darkness rule the mob never asked for.
        event.register(MythosMortalsEntities.ATHENIAN.get(), SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type, level, reason, pos, random) -> true,
            RegisterSpawnPlacementsEvent.Operation.AND);

        event.register(MythosMortalsEntities.SPARTAN.get(), SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type, level, reason, pos, random) -> true,
            RegisterSpawnPlacementsEvent.Operation.AND);

        event.register(MythosMortalsEntities.ARPY.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type, level, reason, pos, random) -> true,
            RegisterSpawnPlacementsEvent.Operation.AND);
    }

    @SubscribeEvent
    public static void onAddBlockEntityBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityType.SIGN, MythosMortalsBlocks.OLIVE_SIGN.get(), MythosMortalsBlocks.OLIVE_WALL_SIGN.get());
        event.modify(BlockEntityType.HANGING_SIGN, MythosMortalsBlocks.OLIVE_HANGING_SIGN.get(), MythosMortalsBlocks.OLIVE_WALL_HANGING_SIGN.get());
    }

    @SubscribeEvent
    public static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(MythosMortalsItems.ARMORY_TAB.getKey())) {
            event.accept(MythosMortalsBlocks.OWL_STATUE_ITEM.get());
            event.accept(MythosMortalsBlocks.TIN_ORE_ITEM.get());
            event.accept(MythosMortalsBlocks.DEEPSLATE_TIN_ORE_ITEM.get());
            event.accept(MythosMortalsBlocks.MARBLE_ITEM.get());
            event.accept(MythosMortalsBlocks.SMOOTH_MARBLE_ITEM.get());
            event.accept(MythosMortalsBlocks.MARBLE_BRICKS_ITEM.get());
            event.accept(MythosMortalsBlocks.MARBLE_PILLAR_ITEM.get());
            event.accept(MythosMortalsBlocks.SMOOTH_GOLDEN_MARBLE_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_LOG_ITEM.get());
            event.accept(MythosMortalsBlocks.STRIPPED_OLIVE_LOG_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_WOOD_ITEM.get());
            event.accept(MythosMortalsBlocks.STRIPPED_OLIVE_WOOD_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_PLANKS_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_LEAVES_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_SAPLING_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_STAIRS_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_SLAB_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_FENCE_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_FENCE_GATE_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_BUTTON_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_PRESSURE_PLATE_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_DOOR_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_TRAPDOOR_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_SIGN_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_HANGING_SIGN_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_BOAT_ITEM.get());
            event.accept(MythosMortalsBlocks.OLIVE_CHEST_BOAT_ITEM.get());
            event.accept(MythosMortalsBlocks.GREEK_AMPHORA_ITEM.get());
        }
    }

    private MythosMortalsCommonEvents() {
    }
}
