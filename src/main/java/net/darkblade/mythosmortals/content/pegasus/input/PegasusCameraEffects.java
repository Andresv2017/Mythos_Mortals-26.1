package net.darkblade.mythosmortals.content.pegasus.input;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.mythosmortals.content.pegasus.PegasusEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Widens the field of view with the pegasus' speed, so acceleration is something you feel rather
 * than read off the ground going past. The world stretches as it surges and settles back as it
 * slows; a dash punches it wide open for a second.
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

    private static float boost;

    /**
     * Smoothed on the tick, not on the frame: the event below fires once per frame, so easing there
     * would make the effect depend on the player's frame rate.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        float target = 0.0F;

        if (player != null && player.getVehicle() instanceof PegasusEntity pegasus && pegasus.isFlying()) {
            double speed = pegasus.getDeltaMovement().length();
            double ramp = (speed - SPEED_FLOOR) / (SPEED_CEILING - SPEED_FLOOR);
            target = (float) Mth.clamp(ramp, 0.0, 1.0) * MAX_BOOST;
        }

        boost = Mth.lerp(target > boost ? RISE : FALL, boost, target);
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (boost > 1.0E-3F) {
            event.setNewFovModifier(event.getNewFovModifier() * (1.0F + boost));
        }
    }

    private PegasusCameraEffects() {}
}
