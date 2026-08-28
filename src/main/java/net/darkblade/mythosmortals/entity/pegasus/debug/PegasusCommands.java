package net.darkblade.mythosmortals.entity.pegasus.debug;

import net.minecraft.commands.Commands;
import net.darkblade.mythosmortals.core.MythosMortals;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = MythosMortals.MODID)
public final class PegasusCommands {

    @SubscribeEvent
    public static void onRegisterCommands(@NotNull RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(MythosMortals.MODID)
                .then(Commands.literal("debug")
                        .then(Commands.literal("pegasusflight")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(ctx -> {
                                    boolean enabled = PegasusFlightDebug.toggle();
                                    ctx.getSource().sendSuccess(() -> PegasusFlightDebug.helpMessage(enabled), false);
                                    return 1;
                                }))
                        .then(Commands.literal("pegasusdash")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(ctx -> {
                                    boolean enabled = PegasusDashDebug.toggle();
                                    ctx.getSource().sendSuccess(() -> PegasusDashDebug.helpMessage(enabled), false);
                                    return 1;
                                }))));
    }

    private PegasusCommands() {}
}
