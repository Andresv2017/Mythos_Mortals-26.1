package net.darkblade.mythosmortals.entity.owl.client.input;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.deluxelib.client.PerchClient;
import net.darkblade.mythosmortals.entity.owl.OwlEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class OwlPerchInput {

    @SubscribeEvent
    public static void onDismountClick(InputEvent.@NotNull InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().isEmpty()) {
            return;
        }
        int perchedId = PerchClient.perchedEntityIdFor(player.getId());
        if (perchedId == -1 || !(player.level().getEntity(perchedId) instanceof OwlEntity)) {
            return;
        }
        event.setCanceled(true);
        PerchClient.requestDismount();
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.@NotNull Key event) {
        OwlPerchTuner.onKey(event.getKey(), event.getAction());
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(@NotNull RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(MythosMortals.MODID)
                .then(Commands.literal("debug")
                        .then(Commands.literal("owlperch")
                                .executes(ctx -> {
                                    boolean enabled = OwlPerchTuner.toggle();
                                    ctx.getSource().sendSuccess(() -> OwlPerchTuner.helpMessage(enabled), false);
                                    return 1;
                                })
                                .then(Commands.literal("reset").executes(ctx -> {
                                    OwlPerchTuner.reset();
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("[owlperch] back to the compiled values"), false);
                                    return 1;
                                })))));
    }

    private OwlPerchInput() {}
}
