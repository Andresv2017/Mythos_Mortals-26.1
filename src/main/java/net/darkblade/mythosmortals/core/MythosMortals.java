package net.darkblade.mythosmortals.core;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.darkblade.deluxelib.network.NetworkCreator;
import net.darkblade.mythosmortals.registry.MythosMortalsDatagen;
import net.darkblade.mythosmortals.registry.MythosMortalsGlintStyles;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MythosMortals.MODID)
public class MythosMortals {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "mythosmortals";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final NetworkCreator NETWORK = NetworkCreator.create(MODID, 1);

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public MythosMortals(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (MythosMortals) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        MythosMortalsRegistry.register(modEventBus);
        MythosMortalsRegistry.registerGameplay();
        // registerPackets() calls NETWORK.regPacket(...) for this mod's own packets (currently just
        // the owl action packets) — it must run before the listener below fires the payload event.
        MythosMortalsRegistry.registerPackets();
        MythosMortalsItems.register(modEventBus);

        modEventBus.addListener(NETWORK::register); // RegisterPayloadHandlersEvent

        modEventBus.addListener(MythosMortalsDatagen::gatherClientData);
        modEventBus.addListener(MythosMortalsDatagen::gatherServerData);

        if (FMLEnvironment.getDist().isClient()) {
            MythosMortalsGlintStyles.register();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
