package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.deluxelib.block.StatueBlockEntity;
import net.darkblade.deluxelib.block.StatueRegistry;
import net.darkblade.deluxelib.block.StatueRenderer;
import net.darkblade.deluxelib.client.render.HelmetInteriors;
import net.darkblade.deluxelib.client.render.ShieldPoseNudges;
import net.darkblade.deluxelib.client.render.ThrownWeaponRenderer;
import net.darkblade.deluxelib.spawn.DeluxeBiomeSpawns;
import net.darkblade.mythosmortals.content.amphora.FilledAmphoraBlock;
import net.darkblade.mythosmortals.content.amphora.GreekAmphoraBlock;
import net.darkblade.mythosmortals.content.amphora.MarinatingRecipe;
import net.darkblade.mythosmortals.content.arpy.ArpyEntity;
import net.darkblade.mythosmortals.content.arpy.ArpyModel;
import net.darkblade.mythosmortals.content.arpy.ArpyRenderer;
import net.darkblade.mythosmortals.content.athenian.AthenianEntity;
import net.darkblade.mythosmortals.content.effect.BorealCourageEffect;
import net.darkblade.mythosmortals.content.athenian.AthenianHelmetInteriorModel;
import net.darkblade.mythosmortals.content.athenian.AthenianModel;
import net.darkblade.mythosmortals.content.athenian.AthenianRenderer;
import net.darkblade.mythosmortals.content.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.content.minotaur.MinotaurModel;
import net.darkblade.mythosmortals.content.minotaur.MinotaurRenderer;
import net.darkblade.mythosmortals.content.owl.CopperOwlModel;
import net.darkblade.mythosmortals.content.owl.OwlEntity;
import net.darkblade.mythosmortals.content.owl.OwlRenderer;
import net.darkblade.mythosmortals.content.owl.network.OwlAttackServerPacket;
import net.darkblade.mythosmortals.content.owl.network.OwlMarkServerPacket;
import net.darkblade.mythosmortals.content.owl.network.OwlOrderAttackServerPacket;
import net.darkblade.mythosmortals.content.owl.network.OwlSonicAttackServerPacket;
import net.darkblade.mythosmortals.content.owl.statue.OwlStatueBlock;
import net.darkblade.mythosmortals.content.owl.statue.OwlStatueClient;
import net.darkblade.mythosmortals.content.spartan.SpartanEntity;
import net.darkblade.mythosmortals.content.spartan.SpartanHelmetInteriorModel;
import net.darkblade.mythosmortals.content.spartan.SpartanModel;
import net.darkblade.mythosmortals.content.spartan.SpartanRenderer;
import net.darkblade.mythosmortals.content.spear.DoriSpearProjectileModel;
import net.darkblade.mythosmortals.content.spear.ThrownDoriSpear;
import net.darkblade.mythosmortals.content.structure.MarkedStructurePiece;
import net.darkblade.mythosmortals.content.structure.MarkedTemplateStructure;
import net.darkblade.mythosmortals.content.olive.OliveLeavesBlock;
import net.darkblade.mythosmortals.content.olive.OliveTree;
import net.darkblade.mythosmortals.content.vineyard.GrapeStakeBlock;
import net.darkblade.mythosmortals.content.vineyard.GrapeVineBlock;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.particle.SonicBoomParticle;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Unit;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The demo mod's own registry: its entity types, blocks, block items, and block entity types, its
 * {@link #registerGameplay} wiring, and — client-side — registering its statue and helmet interiors
 * into the DeluxeLib library registries ({@code StatueRegistry}, {@code HelmetInteriors}).
 *
 * <p>Uses the specialized {@link DeferredRegister.Blocks}/{@code Items.registerSimpleBlockItem}
 * pair (NeoForge's own documented way to register a block + its BlockItem together), not the
 * generic {@code DeferredRegister<Block>} + a hand-built {@code BlockItem}: the hand-built version
 * crashed at load with "Trying to access unbound value" — the block wasn't guaranteed to be bound
 * yet when the item's factory ran. The specialized registers handle that ordering correctly.
 */
public final class MythosMortalsRegistry {
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

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownDoriSpear>> THROWN_DORI_SPEAR =
        ENTITY_TYPES.register("thrown_dori_spear",
            () -> EntityType.Builder.<ThrownDoriSpear>of(ThrownDoriSpear::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(4)
                .build(ResourceKey.create(Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(MythosMortals.MODID, "thrown_dori_spear")))
        );

    private static final Identifier DORI_SPEAR_TEXTURE =
        Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/dori_spear_entity.png");

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MythosMortals.MODID);

    public static final DeferredBlock<OwlStatueBlock> OWL_STATUE =
        BLOCKS.registerBlock("owl_statue", OwlStatueBlock::new,
            () -> BlockBehaviour.Properties.of().strength(5.0F).requiresCorrectToolForDrops());


    public static final DeferredItem<BlockItem> OWL_STATUE_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("owl_statue", OWL_STATUE::get);

    public static final DeferredBlock<Block> TIN_ORE =
        BLOCKS.registerBlock("tin_ore", Block::new,
            () -> BlockBehaviour.Properties.of()
                .strength(3.0F, 3.0F)
                .requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> DEEPSLATE_TIN_ORE =
        BLOCKS.registerBlock("deepslate_tin_ore", Block::new,
            () -> BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE)
                .sound(SoundType.DEEPSLATE)
                .strength(4.5F, 3.0F)
                .requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> TIN_ORE_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("tin_ore", TIN_ORE::get);
    public static final DeferredItem<BlockItem> DEEPSLATE_TIN_ORE_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("deepslate_tin_ore", DEEPSLATE_TIN_ORE::get);

    public static final DeferredBlock<Block> MARBLE =
        BLOCKS.registerBlock("marble", Block::new, MythosMortalsRegistry::marbleProperties);
    public static final DeferredBlock<Block> SMOOTH_MARBLE =
        BLOCKS.registerBlock("smooth_marble", Block::new, MythosMortalsRegistry::marbleProperties);
    public static final DeferredBlock<Block> MARBLE_BRICKS =
        BLOCKS.registerBlock("marble_bricks", Block::new, MythosMortalsRegistry::marbleProperties);
    public static final DeferredBlock<Block> SMOOTH_GOLDEN_MARBLE =
        BLOCKS.registerBlock("smooth_golden_marble", Block::new, MythosMortalsRegistry::marbleProperties);

    public static final DeferredBlock<RotatedPillarBlock> MARBLE_PILLAR =
        BLOCKS.registerBlock("marble_pillar", RotatedPillarBlock::new, MythosMortalsRegistry::marbleProperties);

    private static BlockBehaviour.Properties marbleProperties() {
        return BlockBehaviour.Properties.of()
            .strength(1.5F, 6.0F)
            .requiresCorrectToolForDrops();
    }

    public static final DeferredItem<BlockItem> MARBLE_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("marble", MARBLE::get);
    public static final DeferredItem<BlockItem> SMOOTH_MARBLE_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("smooth_marble", SMOOTH_MARBLE::get);
    public static final DeferredItem<BlockItem> MARBLE_BRICKS_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("marble_bricks", MARBLE_BRICKS::get);
    public static final DeferredItem<BlockItem> SMOOTH_GOLDEN_MARBLE_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("smooth_golden_marble", SMOOTH_GOLDEN_MARBLE::get);
    public static final DeferredItem<BlockItem> MARBLE_PILLAR_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("marble_pillar", MARBLE_PILLAR::get);

    public static final DeferredBlock<GrapeStakeBlock> STICK_BLOCK =
        BLOCKS.registerBlock("stick_block", GrapeStakeBlock::new,
            () -> BlockBehaviour.Properties.of()
                .noOcclusion()
                .noCollision()
                .instabreak()
                .sound(SoundType.WOOD)
                .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<GrapeVineBlock> GRAPE_VINE =
        BLOCKS.registerBlock("grape_vine", GrapeVineBlock::new,
            () -> BlockBehaviour.Properties.of()
                .noOcclusion()
                .noCollision()
                .instabreak()
                .randomTicks()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<RotatedPillarBlock> OLIVE_LOG =
        BLOCKS.registerBlock("olive_log", RotatedPillarBlock::new, MythosMortalsRegistry::woodProperties);
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_OLIVE_LOG =
        BLOCKS.registerBlock("stripped_olive_log", RotatedPillarBlock::new, MythosMortalsRegistry::woodProperties);
    public static final DeferredBlock<RotatedPillarBlock> OLIVE_WOOD =
        BLOCKS.registerBlock("olive_wood", RotatedPillarBlock::new, MythosMortalsRegistry::woodProperties);
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_OLIVE_WOOD =
        BLOCKS.registerBlock("stripped_olive_wood", RotatedPillarBlock::new, MythosMortalsRegistry::woodProperties);
    public static final DeferredBlock<Block> OLIVE_PLANKS =
        BLOCKS.registerBlock("olive_planks", Block::new,
            () -> woodProperties().strength(2.0F, 3.0F));

    private static BlockBehaviour.Properties woodProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava();
    }

    public static final DeferredBlock<OliveLeavesBlock> OLIVE_LEAVES =
        BLOCKS.registerBlock("olive_leaves", OliveLeavesBlock::new,
            () -> BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .strength(0.2F)
                .randomTicks()
                .sound(SoundType.GRASS)
                .noOcclusion()
                .isValidSpawn((state, level, pos, type) -> type == EntityType.OCELOT || type == EntityType.PARROT)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY));


    public static final DeferredBlock<StairBlock> OLIVE_STAIRS =
        BLOCKS.registerBlock("olive_stairs",
            props -> new StairBlock(OLIVE_PLANKS.get().defaultBlockState(), props),
            () -> woodProperties().strength(2.0F, 3.0F));

    public static final DeferredBlock<SlabBlock> OLIVE_SLAB =
        BLOCKS.registerBlock("olive_slab", SlabBlock::new,
            () -> woodProperties().strength(2.0F, 3.0F));

    public static final DeferredBlock<FenceBlock> OLIVE_FENCE =
        BLOCKS.registerBlock("olive_fence", FenceBlock::new,
            () -> woodProperties().forceSolidOn().strength(2.0F, 3.0F));

    public static final DeferredBlock<FenceGateBlock> OLIVE_FENCE_GATE =
        BLOCKS.registerBlock("olive_fence_gate",
            props -> new FenceGateBlock(props, SoundEvents.FENCE_GATE_OPEN, SoundEvents.FENCE_GATE_CLOSE),
            () -> woodProperties().forceSolidOn().strength(2.0F, 3.0F));

    public static final DeferredBlock<ButtonBlock> OLIVE_BUTTON =
        BLOCKS.registerBlock("olive_button",
            props -> new ButtonBlock(BlockSetType.OAK, 30, props),
            () -> BlockBehaviour.Properties.of()
                .noCollision()
                .strength(0.5F)
                .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<PressurePlateBlock> OLIVE_PRESSURE_PLATE =
        BLOCKS.registerBlock("olive_pressure_plate",
            props -> new PressurePlateBlock(BlockSetType.OAK, props),
            () -> BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .forceSolidOn()
                .noCollision()
                .strength(0.5F)
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<BlockItem> OLIVE_STAIRS_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("olive_stairs", OLIVE_STAIRS::get);
    public static final DeferredItem<BlockItem> OLIVE_SLAB_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("olive_slab", OLIVE_SLAB::get);
    public static final DeferredItem<BlockItem> OLIVE_FENCE_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("olive_fence", OLIVE_FENCE::get);
    public static final DeferredItem<BlockItem> OLIVE_FENCE_GATE_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("olive_fence_gate", OLIVE_FENCE_GATE::get);
    public static final DeferredItem<BlockItem> OLIVE_BUTTON_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("olive_button", OLIVE_BUTTON::get);
    public static final DeferredItem<BlockItem> OLIVE_PRESSURE_PLATE_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("olive_pressure_plate", OLIVE_PRESSURE_PLATE::get);

    public static final DeferredBlock<DoorBlock> OLIVE_DOOR =
        BLOCKS.registerBlock("olive_door",
            props -> new DoorBlock(BlockSetType.OAK, props),
            () -> BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .instrument(NoteBlockInstrument.BASS)
                .strength(3.0F)
                .noOcclusion()
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<TrapDoorBlock> OLIVE_TRAPDOOR =
        BLOCKS.registerBlock("olive_trapdoor",
            props -> new TrapDoorBlock(BlockSetType.OAK, props),
            () -> BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .instrument(NoteBlockInstrument.BASS)
                .strength(3.0F)
                .noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false)
                .ignitedByLava());

    public static final DeferredItem<BlockItem> OLIVE_DOOR_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("olive_door", OLIVE_DOOR::get);
    public static final DeferredItem<BlockItem> OLIVE_TRAPDOOR_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("olive_trapdoor", OLIVE_TRAPDOOR::get);

    public static final WoodType OLIVE_WOOD_TYPE =
        WoodType.register(new WoodType("mythosmortals:olive", BlockSetType.OAK));

    public static final DeferredBlock<StandingSignBlock> OLIVE_SIGN =
        BLOCKS.registerBlock("olive_sign",
            props -> new StandingSignBlock(OLIVE_WOOD_TYPE, props),
            MythosMortalsRegistry::signProperties);

    public static final DeferredBlock<CeilingHangingSignBlock> OLIVE_HANGING_SIGN =
        BLOCKS.registerBlock("olive_hanging_sign",
            props -> new CeilingHangingSignBlock(OLIVE_WOOD_TYPE, props),
            MythosMortalsRegistry::signProperties);

    public static final DeferredBlock<WallSignBlock> OLIVE_WALL_SIGN =
        BLOCKS.registerBlock("olive_wall_sign",
            props -> new WallSignBlock(OLIVE_WOOD_TYPE, props),
            () -> wallVariant(OLIVE_SIGN.get())
                .mapColor(MapColor.WOOD)
                .forceSolidOn()
                .instrument(NoteBlockInstrument.BASS)
                .noCollision()
                .strength(1.0F)
                .ignitedByLava());

    public static final DeferredBlock<WallHangingSignBlock> OLIVE_WALL_HANGING_SIGN =
        BLOCKS.registerBlock("olive_wall_hanging_sign",
            props -> new WallHangingSignBlock(OLIVE_WOOD_TYPE, props),
            () -> wallVariant(OLIVE_HANGING_SIGN.get())
                .mapColor(MapColor.WOOD)
                .forceSolidOn()
                .instrument(NoteBlockInstrument.BASS)
                .noCollision()
                .strength(1.0F)
                .ignitedByLava());

    private static BlockBehaviour.Properties signProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava();
    }

    private static BlockBehaviour.Properties wallVariant(Block standing) {
        return BlockBehaviour.Properties.of()
            .overrideLootTable(standing.getLootTable())
            .overrideDescription(standing.getDescriptionId());
    }

    public static final DeferredItem<SignItem> OLIVE_SIGN_ITEM =
        MythosMortalsItems.ITEMS.registerItem("olive_sign",
            props -> new SignItem(OLIVE_SIGN.get(), OLIVE_WALL_SIGN.get(), props),
            props -> props.stacksTo(16));

    public static final DeferredItem<HangingSignItem> OLIVE_HANGING_SIGN_ITEM =
        MythosMortalsItems.ITEMS.registerItem("olive_hanging_sign",
            props -> new HangingSignItem(OLIVE_HANGING_SIGN.get(), OLIVE_WALL_HANGING_SIGN.get(), props),
            props -> props.stacksTo(16));

    public static final DeferredHolder<EntityType<?>, EntityType<Boat>> OLIVE_BOAT =
        ENTITY_TYPES.register("olive_boat",
            () -> EntityType.Builder.<Boat>of((type, level) -> new Boat(type, level, MythosMortalsRegistry.OLIVE_BOAT_ITEM::get), MobCategory.MISC)
                .noLootTable()
                .sized(1.375F, 0.5625F)
                .eyeHeight(0.5625F)
                .clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MythosMortals.MODID, "olive_boat")))
        );

    public static final DeferredHolder<EntityType<?>, EntityType<ChestBoat>> OLIVE_CHEST_BOAT =
        ENTITY_TYPES.register("olive_chest_boat",
            () -> EntityType.Builder.<ChestBoat>of((type, level) -> new ChestBoat(type, level, MythosMortalsRegistry.OLIVE_CHEST_BOAT_ITEM::get), MobCategory.MISC)
                .noLootTable()
                .sized(1.375F, 0.5625F)
                .eyeHeight(0.5625F)
                .clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MythosMortals.MODID, "olive_chest_boat")))
        );

    public static final DeferredItem<BoatItem> OLIVE_BOAT_ITEM =
        MythosMortalsItems.ITEMS.registerItem("olive_boat",
            props -> new BoatItem(OLIVE_BOAT.get(), props),
            props -> props.stacksTo(1));

    public static final DeferredItem<BoatItem> OLIVE_CHEST_BOAT_ITEM =
        MythosMortalsItems.ITEMS.registerItem("olive_chest_boat",
            props -> new BoatItem(OLIVE_CHEST_BOAT.get(), props),
            props -> props.stacksTo(1));

    public static final ModelLayerLocation OLIVE_BOAT_LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "boat/olive"), "main");
    public static final ModelLayerLocation OLIVE_CHEST_BOAT_LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "chest_boat/olive"), "main");

    public static final DeferredBlock<SaplingBlock> OLIVE_SAPLING =
        BLOCKS.registerBlock("olive_sapling", props -> new SaplingBlock(OliveTree.GROWER, props),
            () -> BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollision()
                .randomTicks()
                .instabreak()
                .sound(SoundType.GRASS)
                .pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<BlockItem> OLIVE_SAPLING_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("olive_sapling", OLIVE_SAPLING::get);

    public static final DeferredItem<BlockItem> OLIVE_LOG_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("olive_log", OLIVE_LOG::get);
    public static final DeferredItem<BlockItem> STRIPPED_OLIVE_LOG_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("stripped_olive_log", STRIPPED_OLIVE_LOG::get);
    public static final DeferredItem<BlockItem> OLIVE_WOOD_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("olive_wood", OLIVE_WOOD::get);
    public static final DeferredItem<BlockItem> STRIPPED_OLIVE_WOOD_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("stripped_olive_wood", STRIPPED_OLIVE_WOOD::get);
    public static final DeferredItem<BlockItem> OLIVE_PLANKS_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("olive_planks", OLIVE_PLANKS::get);
    public static final DeferredItem<BlockItem> OLIVE_LEAVES_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("olive_leaves", OLIVE_LEAVES::get);

    public static final DeferredBlock<GreekAmphoraBlock> GREEK_AMPHORA =
        BLOCKS.registerBlock("greek_amphora", GreekAmphoraBlock::new, MythosMortalsRegistry::amphoraProperties);

    public static final DeferredBlock<Block> GREEK_AMPHORA_GRAPES =
        BLOCKS.registerBlock("greek_amphora_grapes", Block::new, MythosMortalsRegistry::amphoraProperties);
    public static final DeferredBlock<Block> GREEK_AMPHORA_OLIVES =
        BLOCKS.registerBlock("greek_amphora_olives", Block::new, MythosMortalsRegistry::amphoraProperties);

    public static final DeferredBlock<FilledAmphoraBlock> GREEK_AMPHORA_WINE =
        BLOCKS.registerBlock("greek_amphora_wine",
            props -> new FilledAmphoraBlock(MythosMortalsItems.WINE_BOTTLE::get, props), MythosMortalsRegistry::amphoraProperties);
    public static final DeferredBlock<FilledAmphoraBlock> GREEK_AMPHORA_OLIVE_OIL =
        BLOCKS.registerBlock("greek_amphora_olive_oil",
            props -> new FilledAmphoraBlock(MythosMortalsItems.OLIVE_OIL_BOTTLE::get, props), MythosMortalsRegistry::amphoraProperties);

    private static BlockBehaviour.Properties amphoraProperties() {
        return BlockBehaviour.Properties.of()
            .strength(1.0F)
            .sound(SoundType.DECORATED_POT)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY);
    }

    public static final DeferredItem<BlockItem> GREEK_AMPHORA_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("greek_amphora", GREEK_AMPHORA::get);
    public static final DeferredItem<BlockItem> GREEK_AMPHORA_GRAPES_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("greek_amphora_grapes", GREEK_AMPHORA_GRAPES::get);
    public static final DeferredItem<BlockItem> GREEK_AMPHORA_OLIVES_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("greek_amphora_olives", GREEK_AMPHORA_OLIVES::get);

    public static final DeferredItem<BlockItem> GREEK_AMPHORA_WINE_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("greek_amphora_wine", GREEK_AMPHORA_WINE::get,
            () -> new Item.Properties()
                .food(MythosMortalsItems.WINE_FOOD, Consumables.DEFAULT_DRINK));
    public static final DeferredItem<BlockItem> GREEK_AMPHORA_OLIVE_OIL_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("greek_amphora_olive_oil", GREEK_AMPHORA_OLIVE_OIL::get);

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, MythosMortals.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> OWL_BOOM =
        PARTICLE_TYPES.register("owl_boom", () -> new SimpleParticleType(false));

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, MythosMortals.MODID);

    public static final DeferredHolder<MobEffect, BorealCourageEffect> BOREAL_COURAGE =
        MOB_EFFECTS.register("boreal_courage", BorealCourageEffect::new);

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MythosMortals.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> MARINATED =
        DATA_COMPONENTS.registerComponentType("marinated", builder -> builder
            .persistent(Unit.CODEC)
            .networkSynchronized(Unit.STREAM_CODEC));

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, MythosMortals.MODID);


    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MarinatingRecipe>> MARINATING =
        RECIPE_SERIALIZERS.register("marinating",
            () -> new RecipeSerializer<>(MarinatingRecipe.MAP_CODEC, MarinatingRecipe.STREAM_CODEC));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MythosMortals.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StatueBlockEntity>> OWL_STATUE_BLOCK_ENTITY =
        StatueBlockEntity.registerType(BLOCK_ENTITY_TYPES, "owl_statue", OWL_STATUE::get, OwlStatueBlock.OWL_TYPE);

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_TYPE, MythosMortals.MODID);


    public static final DeferredHolder<StructureType<?>, StructureType<MarkedTemplateStructure>> MARKED_TEMPLATE_STRUCTURE =
        STRUCTURE_TYPES.register("marked_template",
            () -> (StructureType<MarkedTemplateStructure>) () -> MarkedTemplateStructure.CODEC);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
        DeferredRegister.create(Registries.STRUCTURE_PIECE, MythosMortals.MODID);


    public static final DeferredHolder<StructurePieceType, StructurePieceType> MARKED_STRUCTURE_PIECE =
        STRUCTURE_PIECES.register("marked_structure",
            () -> (StructurePieceType.StructureTemplateType) MarkedStructurePiece::new);


    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        PARTICLE_TYPES.register(bus);
        MOB_EFFECTS.register(bus);
        DATA_COMPONENTS.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        STRUCTURE_TYPES.register(bus);
        STRUCTURE_PIECES.register(bus);
    }


    public static void registerGameplay() {
        DeluxeBiomeSpawns.builder(() -> ATHENIAN.get(), MobCategory.MONSTER)
            .spawnRate(20, 1, 2)
            .biomes(Biomes.PLAINS)
            .submit();
    }


    public static void registerPackets() {
        MythosMortals.NETWORK.regPacket(OwlAttackServerPacket.class);
        MythosMortals.NETWORK.regPacket(OwlSonicAttackServerPacket.class);
        MythosMortals.NETWORK.regPacket(OwlMarkServerPacket.class);
        MythosMortals.NETWORK.regPacket(OwlOrderAttackServerPacket.class);
    }

    @EventBusSubscriber(modid = MythosMortals.MODID)
    public static final class CommonModEvents {
        @SubscribeEvent
        public static void onAttributes(EntityAttributeCreationEvent event) {
            event.put(ATHENIAN.get(), AthenianEntity.createAttributes().build());
            event.put(ARPY.get(), ArpyEntity.createAttributes().build());
            event.put(SPARTAN.get(), SpartanEntity.createAttributes().build());
            event.put(MINOTAUR.get(), MinotaurEntity.createAttributes().build());
            event.put(OWL.get(), OwlEntity.createAttributes().build());
        }


        @SubscribeEvent
        public static void onAddBlockEntityBlocks(BlockEntityTypeAddBlocksEvent event) {
            event.modify(BlockEntityType.SIGN, OLIVE_SIGN.get(), OLIVE_WALL_SIGN.get());
            event.modify(BlockEntityType.HANGING_SIGN, OLIVE_HANGING_SIGN.get(), OLIVE_WALL_HANGING_SIGN.get());
        }

        @SubscribeEvent
        public static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey().equals(MythosMortalsItems.ARMORY_TAB.getKey())) {
                event.accept(OWL_STATUE_ITEM.get());
                event.accept(TIN_ORE_ITEM.get());
                event.accept(DEEPSLATE_TIN_ORE_ITEM.get());
                event.accept(MARBLE_ITEM.get());
                event.accept(SMOOTH_MARBLE_ITEM.get());
                event.accept(MARBLE_BRICKS_ITEM.get());
                event.accept(MARBLE_PILLAR_ITEM.get());
                event.accept(SMOOTH_GOLDEN_MARBLE_ITEM.get());
                event.accept(OLIVE_LOG_ITEM.get());
                event.accept(STRIPPED_OLIVE_LOG_ITEM.get());
                event.accept(OLIVE_WOOD_ITEM.get());
                event.accept(STRIPPED_OLIVE_WOOD_ITEM.get());
                event.accept(OLIVE_PLANKS_ITEM.get());
                event.accept(OLIVE_LEAVES_ITEM.get());
                event.accept(OLIVE_SAPLING_ITEM.get());
                event.accept(OLIVE_STAIRS_ITEM.get());
                event.accept(OLIVE_SLAB_ITEM.get());
                event.accept(OLIVE_FENCE_ITEM.get());
                event.accept(OLIVE_FENCE_GATE_ITEM.get());
                event.accept(OLIVE_BUTTON_ITEM.get());
                event.accept(OLIVE_PRESSURE_PLATE_ITEM.get());
                event.accept(OLIVE_DOOR_ITEM.get());
                event.accept(OLIVE_TRAPDOOR_ITEM.get());
                event.accept(OLIVE_SIGN_ITEM.get());
                event.accept(OLIVE_HANGING_SIGN_ITEM.get());
                event.accept(OLIVE_BOAT_ITEM.get());
                event.accept(OLIVE_CHEST_BOAT_ITEM.get());
                event.accept(GREEK_AMPHORA_ITEM.get());
            }
        }
    }

    @EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
    public static final class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(AthenianModel.LAYER_LOCATION, AthenianModel::createBodyLayer);
            event.registerLayerDefinition(ArpyModel.LAYER_LOCATION, ArpyModel::createBodyLayer);
            event.registerLayerDefinition(SpartanModel.LAYER_LOCATION, SpartanModel::createBodyLayer);
            event.registerLayerDefinition(MinotaurModel.LAYER_LOCATION, MinotaurModel::createBodyLayer);
            event.registerLayerDefinition(CopperOwlModel.LAYER_LOCATION, CopperOwlModel::createBodyLayer);
            event.registerLayerDefinition(AthenianHelmetInteriorModel.LAYER_LOCATION, AthenianHelmetInteriorModel::createLayer);
            event.registerLayerDefinition(SpartanHelmetInteriorModel.LAYER_LOCATION, SpartanHelmetInteriorModel::createLayer);
            event.registerLayerDefinition(DoriSpearProjectileModel.LAYER_LOCATION, DoriSpearProjectileModel::createLayer);
            event.registerLayerDefinition(OLIVE_BOAT_LAYER, BoatModel::createBoatModel);
            event.registerLayerDefinition(OLIVE_CHEST_BOAT_LAYER, BoatModel::createChestBoatModel);
        }

        @SubscribeEvent
        public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(OWL_BOOM.get(), SonicBoomParticle.Provider::new);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ATHENIAN.get(), AthenianRenderer::new);
            event.registerEntityRenderer(ARPY.get(), ArpyRenderer::new);
            event.registerEntityRenderer(SPARTAN.get(), SpartanRenderer::new);
            event.registerEntityRenderer(MINOTAUR.get(), MinotaurRenderer::new);
            event.registerEntityRenderer(OWL.get(), OwlRenderer::new);
            event.registerEntityRenderer(THROWN_DORI_SPEAR.get(),
                ctx -> new ThrownWeaponRenderer<>(ctx, DoriSpearProjectileModel.LAYER_LOCATION, DORI_SPEAR_TEXTURE));
            event.registerBlockEntityRenderer(OWL_STATUE_BLOCK_ENTITY.get(), StatueRenderer::new);
            event.registerEntityRenderer(OLIVE_BOAT.get(), ctx -> new BoatRenderer(ctx, OLIVE_BOAT_LAYER));
            event.registerEntityRenderer(OLIVE_CHEST_BOAT.get(), ctx -> new BoatRenderer(ctx, OLIVE_CHEST_BOAT_LAYER));
        }


        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> Sheets.addWoodType(OLIVE_WOOD_TYPE));

            StatueRegistry.register(OwlStatueBlock.OWL_TYPE, OWL_STATUE_ITEM, OwlStatueClient.CONFIG);

            HelmetInteriors.register(MythosMortalsItems.ATHENIAN_HELMET, AthenianHelmetInteriorModel.LAYER_LOCATION,
                    Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/athenian_helmet_entity.png"));
            HelmetInteriors.register(MythosMortalsItems.SPARTAN_HELMET, SpartanHelmetInteriorModel.LAYER_LOCATION,
                    Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/spartan_helmet_entity.png"));

            ShieldPoseNudges.register(MythosMortalsItems.ATHENIAN_SHIELD);
            ShieldPoseNudges.register(MythosMortalsItems.SPARTAN_SHIELD);
        }
    }

    private MythosMortalsRegistry() {}
}
