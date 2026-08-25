package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.mythosmortals.entity.pegasus.PegasusEntity;
import net.darkblade.mythosmortals.entity.arpy.ArpyEntity;
import net.darkblade.mythosmortals.entity.athenian.AthenianEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.owl.OwlEntity;
import net.darkblade.mythosmortals.entity.spartan.SpartanEntity;
import net.darkblade.mythosmortals.item.spear.ThrownDoriSpear;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosMortalsEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, MythosMortals.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<AthenianEntity>> ATHENIAN =
        ENTITY_TYPES.register("athenian",
            () -> EntityType.Builder.of(AthenianEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(8)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MythosMortals.MODID, "athenian")))
        );

    public static final DeferredHolder<EntityType<?>, EntityType<SpartanEntity>> SPARTAN =
        ENTITY_TYPES.register("spartan",
            () -> EntityType.Builder.of(SpartanEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(8)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MythosMortals.MODID, "spartan")))
        );

    public static final DeferredHolder<EntityType<?>, EntityType<ArpyEntity>> ARPY =
        ENTITY_TYPES.register("arpy",
            () -> EntityType.Builder.<ArpyEntity>of(ArpyEntity::new, MobCategory.MONSTER)
                .sized(1.3F, 1.4F)
                .clientTrackingRange(8)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MythosMortals.MODID, "arpy")))
        );

    public static final DeferredHolder<EntityType<?>, EntityType<OwlEntity>> OWL =
        ENTITY_TYPES.register("owl",
            () -> EntityType.Builder.<OwlEntity>of(OwlEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 0.6F)
                .clientTrackingRange(8)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MythosMortals.MODID, "owl")))
        );

    public static final DeferredHolder<EntityType<?>, EntityType<MinotaurEntity>> MINOTAUR =
        ENTITY_TYPES.register("minotaur",
            () -> EntityType.Builder.<MinotaurEntity>of(MinotaurEntity::new, MobCategory.MONSTER)
                .sized(1.4F, 3.2F)
                .clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MythosMortals.MODID, "minotaur")))
        );

    public static final DeferredHolder<EntityType<?>, EntityType<PegasusEntity>> PEGASUS =
        ENTITY_TYPES.register("pegasus",
            () -> EntityType.Builder.<PegasusEntity>of(PegasusEntity::new, MobCategory.CREATURE)
                .sized(1.4F, 2.0F)
                .clientTrackingRange(12)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MythosMortals.MODID, "pegasus")))
        );

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownDoriSpear>> THROWN_DORI_SPEAR =
        ENTITY_TYPES.register("thrown_dori_spear",
            () -> EntityType.Builder.<ThrownDoriSpear>of(ThrownDoriSpear::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(4)
                .build(ResourceKey.create(Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(MythosMortals.MODID, "thrown_dori_spear")))
        );

    public static final DeferredHolder<EntityType<?>, EntityType<Boat>> OLIVE_BOAT =
        ENTITY_TYPES.register("olive_boat",
            () -> EntityType.Builder.<Boat>of((type, level) -> new Boat(type, level, MythosMortalsBlocks.OLIVE_BOAT_ITEM::get), MobCategory.MISC)
                .noLootTable()
                .sized(1.375F, 0.5625F)
                .eyeHeight(0.5625F)
                .clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MythosMortals.MODID, "olive_boat")))
        );

    public static final DeferredHolder<EntityType<?>, EntityType<ChestBoat>> OLIVE_CHEST_BOAT =
        ENTITY_TYPES.register("olive_chest_boat",
            () -> EntityType.Builder.<ChestBoat>of((type, level) -> new ChestBoat(type, level, MythosMortalsBlocks.OLIVE_CHEST_BOAT_ITEM::get), MobCategory.MISC)
                .noLootTable()
                .sized(1.375F, 0.5625F)
                .eyeHeight(0.5625F)
                .clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MythosMortals.MODID, "olive_chest_boat")))
        );

    private MythosMortalsEntities() {
    }
}
