package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.deluxelib.block.StatueBlockEntity;
import net.darkblade.deluxelib.block.StatueRegistry;
import net.darkblade.deluxelib.block.StatueRenderer;
import net.darkblade.deluxelib.client.render.HelmetInteriors;
import net.darkblade.deluxelib.client.render.ShieldPoseNudges;
import net.darkblade.deluxelib.client.render.ThrownWeaponRenderer;
import net.darkblade.deluxelib.spawn.DeluxeBiomeSpawns;
import net.darkblade.mythosmortals.entity.pegasus.PegasusEntity;
import net.darkblade.mythosmortals.entity.pegasus.client.render.PegasusModel;
import net.darkblade.mythosmortals.entity.pegasus.client.render.PegasusRenderer;
import net.darkblade.mythosmortals.entity.pegasus.menu.PegasusInventoryMenu;
import net.darkblade.mythosmortals.entity.pegasus.client.PegasusInventoryScreen;
import net.darkblade.mythosmortals.entity.pegasus.network.PegasusDashServerPacket;
import net.darkblade.mythosmortals.block.amphora.MarinatingRecipe;
import net.darkblade.mythosmortals.entity.arpy.ArpyEntity;
import net.darkblade.mythosmortals.entity.arpy.client.render.ArpyModel;
import net.darkblade.mythosmortals.entity.arpy.client.render.ArpyRenderer;
import net.darkblade.mythosmortals.entity.athenian.AthenianEntity;
import net.darkblade.mythosmortals.effect.BorealCourageEffect;
import net.darkblade.mythosmortals.entity.athenian.client.render.AthenianHelmetInteriorModel;
import net.darkblade.mythosmortals.entity.athenian.client.render.AthenianModel;
import net.darkblade.mythosmortals.entity.athenian.client.render.AthenianRenderer;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.client.render.MinotaurModel;
import net.darkblade.mythosmortals.entity.minotaur.client.render.MinotaurRenderer;
import net.darkblade.mythosmortals.entity.owl.client.render.CopperOwlModel;
import net.darkblade.mythosmortals.entity.owl.OwlEntity;
import net.darkblade.mythosmortals.entity.owl.client.render.OwlRenderer;
import net.darkblade.mythosmortals.entity.owl.network.OwlAttackServerPacket;
import net.darkblade.mythosmortals.entity.owl.network.OwlMarkServerPacket;
import net.darkblade.mythosmortals.entity.owl.network.OwlOrderAttackServerPacket;
import net.darkblade.mythosmortals.entity.owl.network.OwlSonicAttackServerPacket;
import net.darkblade.mythosmortals.entity.owl.statue.OwlStatueBlock;
import net.darkblade.mythosmortals.entity.owl.client.OwlStatueClient;
import net.darkblade.mythosmortals.entity.spartan.SpartanEntity;
import net.darkblade.mythosmortals.entity.spartan.client.render.SpartanHelmetInteriorModel;
import net.darkblade.mythosmortals.entity.spartan.client.render.SpartanModel;
import net.darkblade.mythosmortals.entity.spartan.client.render.SpartanRenderer;
import net.darkblade.mythosmortals.item.spear.client.render.DoriSpearProjectileModel;
import net.darkblade.mythosmortals.worldgen.structure.MarkedStructurePiece;
import net.darkblade.mythosmortals.worldgen.structure.MarkedTemplateStructure;
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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
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
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosMortalsRegistry {

    private static final Identifier DORI_SPEAR_TEXTURE =
        Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/dori_spear_entity.png");



    public static final ModelLayerLocation OLIVE_BOAT_LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "boat/olive"), "main");
    public static final ModelLayerLocation OLIVE_CHEST_BOAT_LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "chest_boat/olive"), "main");


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
        StatueBlockEntity.registerType(BLOCK_ENTITY_TYPES, "owl_statue", MythosMortalsBlocks.OWL_STATUE::get, OwlStatueBlock.OWL_TYPE);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(Registries.MENU, MythosMortals.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<PegasusInventoryMenu>> PEGASUS_MENU =
        MENU_TYPES.register("pegasus_inventory",
            () -> IMenuTypeExtension.create(PegasusInventoryMenu::new));

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
        MythosMortalsEntities.ENTITY_TYPES.register(bus);
        MENU_TYPES.register(bus);
        MythosMortalsBlocks.BLOCKS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        PARTICLE_TYPES.register(bus);
        MOB_EFFECTS.register(bus);
        DATA_COMPONENTS.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        STRUCTURE_TYPES.register(bus);
        STRUCTURE_PIECES.register(bus);
    }


    public static void registerGameplay() {
        DeluxeBiomeSpawns.builder(() -> MythosMortalsEntities.ATHENIAN.get(), MobCategory.MONSTER)
            .spawnRate(20, 1, 2)
            .biomes(Biomes.PLAINS)
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

    @EventBusSubscriber(modid = MythosMortals.MODID)
    public static final class CommonModEvents {
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
            event.registerLayerDefinition(PegasusModel.LAYER_LOCATION, PegasusModel::createBodyLayer);
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
        public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
            event.register(PEGASUS_MENU.get(), PegasusInventoryScreen::new);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(MythosMortalsEntities.ATHENIAN.get(), AthenianRenderer::new);
            event.registerEntityRenderer(MythosMortalsEntities.ARPY.get(), ArpyRenderer::new);
            event.registerEntityRenderer(MythosMortalsEntities.SPARTAN.get(), SpartanRenderer::new);
            event.registerEntityRenderer(MythosMortalsEntities.MINOTAUR.get(), MinotaurRenderer::new);
            event.registerEntityRenderer(MythosMortalsEntities.OWL.get(), OwlRenderer::new);
            event.registerEntityRenderer(MythosMortalsEntities.PEGASUS.get(), PegasusRenderer::new);
            event.registerEntityRenderer(MythosMortalsEntities.THROWN_DORI_SPEAR.get(),
                ctx -> new ThrownWeaponRenderer<>(ctx, DoriSpearProjectileModel.LAYER_LOCATION, DORI_SPEAR_TEXTURE));
            event.registerBlockEntityRenderer(OWL_STATUE_BLOCK_ENTITY.get(), StatueRenderer::new);
            event.registerEntityRenderer(MythosMortalsEntities.OLIVE_BOAT.get(), ctx -> new BoatRenderer(ctx, OLIVE_BOAT_LAYER));
            event.registerEntityRenderer(MythosMortalsEntities.OLIVE_CHEST_BOAT.get(), ctx -> new BoatRenderer(ctx, OLIVE_CHEST_BOAT_LAYER));
        }


        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> Sheets.addWoodType(MythosMortalsBlocks.OLIVE_WOOD_TYPE));

            StatueRegistry.register(OwlStatueBlock.OWL_TYPE, MythosMortalsBlocks.OWL_STATUE_ITEM, OwlStatueClient.CONFIG);

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
