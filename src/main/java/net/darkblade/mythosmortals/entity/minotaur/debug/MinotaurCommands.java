package net.darkblade.mythosmortals.entity.minotaur.debug;

import net.minecraft.commands.Commands;
import net.darkblade.mythosmortals.core.MythosMortals;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = MythosMortals.MODID)
public final class MinotaurCommands {

    @SubscribeEvent
    public static void onRegisterCommands(@NotNull RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(MythosMortals.MODID)
                .then(Commands.literal("debug")
                        .then(Commands.literal("minotauranim")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(ctx -> {
                                    boolean enabled = MinotaurAnimDebug.toggleActionBar();
                                    ctx.getSource().sendSuccess(() -> MinotaurAnimDebug.helpMessage(enabled, false), false);
                                    return 1;
                                }))
                        .then(Commands.literal("minotauranimlog")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(ctx -> {
                                    boolean enabled = MinotaurAnimDebug.toggleConsole();
                                    ctx.getSource().sendSuccess(() -> MinotaurAnimDebug.helpMessage(enabled, true), false);
                                    return 1;
                                }))));
    }

    private MinotaurCommands() {}
}
