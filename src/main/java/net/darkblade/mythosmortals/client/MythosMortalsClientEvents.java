package net.darkblade.mythosmortals.client;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.deluxelib.block.StatueRegistry;
import net.darkblade.deluxelib.block.StatueRenderer;
import net.darkblade.deluxelib.client.render.HelmetInteriors;
import net.darkblade.deluxelib.client.render.ShieldPoseNudges;
import net.darkblade.deluxelib.client.render.ThrownWeaponRenderer;
import net.darkblade.mythosmortals.entity.pegasus.client.render.PegasusModel;
import net.darkblade.mythosmortals.entity.pegasus.client.render.PegasusRenderer;
import net.darkblade.mythosmortals.entity.pegasus.client.PegasusInventoryScreen;
import net.darkblade.mythosmortals.entity.arpy.client.render.ArpyModel;
import net.darkblade.mythosmortals.entity.arpy.client.render.ArpyRenderer;
import net.darkblade.mythosmortals.entity.athenian.client.render.AthenianHelmetInteriorModel;
import net.darkblade.mythosmortals.entity.athenian.client.render.AthenianModel;
import net.darkblade.mythosmortals.entity.athenian.client.render.AthenianRenderer;
import net.darkblade.mythosmortals.entity.minotaur.client.render.MinotaurModel;
import net.darkblade.mythosmortals.entity.minotaur.client.render.MinotaurRenderer;
import net.darkblade.mythosmortals.entity.owl.client.render.CopperOwlModel;
import net.darkblade.mythosmortals.entity.owl.client.render.OwlRenderer;
import net.darkblade.mythosmortals.entity.owl.statue.OwlStatueBlock;
import net.darkblade.mythosmortals.entity.owl.client.OwlStatueClient;
import net.darkblade.mythosmortals.entity.spartan.client.render.SpartanHelmetInteriorModel;
import net.darkblade.mythosmortals.entity.spartan.client.render.SpartanModel;
import net.darkblade.mythosmortals.entity.spartan.client.render.SpartanRenderer;
import net.darkblade.mythosmortals.item.spear.client.render.DoriSpearProjectileModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.particle.SonicBoomParticle;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.darkblade.mythosmortals.registry.MythosMortalsBlockEntities;
import net.darkblade.mythosmortals.registry.MythosMortalsBlocks;
import net.darkblade.mythosmortals.registry.MythosMortalsEntities;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.darkblade.mythosmortals.registry.MythosMortalsMenus;
import net.darkblade.mythosmortals.registry.MythosMortalsParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class MythosMortalsClientEvents {

    private static final Identifier DORI_SPEAR_TEXTURE =
        Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/dori_spear_entity.png");

    private static final ModelLayerLocation OLIVE_BOAT_LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "boat/olive"), "main");
    private static final ModelLayerLocation OLIVE_CHEST_BOAT_LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "chest_boat/olive"), "main");

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
        event.registerSpriteSet(MythosMortalsParticles.OWL_BOOM.get(), SonicBoomParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MythosMortalsMenus.PEGASUS_MENU.get(), PegasusInventoryScreen::new);
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
        event.registerBlockEntityRenderer(MythosMortalsBlockEntities.OWL_STATUE_BLOCK_ENTITY.get(), StatueRenderer::new);
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

    private MythosMortalsClientEvents() {
    }
}
