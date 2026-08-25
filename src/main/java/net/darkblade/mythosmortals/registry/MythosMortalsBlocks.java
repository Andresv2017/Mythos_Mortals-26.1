package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.mythosmortals.block.amphora.FilledAmphoraBlock;
import net.darkblade.mythosmortals.block.amphora.GreekAmphoraBlock;
import net.darkblade.mythosmortals.entity.owl.statue.OwlStatueBlock;
import net.darkblade.mythosmortals.block.olive.OliveLeavesBlock;
import net.darkblade.mythosmortals.block.olive.OliveTree;
import net.darkblade.mythosmortals.block.vineyard.GrapeStakeBlock;
import net.darkblade.mythosmortals.block.vineyard.GrapeVineBlock;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumables;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosMortalsBlocks {

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
        BLOCKS.registerBlock("marble", Block::new, MythosMortalsBlocks::marbleProperties);
    public static final DeferredBlock<Block> SMOOTH_MARBLE =
        BLOCKS.registerBlock("smooth_marble", Block::new, MythosMortalsBlocks::marbleProperties);
    public static final DeferredBlock<Block> MARBLE_BRICKS =
        BLOCKS.registerBlock("marble_bricks", Block::new, MythosMortalsBlocks::marbleProperties);
    public static final DeferredBlock<Block> SMOOTH_GOLDEN_MARBLE =
        BLOCKS.registerBlock("smooth_golden_marble", Block::new, MythosMortalsBlocks::marbleProperties);

    public static final DeferredBlock<RotatedPillarBlock> MARBLE_PILLAR =
        BLOCKS.registerBlock("marble_pillar", RotatedPillarBlock::new, MythosMortalsBlocks::marbleProperties);

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
        BLOCKS.registerBlock("olive_log", RotatedPillarBlock::new, MythosMortalsBlocks::woodProperties);
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_OLIVE_LOG =
        BLOCKS.registerBlock("stripped_olive_log", RotatedPillarBlock::new, MythosMortalsBlocks::woodProperties);
    public static final DeferredBlock<RotatedPillarBlock> OLIVE_WOOD =
        BLOCKS.registerBlock("olive_wood", RotatedPillarBlock::new, MythosMortalsBlocks::woodProperties);
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_OLIVE_WOOD =
        BLOCKS.registerBlock("stripped_olive_wood", RotatedPillarBlock::new, MythosMortalsBlocks::woodProperties);
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
            MythosMortalsBlocks::signProperties);

    public static final DeferredBlock<CeilingHangingSignBlock> OLIVE_HANGING_SIGN =
        BLOCKS.registerBlock("olive_hanging_sign",
            props -> new CeilingHangingSignBlock(OLIVE_WOOD_TYPE, props),
            MythosMortalsBlocks::signProperties);

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


    public static final DeferredItem<BoatItem> OLIVE_BOAT_ITEM =
        MythosMortalsItems.ITEMS.registerItem("olive_boat",
            props -> new BoatItem(MythosMortalsEntities.OLIVE_BOAT.get(), props),
            props -> props.stacksTo(1));

    public static final DeferredItem<BoatItem> OLIVE_CHEST_BOAT_ITEM =
        MythosMortalsItems.ITEMS.registerItem("olive_chest_boat",
            props -> new BoatItem(MythosMortalsEntities.OLIVE_CHEST_BOAT.get(), props),
            props -> props.stacksTo(1));

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
        BLOCKS.registerBlock("greek_amphora", GreekAmphoraBlock::new, MythosMortalsBlocks::amphoraProperties);

    public static final DeferredBlock<Block> GREEK_AMPHORA_GRAPES =
        BLOCKS.registerBlock("greek_amphora_grapes", Block::new, MythosMortalsBlocks::amphoraProperties);
    public static final DeferredBlock<Block> GREEK_AMPHORA_OLIVES =
        BLOCKS.registerBlock("greek_amphora_olives", Block::new, MythosMortalsBlocks::amphoraProperties);

    public static final DeferredBlock<FilledAmphoraBlock> GREEK_AMPHORA_WINE =
        BLOCKS.registerBlock("greek_amphora_wine",
            props -> new FilledAmphoraBlock(MythosMortalsItems.WINE_BOTTLE::get, props), MythosMortalsBlocks::amphoraProperties);
    public static final DeferredBlock<FilledAmphoraBlock> GREEK_AMPHORA_OLIVE_OIL =
        BLOCKS.registerBlock("greek_amphora_olive_oil",
            props -> new FilledAmphoraBlock(MythosMortalsItems.OLIVE_OIL_BOTTLE::get, props), MythosMortalsBlocks::amphoraProperties);

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

    private MythosMortalsBlocks() {
    }
}
