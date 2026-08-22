package net.darkblade.mythosmortals.content.pegasus;

import net.minecraft.commands.Commands;
import net.darkblade.mythosmortals.MythosMortals;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /mythosmortals debug pegasusflight} — toggles {@link PegasusFlightDebug}.
 *
 * <p>A server command, not a client one: the flight state machine lives entirely on the server, and
 * the readout has to come from the side that owns the timers.
 */
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
                                }))));
    }

    private PegasusCommands() {}
}
