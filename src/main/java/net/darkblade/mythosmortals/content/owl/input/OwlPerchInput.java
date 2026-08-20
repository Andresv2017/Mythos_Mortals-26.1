package net.darkblade.mythosmortals.content.owl.input;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.deluxelib.client.PerchClient;
import net.darkblade.mythosmortals.content.owl.OwlEntity;
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

/**
 * Client-side input and debug wiring for the demo owl's perch — the parts of perching that are game
 * design or debug tooling rather than mechanism, and so stay out of the library:
 *
 * <ul>
 *   <li>The gesture that releases the owl (right-click with an empty hand).</li>
 *   <li>Routing raw numpad presses to {@link OwlPerchTuner}.</li>
 *   <li>The {@code /deluxelib debug owlperch} subcommand that toggles that tuner.</li>
 * </ul>
 *
 * <p>Same split the possession refactor settled on: the library owns the mechanism and exposes an
 * entry point ({@code PerchClient.requestDismount()}), the consumer decides what triggers it.
 */
@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class OwlPerchInput {

    /**
     * Right-click ANYWHERE with an empty hand, while your bonded owl is perched on you, dismounts it.
     *
     * <p>Not a click on the owl itself (which {@code OwlEntity#mobInteract} would also handle): a
     * perched owl's hitbox tracks its real position a tick behind the arm-welded render, so hitting
     * it precisely is fiddly. Same "no need to aim at the bird" idea as the possession-activation
     * keybind.
     *
     * <p><b>Owls only</b>, and the type check is the point. Perching is a library-wide mechanic that
     * any {@code Perchable} uses, so gating on "is something perched" would hand this gesture — and
     * the {@code setCanceled} that swallows the click with it — to every other perchable creature,
     * whose owner may well have chosen a different release gesture entirely.
     */
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

    /**
     * Registers {@code /mythosmortals debug owlperch}. Lives here rather than in DeluxeLib's
     * {@code DeluxeCommands} because this tuner is Mythos & Mortals content, not library code — it
     * gets its own {@code mythosmortals debug} root rather than merging into DeluxeLib's.
     */
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
