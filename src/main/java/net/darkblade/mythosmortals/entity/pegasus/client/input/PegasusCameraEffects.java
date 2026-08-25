package net.darkblade.mythosmortals.entity.pegasus.client.input;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.mythosmortals.entity.pegasus.PegasusEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class PegasusCameraEffects {

    private static final double SPEED_FLOOR = 0.35;
    private static final double SPEED_CEILING = 1.60;
    private static final float MAX_BOOST = 0.35F;
    private static final float RISE = 0.28F;
    private static final float FALL = 0.10F;

    private static final float GROUND_CAMERA_DISTANCE = 6.0F;
    private static final float FLYING_CAMERA_DISTANCE = 8.5F;
    private static final float CAMERA_EASE = 0.15F;

    private static float boost;
    private static float prevBoost;
    private static float cameraDistance;
    private static float prevCameraDistance;

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

    @SubscribeEvent
    public static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        float smooth = Mth.lerp(partialTick(), prevCameraDistance, cameraDistance);
        if (smooth > 0.05F) {
            event.setDistance(Math.max(event.getDistance(), smooth));
        }
    }

    private PegasusCameraEffects() {}
}
