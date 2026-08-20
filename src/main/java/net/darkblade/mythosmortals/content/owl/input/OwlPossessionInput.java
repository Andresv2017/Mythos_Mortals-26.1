package net.darkblade.mythosmortals.content.owl.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.deluxelib.client.PossessionClient;
import net.darkblade.deluxelib.client.PossessionInputHandler;
import net.darkblade.mythosmortals.content.owl.OwlAim;
import net.darkblade.mythosmortals.content.owl.OwlEntity;
import net.darkblade.mythosmortals.content.owl.network.OwlAttackServerPacket;
import net.darkblade.mythosmortals.content.owl.network.OwlMarkServerPacket;
import net.darkblade.mythosmortals.content.owl.network.OwlSonicAttackServerPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;


/**
 * The owl-specific half of Athena's Sight — demo content, not library. {@link PossessionClient} owns
 * everything generic (camera, body freeze, input forwarding, click cancelling); this class owns the
 * two things that are game design rather than infrastructure:
 *
 * <ul>
 *   <li>the toggle keybind, which decides <i>when</i> a session starts and ends, and</li>
 *   <li>a {@link PossessionInputHandler}, which decides what the owl <i>does</i> with the pilot's
 *       clicks: dive strike, sonic screech, and marking the aimed enemy.</li>
 * </ul>
 *
 * Client-only: it touches {@link KeyMapping} and {@link Minecraft}, so only the {@code Dist.CLIENT}
 * handlers below ever load it.
 */
@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class OwlPossessionInput {

    /** Single toggle for Athena's Sight: pressed while free, invokes the nearest bonded owl
     * companion (no need to click it, since it's flying beside you); pressed while already
     * possessing, ends the session early. */
    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.mythosmortals.athena_sight",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H,
            KeyMapping.Category.GAMEPLAY);

    /** How far the marking raycast reaches (blocks). */
    private static final double MARK_REACH = 40.0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        while (TOGGLE_KEY.consumeClick()) {
            if (PossessionClient.possessed() == null) {
                // Not currently possessing anything: the toggle key activates. No entity id needed
                // — the server finds the nearest bonded owl companion itself.
                PossessionClient.requestActivate();
            } else {
                // Same key ends an active session — the server drops CONTROLLER_ID and the library
                // disengages on a later tick.
                PossessionClient.requestCancel();
            }
        }
    }

    /** What the owl does with the pilot's clicks, once the library has cancelled them for the body. */
    private static final class OwlActions implements PossessionInputHandler {

        @Override
        public void onAttack(LivingEntity possessed) {
            if (possessed instanceof OwlEntity owl) {
                // Fire the owl's own custom dive-strike server-side (its HitWindow handles who it
                // catches).
                MythosMortals.NETWORK.sendToServer(new OwlAttackServerPacket(owl.getId()));
            }
        }

        @Override
        public void onUse(LivingEntity possessed) {
            if (!(possessed instanceof OwlEntity owl)) {
                return;
            }
            // Right-click marks the aimed enemy with a see-through-walls glow.
            Entity target = OwlAim.findAimedLiving(Minecraft.getInstance(), owl, MARK_REACH);
            if (target != null) {
                MythosMortals.NETWORK.sendToServer(new OwlMarkServerPacket(owl.getId(), target.getId()));
            }
        }

        @Override
        public void onMouseButton(LivingEntity possessed, int button) {
            // Mouse wheel (middle button) fires the sonic screech. The library already restricts the
            // dispatch to a middle-button press outside any GUI screen, and cancels it there; this
            // re-checks the button so a future dispatch of some other button can't fire the screech.
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && possessed instanceof OwlEntity owl) {
                MythosMortals.NETWORK.sendToServer(new OwlSonicAttackServerPacket(owl.getId()));
            }
        }
    }

    /** Finds the living entity the player is aiming at from the owl's eyes, within {@code reach}.
     * Forgiving on purpose: each candidate's box is inflated ~1 block and we pick the one nearest
     * along the look ray, so marking a mob at range doesn't need pixel-perfect aim. */

    private OwlPossessionInput() {}

    /** Mod-bus half: registers the toggle keybind and the owl's possession input handler. */
    @EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_KEY);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            PossessionClient.registerInputHandler(new OwlActions());
        }

        private ModEvents() {}
    }
}
