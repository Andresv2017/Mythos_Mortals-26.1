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

@Mod(MythosMortals.MODID)
public class MythosMortals {
    public static final String MODID = "mythosmortals";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final NetworkCreator NETWORK = NetworkCreator.create(MODID, 1);


    public MythosMortals(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        MythosMortalsRegistry.register(modEventBus);
        MythosMortalsRegistry.registerGameplay();
        MythosMortalsRegistry.registerPackets();
        MythosMortalsItems.register(modEventBus);

        modEventBus.addListener(NETWORK::register);

        modEventBus.addListener(MythosMortalsDatagen::gatherClientData);
        modEventBus.addListener(MythosMortalsDatagen::gatherServerData);

        if (FMLEnvironment.getDist().isClient()) {
            MythosMortalsGlintStyles.register();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }
}
