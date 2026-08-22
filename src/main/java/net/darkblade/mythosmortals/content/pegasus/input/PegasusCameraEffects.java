package net.darkblade.mythosmortals.content.pegasus.input;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.mythosmortals.content.pegasus.PegasusEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * How riding a pegasus looks from behind the camera.
 *
 * <p>The field of view widens with speed, so acceleration is something you feel rather than read off
 * the ground going past: the world stretches as it surges and settles back as it slows, and a dash
 * punches it wide open for a second. The third-person camera also backs off while mounted, because
 * a winged horse at vanilla's four blocks fills the screen.
 *
 * <p>Applies to the bucking phase too — being flung around by an animal you have no control over is
 * exactly when the camera should be selling the speed.
 */
@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class PegasusCameraEffects {

    /** Speed at which the effect starts, and the speed that maxes it out (blocks per tick). */
    private static final double SPEED_FLOOR = 0.35;
    private static final double SPEED_CEILING = 1.60;
    /** Field of view added at full speed, as a fraction. */
    private static final float MAX_BOOST = 0.35F;
    /** Per-tick approach rate. Quick to widen (the surge), slower to relax (the settle). */
    private static final float RISE = 0.28F;
    private static final float FALL = 0.10F;

    /** Third-person camera pull-back while mounted: the pegasus is big and needs room on screen. */
    private static final float GROUND_CAMERA_DISTANCE = 6.0F;
    private static final float FLYING_CAMERA_DISTANCE = 8.5F;
    private static final float CAMERA_EASE = 0.15F;

    private static float boost;
    private static float prevBoost;
    private static float cameraDistance;
    private static float prevCameraDistance;

    /**
     * Advances both easings once per tick, keeping the previous value so the frame events can
     * interpolate between them.
     *
     * <p>Easing straight in the frame events would tie the rate to the player's frame rate; easing
     * on the tick alone and reading the result raw gives twenty visible steps a second, which is
     * what made the camera look like it was dragging. Tick for the value, partial tick for the
     * picture — the same split the rest of the renderer uses.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        float target = 0.0F;
        float targetDistance = 0.0F;

        if (player != null && player.getVehicle() instanceof PegasusEntity pegasus) {
            targetDistance = pegasus.isFlying() ? FLYING_CAMERA_DISTANCE : GROUND_CAMERA_DISTANCE;
            if (pegasus.isFlying()) {
                // The entity's own smoothed, synced speed rather than raw deltaMovement, which
                // jitters tick to tick and made the field of view breathe.
                double ramp = (pegasus.travelSpeed() - SPEED_FLOOR) / (SPEED_CEILING - SPEED_FLOOR);
                target = (float) Mth.clamp(ramp, 0.0, 1.0) * MAX_BOOST;
            }
        }

        prevBoost = boost;
        prevCameraDistance = cameraDistance;
        boost = Mth.lerp(target > boost ? RISE : FALL, boost, target);
        cameraDistance = Mth.lerp(CAMERA_EASE, cameraDistance, targetDistance);
    }

    private static float partialTick() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        float smooth = Mth.lerp(partialTick(), prevBoost, boost);
        if (smooth > 1.0E-3F) {
            event.setNewFovModifier(event.getNewFovModifier() * (1.0F + smooth));
        }
    }

    /**
     * Backs the third-person camera off while mounted, so the pegasus and its wingspan fit on screen
     * instead of filling it. The block ray-cast that shortens the camera runs after this, so pulling
     * back here still never pushes the view through a wall.
     */
    @SubscribeEvent
    public static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        float smooth = Mth.lerp(partialTick(), prevCameraDistance, cameraDistance);
        if (smooth > 0.05F) {
            event.setDistance(Math.max(event.getDistance(), smooth));
        }
    }

    private PegasusCameraEffects() {}
}
