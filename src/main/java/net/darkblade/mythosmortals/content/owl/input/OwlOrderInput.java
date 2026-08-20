package net.darkblade.mythosmortals.content.owl.input;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.deluxelib.client.PossessionClient;
import net.darkblade.mythosmortals.content.owl.OwlAim;
import net.darkblade.mythosmortals.content.owl.network.OwlOrderAttackServerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/**
 * Commanding the owl from the ground: <b>look through the spyglass</b> (hold right-click to zoom),
 * put the crosshair on something, and <b>left-click</b> — the owl goes for that target instead of
 * waiting for it to threaten you.
 *
 * <p><b>Why the raw mouse event and not {@code InteractionKeyMappingTriggered}.</b> That event never
 * fires in this situation: in {@code Minecraft.handleKeybinds}, the {@code player.isUsingItem()}
 * branch drains {@code keyAttack.consumeClick()} <em>without</em> calling {@code startAttack()}, so
 * vanilla never raises an attack interaction while an item is being used — and zooming a spyglass is
 * exactly "an item being used". {@link InputEvent.MouseButton.Pre} sees the press regardless, which
 * is the same reason the sonic screech is bound there ({@code PossessionClient.onMouseButton}): a
 * button vanilla gives no interaction hook for, in a state vanilla suppresses.
 *
 * <p>Zoom is only a field-of-view change, so the player's view vector is unaffected and the aim ray
 * is the ordinary one — with the magnification actually making it precise, which is the point.
 *
 * <p>The press is cancelled so nothing else interprets it. Guarded tightly: it only ever applies
 * while the item being used <em>is</em> a spyglass, so no other click anywhere is affected.
 *
 * <p>Uses the raw left button rather than resolving {@code options.keyAttack}'s binding — the same
 * simplification the sonic screech already makes with the middle button. A player who rebinds attack
 * off the left mouse button would order with the left button anyway.
 */
@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class OwlOrderInput {

    /**
     * How far the designation ray reaches. Far past any normal interaction on purpose — a spyglass is
     * for picking things out at a distance, and having to walk closer to use one defeats it.
     *
     * <p>Not raised further because of a hard limit underneath: the client can only raycast entities
     * the server is actually tracking for it, and beyond roughly 128 blocks a mob simply isn't loaded
     * on this client to be hit at all. The server re-checks its own, wider bound.
     */
    private static final double ORDER_REACH = 96.0;

    @SubscribeEvent
    public static void onSpyglassOrder(InputEvent.MouseButton.@NotNull Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        // While piloting, the left click is the owl's dive and PossessionClient owns it.
        if (PossessionClient.possessed() != null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            return;   // no world, or a GUI is open and the click belongs to it
        }
        // The gesture is "while looking through the spyglass", not merely "while holding one".
        if (!player.isUsingItem() || !player.getUseItem().is(Items.SPYGLASS)) {
            return;
        }
        Entity target = OwlAim.findAimedLiving(mc, player, ORDER_REACH);
        if (target == null) {
            return;   // nothing living under the crosshair — leave the click alone
        }
        event.setCanceled(true);
        // The server validates ownership, range and what may be attacked; this only names a candidate.
        MythosMortals.NETWORK.sendToServer(new OwlOrderAttackServerPacket(target.getId()));
    }

    private OwlOrderInput() {}
}
