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

    /** The thrown Dori spear, on the same register as the demo mobs — it used to have its own
     * {@code DeluxeProjectiles} class in the library, whose entire content this is. The id is
     * unchanged, so nothing about the entity moved as far as the game is concerned. */
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

    // requiresCorrectToolForDrops(): needs an iron+ pickaxe to actually drop the item (a vanilla
    // stand-in for the not-yet-built Bronze Pickaxe, same "borrow a vanilla item until the real bronze
    // tool chain exists" idea already used for the Athena's Sight upgrade's spyglass) — see
    // data/minecraft/tags/block/mineable/pickaxe.json and needs_iron_tool.json.
    public static final DeferredBlock<OwlStatueBlock> OWL_STATUE =
        BLOCKS.registerBlock("owl_statue", OwlStatueBlock::new,
            // strength(2.0F) broke almost instantly with the required iron+ pickaxe — 5.0F (Iron
            // Block's own hardness) makes a statue actually feel like it takes some real mining.
            () -> BlockBehaviour.Properties.of().strength(5.0F).requiresCorrectToolForDrops());

    // Registrado sobre MythosMortalsItems.ITEMS (no un registro de ítems propio) para que siga apareciendo
    // en la pestaña creativa junto al resto.
    public static final DeferredItem<BlockItem> OWL_STATUE_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("owl_statue", OWL_STATUE::get);

    /**
     * Mena de estaño, la fuente en mundo del {@code raw_tin} que ya existía como ítem suelto.
     *
     * <p>Los ids {@code tin_ore}/{@code deepslate_tin_ore} son los mismos que tenían cuando eran
     * ítems planos en {@code MythosMortalsItems} — lo que cambia es que ahora hay un bloque detrás, así que
     * el modelo que faltaba (y que dejaba el ícono en morado y negro en el creativo) sale del
     * blockstate en lugar de una textura {@code item/} inexistente.
     *
     * <p>Dureza y resistencia copiadas de la mena de cobre/hierro de vanilla, y
     * {@code requiresCorrectToolForDrops} + la etiqueta {@code needs_stone_tool}: pico de piedra o
     * mejor, o no suelta nada.
     */
    public static final DeferredBlock<Block> TIN_ORE =
        BLOCKS.registerBlock("tin_ore", Block::new,
            () -> BlockBehaviour.Properties.of()
                .strength(3.0F, 3.0F)
                .requiresCorrectToolForDrops());

    /** La variante de pizarra profunda: más dura y con el sonido de deepslate, igual que en vanilla.
     * Cuál de las dos genera el mundo lo decide el bloque que sustituye, no la altura — ver
     * {@code worldgen/configured_feature/ore_tin.json}. */
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

    /**
     * Mármol: la piedra griega del demo. Sólo esta variante genera en el mundo (ver
     * {@code worldgen/configured_feature/ore_marble.json}); las otras cuatro son decorativas y se
     * sacan del creativo — no hay recetas todavía, igual que el resto de materiales del demo.
     *
     * <p>Dureza y resistencia de la piedra/granito de vanilla, y {@code requiresCorrectToolForDrops}
     * sin etiqueta {@code needs_*}: cualquier pico sirve, como con el granito.
     */
    public static final DeferredBlock<Block> MARBLE =
        BLOCKS.registerBlock("marble", Block::new, MythosMortalsRegistry::marbleProperties);
    public static final DeferredBlock<Block> SMOOTH_MARBLE =
        BLOCKS.registerBlock("smooth_marble", Block::new, MythosMortalsRegistry::marbleProperties);
    public static final DeferredBlock<Block> MARBLE_BRICKS =
        BLOCKS.registerBlock("marble_bricks", Block::new, MythosMortalsRegistry::marbleProperties);
    public static final DeferredBlock<Block> SMOOTH_GOLDEN_MARBLE =
        BLOCKS.registerBlock("smooth_golden_marble", Block::new, MythosMortalsRegistry::marbleProperties);

    /** La columna es un {@link RotatedPillarBlock} y no un bloque plano: la textura tiene vetas
     * verticales, así que necesita orientarse con el eje en que se coloca y rematar arriba y abajo
     * con {@code smooth_marble} en vez de repetir las vetas en las caras superiores — el mismo
     * tratamiento que la columna de cuarzo de vanilla. */
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

    /**
     * El poste del viñedo. A diferencia de todo lo demás en este registro, <b>no</b> lleva
     * {@code registerSimpleBlockItem} ni entra en el tab creativo: se coloca con un palo de vanilla
     * y al romperse devuelve un palo, así que un ítem propio sobraría — ver {@link GrapeStakeBlock}.
     */
    public static final DeferredBlock<GrapeStakeBlock> STICK_BLOCK =
        BLOCKS.registerBlock("stick_block", GrapeStakeBlock::new,
            () -> BlockBehaviour.Properties.of()
                .noOcclusion()
                .noCollision()
                .instabreak()
                .sound(SoundType.WOOD)
                .pushReaction(PushReaction.DESTROY));

    /**
     * La vid de uvas. Tampoco lleva BlockItem: se planta con el ítem {@code deluxelib:grapes} sobre
     * dos postes, no colocándola. {@code randomTicks()} es obligatorio o nunca crecería.
     */
    public static final DeferredBlock<GrapeVineBlock> GRAPE_VINE =
        BLOCKS.registerBlock("grape_vine", GrapeVineBlock::new,
            () -> BlockBehaviour.Properties.of()
                .noOcclusion()
                .noCollision()
                .instabreak()
                .randomTicks()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY));

    /**
     * El olivo: cuatro variantes de tronco, tablones y hojas. A diferencia de los bloques del
     * viñedo, éstos <b>sí</b> llevan BlockItem y entran en el creativo — son bloques de construcción
     * normales, no piezas de una mecánica.
     *
     * <p>{@code ignitedByLava()} va en la madera igual que en vanilla: la lava sí prende un tronco
     * de olivo. Lo que no hay (todavía) es propagación del fuego de un bloque de olivo a otro —
     * {@code FireBlock.setFlammable} es privada en 26.1 y la vía de NeoForge obligaría a subclasear
     * los cinco bloques sólo para eso. Está anotado como hueco conocido en el spec del olivo.
     */
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

    /** Propiedades de hoja calcadas de {@code Blocks#leavesProperties}: los ayudantes que usa
     * vanilla ({@code Blocks::ocelotOrParrot}, {@code Blocks::never}) son privados, así que van
     * escritos a mano. */
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

    /**
     * Los seis constructivos de olivo. Todos salen de {@code olive_planks.png} sin arte nuevo, y
     * ninguno necesita clase propia: bastan las de vanilla con el {@code BlockSetType.OAK} prestado,
     * que sólo decide los sonidos (clic del botón, chirrido de la puerta de valla). Registrar un
     * {@code BlockSetType} propio sólo tendría sentido si quisiéramos sonidos distintos, y el olivo
     * suena a madera como cualquier otra.
     *
     * <p>Más abajo <b>sí</b> hay un {@link #OLIVE_WOOD_TYPE}, pero no contradice esto: se registró
     * para los carteles, que lo exigen en el constructor y que sacan de su nombre la ruta de la
     * textura. Estos seis siguen en {@code BlockSetType.OAK} a propósito, y no se retrofitaron.
     *
     * <p><b>La escalera es la línea frágil del lote.</b> {@link StairBlock} pide el
     * {@code BlockState} de los tablones en el constructor, no un {@code Supplier}, así que
     * {@code OLIVE_PLANKS.get()} tiene que resolver ya cuando esta fábrica corre. Funciona porque el
     * {@code DeferredRegister} registra en orden de declaración y {@code OLIVE_PLANKS} está declarado
     * más arriba. Mover cualquiera de los dos campos rompe esto con el mismo
     * "Trying to access unbound value" que documenta el javadoc de esta clase.
     */
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

    /** Usa la sobrecarga que toma los sonidos sueltos en vez de un {@code WoodType}: así no hay que
     * registrar un tipo de madera propio sólo para que la puerta chirríe. */
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

    /**
     * Puerta y trampilla. Como los seis de arriba, toman prestado {@link BlockSetType#OAK} — el
     * olivo suena a madera como cualquier otra. Propiedades calcadas de {@code Blocks.OAK_DOOR} y
     * {@code Blocks.OAK_TRAPDOOR}.
     *
     * <p>{@code isValidSpawn} siempre falso en la trampilla es de vanilla y tiene razón de ser: sin
     * él los mobs aparecerían de pie sobre una trampilla cerrada.
     */
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

    /**
     * El tipo de madera del olivo. Existe por los carteles y <b>sólo</b> por ellos: los cuatro
     * bloques de cartel piden un {@code WoodType} en el constructor, y su nombre <b>es</b> la ruta
     * de la textura. {@code Sheets.SIGN_MAPPER} hace {@code Identifier.parse(woodType.name())} y le
     * antepone {@code entity/signs/}, así que este {@code "deluxelib:olive"} es lo que manda el
     * juego a {@code deluxelib:textures/entity/signs/olive.png} en vez de al namespace de vanilla.
     *
     * <p>El namespace es además obligatorio por higiene, igual que en {@code OliveTree.GROWER}:
     * {@code WoodType.TYPES} es un mapa estático global compartido por todos los mods cargados, y un
     * {@code "olive"} a secas es una colisión esperando a otro mod mediterráneo.
     *
     * <p>El {@link BlockSetType#OAK} que lleva dentro es <b>inerte</b>: nada en 26.1 lee
     * {@code WoodType.setType()}. Está ahí porque el record lo exige. Los sonidos que sí se usan son
     * los otros componentes ({@code soundType}, {@code hangingSignSoundType}), y son los de madera
     * normal.
     *
     * <p>Registrarlo aquí, en inicialización estática, llega de sobra a tiempo para lo que de
     * verdad depende de ello: {@code LayerDefinitions.createRoots()} recorre
     * {@code WoodType.values()} y crea las definiciones de capa del cartel de pie, de pared y
     * colgante para todo tipo registrado — <b>por eso este mod no registra ninguna capa de
     * cartel</b>.
     */
    public static final WoodType OLIVE_WOOD_TYPE =
        WoodType.register(new WoodType("mythosmortals:olive", BlockSetType.OAK));

    /**
     * Los cuatro bloques de cartel. Clases de vanilla tal cual; lo único propio es el
     * {@link #OLIVE_WOOD_TYPE}.
     *
     * <p><b>El orden de declaración importa</b>, igual que en {@code OLIVE_STAIRS}: los dos de pared
     * leen la tabla de botín y la clave de nombre de su versión de pie con {@link #wallVariant}, y
     * eso exige que ese bloque ya esté construido. Subir un campo de pared por encima del suyo de
     * pie lo rompe con el mismo "Trying to access unbound value".
     *
     * <p>Los constructores de las cuatro clases ya aplican {@code properties.sound(...)} por su
     * cuenta a partir del {@code WoodType}; no hay que pasarlo.
     */
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

    /**
     * El equivalente del {@code Blocks#wallVariant} de vanilla, que es privado. Sus dos piezas no lo
     * son.
     *
     * <p>Sin el {@code overrideLootTable}, el cartel de pared buscaría una tabla
     * {@code deluxelib:blocks/olive_wall_sign} que no existe y no soltaría nada al romperlo. Sin el
     * {@code overrideDescription}, saldría en pantalla como "Olive Wall Sign" en vez de compartir
     * nombre con su versión de pie.
     */
    private static BlockBehaviour.Properties wallVariant(Block standing) {
        return BlockBehaviour.Properties.of()
            .overrideLootTable(standing.getLootTable())
            .overrideDescription(standing.getDescriptionId());
    }

    /** Los dos bloques de pared <b>no</b> llevan ítem propio: se colocan con estos mismos, que
     * eligen variante según la cara golpeada. Es el comportamiento de vanilla. */
    public static final DeferredItem<SignItem> OLIVE_SIGN_ITEM =
        MythosMortalsItems.ITEMS.registerItem("olive_sign",
            props -> new SignItem(OLIVE_SIGN.get(), OLIVE_WALL_SIGN.get(), props),
            props -> props.stacksTo(16));

    public static final DeferredItem<HangingSignItem> OLIVE_HANGING_SIGN_ITEM =
        MythosMortalsItems.ITEMS.registerItem("olive_hanging_sign",
            props -> new HangingSignItem(OLIVE_HANGING_SIGN.get(), OLIVE_WALL_HANGING_SIGN.get(), props),
            props -> props.stacksTo(16));

    /**
     * Las dos barcas. Viven aquí y no arriba con los mobs porque son contenido del olivo.
     *
     * <p>{@code EntityType.boatFactory} y {@code chestBoatFactory} son privadas, pero cada una es un
     * lambda de una línea: {@link Boat} y {@link ChestBoat} tienen constructor público que toma el
     * {@code Supplier<Item>} de lo que sueltan al romperse. Ese supplier es lo que rompe la
     * dependencia circular con el ítem, que a su vez necesita este {@code EntityType} ya resuelto.
     *
     * <p><b>Y por eso el orden entre registros importa</b>: el {@code BoatItem} llama a
     * {@code OLIVE_BOAT.get()} cuando dispara el {@code RegisterEvent} de {@code Registries.ITEM}.
     * Funciona porque {@code BuiltInRegistries} declara {@code ENTITY_TYPE} antes que {@code ITEM} y
     * los eventos salen en ese orden. Es la misma dependencia que tiene vanilla en
     * {@code Items.OAK_BOAT}.
     *
     * <p>Los dos {@code BoatItem} se declaran aquí y no en {@link MythosMortalsItems} aunque no sean
     * {@code BlockItem}: necesitan estos {@code EntityType}, y hacer que {@code MythosMortalsItems} importe
     * de {@code MythosMortalsRegistry} cerraría un ciclo de inicialización estática entre las dos clases
     * (hoy la flecha va sólo en un sentido). Se registran igualmente sobre {@code MythosMortalsItems.ITEMS},
     * como ya hacen todos los {@code BlockItem} de este archivo.
     *
     * <p><b>El {@code MythosMortalsRegistry.} delante de {@code OLIVE_BOAT_ITEM} no sobra</b>, aunque el
     * campo esté en esta misma clase: el ítem se declara más abajo, y una referencia adelantada
     * <i>por nombre simple</i> dentro de un lambda es un error de compilación
     * ("illegal forward reference", JLS 8.3.3). Cualificarla saca el uso de esa regla, que sólo
     * cubre nombres simples. Quitar el prefijo rompe la compilación.
     */
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

    /**
     * Las capas de modelo de las barcas. <b>Sus nombres son las rutas de textura</b>:
     * {@code BoatRenderer} hace {@code modelId.model().withPath(p -> "textures/entity/" + p +
     * ".png")}, así que {@code deluxelib:boat/olive} resuelve a
     * {@code deluxelib:textures/entity/boat/olive.png}. Renombrar la capa mueve la textura.
     */
    public static final ModelLayerLocation OLIVE_BOAT_LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "boat/olive"), "main");
    public static final ModelLayerLocation OLIVE_CHEST_BOAT_LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "chest_boat/olive"), "main");

    /**
     * El brote. Usa el {@link SaplingBlock} de vanilla tal cual — todo lo que hace falta propio va en
     * el {@link OliveTree#GROWER} que se le pasa. Propiedades calcadas del roble.
     *
     * <p>Sin textura todavía, por decisión explícita: el bloque y su ítem se ven como el cuadro de
     * textura ausente hasta que exista {@code textures/block/olive_sapling.png}. Los modelos ya
     * apuntan a ese nombre, así que el día que aparezca el archivo entra solo.
     */
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

    /**
     * El Ánfora Griega y sus cuatro estados de contenido — ver
     * {@code docs/superpowers/specs/2026-08-17-anfora-griega-design.md}.
     *
     * <p><b>Son cinco bloques hermanos y no uno con una propiedad {@code content} por una razón
     * concreta:</b> un {@code minecraft:smelting} empareja por {@link net.minecraft.world.item.Item},
     * no por blockstate. Para que el horno sepa distinguir "ánfora con uvas" de "ánfora vacía"
     * tienen que ser ítems distintos, y la forma barata de tener ítems distintos es tener bloques
     * distintos con su propio {@code BlockItem}. Con esto, las dos fermentaciones son recetas JSON
     * y no hay una línea de Java detrás.
     *
     * <p>Propiedades compartidas por los cinco: cerámica. Se rompe de un par de golpes, suena a
     * maceta y el pistón la revienta en vez de empujarla.
     */
    public static final DeferredBlock<GreekAmphoraBlock> GREEK_AMPHORA =
        BLOCKS.registerBlock("greek_amphora", GreekAmphoraBlock::new, MythosMortalsRegistry::amphoraProperties);

    /**
     * Estados intermedios: sólo existen para ir al horno, así que no necesitan comportamiento propio.
     *
     * <p>Reutilizan la textura de su producto — las uvas se ven como el vino, las aceitunas como el
     * aceite. No es un apaño por falta de arte: el ánfora es cerámica <b>opaca</b>, así que lo que se
     * ve por fuera es la decoración pintada del recipiente, y un recipiente de vino se decora igual
     * antes y después de fermentar. Con esto no queda ninguna textura pendiente de dibujar.
     */
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
    /**
     * El ánfora de vino en la mano <b>se bebe</b>, como una botella de agua: misma animación, mismo
     * sonido, mismo aguantar el click. Cada trago gasta una ración de las cuatro, y quien decide qué
     * vuelve a la mano es {@code WineEvents} — no un {@code USE_REMAINDER}, que sólo sabe devolver
     * un ítem fijo y aquí depende de cuánto quedaba.
     *
     * <p>Sigue siendo colocable. No chocan: mirar a un bloque va por {@code useOn} (coloca) y mirar
     * al aire va por {@code use} (bebe). {@code BlockItem} sólo sobrescribe el primero.
     */
    public static final DeferredItem<BlockItem> GREEK_AMPHORA_WINE_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("greek_amphora_wine", GREEK_AMPHORA_WINE::get,
            () -> new Item.Properties()
                .food(MythosMortalsItems.WINE_FOOD, Consumables.DEFAULT_DRINK));
    public static final DeferredItem<BlockItem> GREEK_AMPHORA_OLIVE_OIL_ITEM =
        MythosMortalsItems.ITEMS.registerSimpleBlockItem("greek_amphora_olive_oil", GREEK_AMPHORA_OLIVE_OIL::get);

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, MythosMortals.MODID);

    /**
     * The owl's own sonic screech particle, replacing the borrowed vanilla {@code SONIC_BOOM}.
     *
     * <p>Its 9 frames are declared in {@code assets/deluxelib/particles/owl_boom.json} and animate
     * over the particle's lifetime — the sprite set is what makes it a "json-based" particle type, so
     * that file must exist or the particle loader errors on a missing texture list.
     *
     * <p>{@code overrideLimiter = false} on purpose: that flag would make the particle ignore the
     * player's own particle-quality setting. Same call already made for the beam's distance flag —
     * reach further, yes; overrule someone's graphics options, no.
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> OWL_BOOM =
        PARTICLE_TYPES.register("owl_boom", () -> new SimpleParticleType(false));

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, MythosMortals.MODID);

    /** Valentía Boreal — ver {@link BorealCourageEffect}. El id {@code boreal_courage} tiene que
     * seguir coincidiendo con {@code textures/mob_effect/boreal_courage.png}: el icono del HUD se
     * resuelve por id, no se declara en ninguna parte. */
    public static final DeferredHolder<MobEffect, BorealCourageEffect> BOREAL_COURAGE =
        MOB_EFFECTS.register("boreal_courage", BorealCourageEffect::new);

    // El vino no guarda ningún temporizador propio: la ventana del castigo es la duración de
    // Valentía Boreal, que ya se guarda y sincroniza sola como cualquier efecto. Ver WineEvents.

    /** El componente que marca una comida como marinada en aceite de oliva. Un {@link Unit}: la
     * información entera es "lo está o no lo está".
     *
     * <p>Va sincronizado a cliente — sin {@code StreamCodec} el tooltip no vería nada, porque el
     * cliente sólo conoce los componentes que el servidor le manda. */
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MythosMortals.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> MARINATED =
        DATA_COMPONENTS.registerComponentType("marinated", builder -> builder
            .persistent(Unit.CODEC)
            .networkSynchronized(Unit.STREAM_CODEC));

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, MythosMortals.MODID);

    /** El marinado no es una receta sino una familia entera de ellas (una por comida marinable), así
     * que va como {@code CustomRecipe} con un serializador sin campos — ver
     * {@link net.darkblade.mythosmortals.content.amphora.MarinatingRecipe}. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MarinatingRecipe>> MARINATING =
        RECIPE_SERIALIZERS.register("marinating",
            () -> new RecipeSerializer<>(MarinatingRecipe.MAP_CODEC, MarinatingRecipe.STREAM_CODEC));

    // No specialized DeferredRegister subclass for BlockEntityType exists (unlike Blocks/Items) —
    // the plain generic form is NeoForge's own documented pattern for this registry.
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MythosMortals.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StatueBlockEntity>> OWL_STATUE_BLOCK_ENTITY =
        StatueBlockEntity.registerType(BLOCK_ENTITY_TYPES, "owl_statue", OWL_STATUE::get, OwlStatueBlock.OWL_TYPE);

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_TYPE, MythosMortals.MODID);

    /** {@code StructureType} es una interfaz funcional que sólo devuelve el codec. Hay un único
     * tipo para todas las estructuras de plantilla del mod: cuál es la plantilla y a qué
     * profundidad va son campos del JSON, así que la arena y el nido comparten esta entrada y una
     * estructura nueva no necesita Java. */
    public static final DeferredHolder<StructureType<?>, StructureType<MarkedTemplateStructure>> MARKED_TEMPLATE_STRUCTURE =
        STRUCTURE_TYPES.register("marked_template",
            () -> (StructureType<MarkedTemplateStructure>) () -> MarkedTemplateStructure.CODEC);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
        DeferredRegister.create(Registries.STRUCTURE_PIECE, MythosMortals.MODID);

    /** Sin este registro el juego revienta al guardar un chunk que contenga una de estas
     * estructuras: no sabría serializar la pieza. {@code StructureTemplateType} es la variante para
     * piezas de plantilla, la que recibe el {@code StructureTemplateManager} al deserializar. */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> MARKED_STRUCTURE_PIECE =
        STRUCTURE_PIECES.register("marked_structure",
            () -> (StructurePieceType.StructureTemplateType) MarkedStructurePiece::new);

    /** Corre primero en el constructor de {@link net.darkblade.mythosmortals.MythosMortals}, antes de
     * {@code MythosMortalsItems.register}: lo único que importa es que esta inicialización estática (la que
     * mete el BlockItem de la estatua en {@code MythosMortalsItems.ITEMS}) corra antes de que el
     * {@code RegisterEvent} de ese DeferredRegister dispare, y con este orden corre incluso más
     * temprano que antes, no más tarde. */
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

    /**
     * Worked example of {@link DeluxeBiomeSpawns}. Must run during mod construction, same as
     * {@link #register}.
     */
    public static void registerGameplay() {
        DeluxeBiomeSpawns.builder(() -> ATHENIAN.get(), MobCategory.MONSTER)
            .spawnRate(20, 1, 2)
            .biomes(Biomes.PLAINS)
            .submit();
    }

    /**
     * Records the owl's own possession action packets — {@code OwlAttackServerPacket},
     * {@code OwlSonicAttackServerPacket}, {@code OwlMarkServerPacket} — on
     * {@link MythosMortals#NETWORK}, this mod's own channel (separate from DeluxeLib's), created via
     * {@code NetworkCreator.create(MODID, 1)} the same way DeluxeLib creates its own.
     *
     * <p>Must run during mod construction, before {@code modEventBus.addListener(NETWORK::register)}
     * fires, same as the rest of {@link MythosMortals}'s own {@code regPacket} calls.
     */
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

        /**
         * Mete los cuatro bloques de cartel del olivo en los {@code BlockEntityType} de vanilla.
         *
         * <p><b>No es opcional y no hay alternativa.</b> {@code SignBlock#newBlockEntity} devuelve
         * {@code new SignBlockEntity(pos, state)}, y ese constructor delega en
         * {@code this(BlockEntityType.SIGN, pos, state)} — el tipo va a fuego. Igual el colgante con
         * {@code HangingSignBlockEntity}, que sólo tiene el constructor de dos argumentos. Y
         * {@code BlockEntityType} valida que el bloque esté en su {@code validBlocks}, así que sin
         * esto el block entity se destruye al colocar el cartel y el texto se pierde al recargar.
         *
         * <p>El regalo de reusar los tipos de vanilla es el renderizado: {@code StandingSignRenderer}
         * y {@code HangingSignRenderer} ya están registrados contra ellos y sacan el sprite de
         * {@code Sheets.getSignSprite(woodType)}. Este mod no registra ningún
         * {@code BlockEntityRenderer} de cartel.
         *
         * <p>El evento valida que el bloque derive de la superclase común de los ya presentes
         * ({@code SignBlock} en los dos casos) y lanza {@code IllegalArgumentException} si no.
         */
        @SubscribeEvent
        public static void onAddBlockEntityBlocks(BlockEntityTypeAddBlocksEvent event) {
            event.modify(BlockEntityType.SIGN, OLIVE_SIGN.get(), OLIVE_WALL_SIGN.get());
            event.modify(BlockEntityType.HANGING_SIGN, OLIVE_HANGING_SIGN.get(), OLIVE_WALL_HANGING_SIGN.get());
        }

        /** El ítem de la estatua es contenido de este mod, igual que el tab "Deluxe Armory" que lo
         * define ahora en {@link MythosMortalsItems} — ambos viven ya en test/, así que la razón original de
         * este evento (evitar que el paquete de librería item/ importara de test/) desapareció con el
         * cluster #7. Se conserva igualmente: {@code BuildCreativeModeTabContentsEvent} es la vía
         * idiomática de NeoForge para poblar un tab, y mover el {@code accept} al {@code
         * displayItems} de MythosMortalsItems ataría la inicialización estática de las dos clases en un orden
         * que hoy no hace falta. */
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
                // Sólo el ánfora vacía. Las otras cuatro tienen su BlockItem igual —lo necesita el
                // horno, que empareja por ítem— pero no se regalan en el creativo: son estados
                // intermedios y finales de la elaboración, y darlos hechos se salta el juego entero.
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
            // Las barcas reusan la geometría de vanilla tal cual; lo único propio es el nombre de la
            // capa, que es lo que decide la textura. Los carteles NO aparecen aquí a propósito:
            // LayerDefinitions.createRoots() ya recorre WoodType.values() y crea las suyas.
            event.registerLayerDefinition(OLIVE_BOAT_LAYER, BoatModel::createBoatModel);
            event.registerLayerDefinition(OLIVE_CHEST_BOAT_LAYER, BoatModel::createChestBoatModel);
        }

        /** Reuses vanilla's own {@code SonicBoomParticle.Provider} rather than writing a particle
         * class: it is public, it already does exactly what this effect needs (animate the sprite set
         * across a 16-tick life), and it is the very behaviour being replaced — so the screech keeps
         * its feel and only the artwork changes. */
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
            // The library's generic thrown-weapon renderer, told which model and texture to use —
            // there is no spear-specific renderer class any more.
            event.registerEntityRenderer(THROWN_DORI_SPEAR.get(),
                ctx -> new ThrownWeaponRenderer<>(ctx, DoriSpearProjectileModel.LAYER_LOCATION, DORI_SPEAR_TEXTURE));
            event.registerBlockEntityRenderer(OWL_STATUE_BLOCK_ENTITY.get(), StatueRenderer::new);
            // Una sola clase para las dos: no existe un ChestBoatRenderer. La diferencia está en la
            // capa (createChestBoatModel trae la geometría del cofre) y en la textura, que el propio
            // BoatRenderer deriva del nombre de la capa.
            event.registerEntityRenderer(OLIVE_BOAT.get(), ctx -> new BoatRenderer(ctx, OLIVE_BOAT_LAYER));
            event.registerEntityRenderer(OLIVE_CHEST_BOAT.get(), ctx -> new BoatRenderer(ctx, OLIVE_CHEST_BOAT_LAYER));
        }

        /** Registra el contenido demo de este mod en los registros de cliente de la librería.
         * {@code FMLClientSetupEvent} va sobrado de temprano: el horneado es perezoso (primer render)
         * y los comandos de cliente se registran al entrar a un mundo. */
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Registra el material del cartel de olivo en el atlas de carteles. Va en enqueueWork
            // porque el javadoc de addWoodType dice literalmente "Not threadsafe. Enqueue it in
            // client setup". En la práctica Sheets.SIGN_SPRITES se llena desde WoodType.values() al
            // cargar la clase y probablemente ya nos incluiría, pero eso depende de un orden de
            // carga de clases que no controlamos; la llamada es un Map.put idempotente.
            event.enqueueWork(() -> Sheets.addWoodType(OLIVE_WOOD_TYPE));

            StatueRegistry.register(OwlStatueBlock.OWL_TYPE, OWL_STATUE_ITEM, OwlStatueClient.CONFIG);

            HelmetInteriors.register(MythosMortalsItems.ATHENIAN_HELMET, AthenianHelmetInteriorModel.LAYER_LOCATION,
                    Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/athenian_helmet_entity.png"));
            HelmetInteriors.register(MythosMortalsItems.SPARTAN_HELMET, SpartanHelmetInteriorModel.LAYER_LOCATION,
                    Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/spartan_helmet_entity.png"));

            // Both shields are ShieldItems, which skips vanilla's "raise defensively" transform, so
            // they opt in to the library's own smaller blocking nudge instead.
            ShieldPoseNudges.register(MythosMortalsItems.ATHENIAN_SHIELD);
            ShieldPoseNudges.register(MythosMortalsItems.SPARTAN_SHIELD);

            // El estilo de brillo dorado del marinado NO se registra aquí, aunque sea el sitio que
            // parece natural: este evento cuelga de la primera recarga de recursos, que ocurre
            // después de que Minecraft haya construido sus RenderBuffers, y entonces ya es tarde
            // para reservarle buffer fijo. Vive en MythosMortalsGlintStyles, llamado desde el constructor.
        }
    }

    private MythosMortalsRegistry() {}
}
