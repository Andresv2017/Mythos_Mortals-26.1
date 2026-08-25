package net.darkblade.mythosmortals.entity.pegasus;

import net.darkblade.mythosmortals.entity.pegasus.client.input.PegasusClientInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class PegasusFlightController {

    private static final double FORWARD_ACCEL = 0.080;
    private static final double BRAKE = 0.80;
    private static final double STRAFE_ACCEL = 0.030;
    private static final double CLIMB_ACCEL = 0.045;
    private static final double DESCEND_ACCEL = 0.055;
    private static final double SPRINT_MULTIPLIER = 1.7;
    private static final double DRAG = 0.91;
    private static final double MAX_SPEED = 1.6;
    private static final float LEVEL_PITCH_DEADZONE = 8.0F;
    private static final double HOVER_DAMPING = 0.55;

    public static final double DASH_IMPULSE = 1.8;
    public static final int DASH_COOLDOWN_TICKS = 100;

    public static Input riderInput(Player rider) {
        return rider instanceof ServerPlayer server ? server.getLastClientInput() : PegasusClientInput.of(rider);
    }

    public static void travel(PegasusEntity pegasus, Player rider) {
        Input input = riderInput(rider);
        Vec3 look = flightVector(rider);
        Vec3 motion = pegasus.getDeltaMovement();

        if (input.forward()) {
            double thrust = input.sprint() ? FORWARD_ACCEL * SPRINT_MULTIPLIER : FORWARD_ACCEL;
            motion = motion.add(look.scale(thrust));
        } else if (input.backward()) {
            motion = motion.scale(BRAKE);
        }

        int strafe = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
        if (strafe != 0) {
            // Horizontal perpendicular of the look direction, so strafing never adds pitch.
            Vec3 side = new Vec3(look.z, 0.0, -look.x);
            if (side.lengthSqr() > 1.0E-4) {
                motion = motion.add(side.normalize().scale(strafe * STRAFE_ACCEL));
            }
        }

        if (input.jump()) {
            motion = motion.add(0.0, CLIMB_ACCEL, 0.0);
        } else if (input.shift()) {
            motion = motion.add(0.0, -DESCEND_ACCEL, 0.0);
        } else if (!input.forward()) {
            // Altitude hold. Asking for nothing should mean staying put, not sinking: hovering is a
            // thing you do on a winged mount, and it has to be as easy as letting go of the keys.
            motion = new Vec3(motion.x, motion.y * HOVER_DAMPING, motion.z);
        }

        motion = motion.scale(DRAG);
        if (motion.length() > MAX_SPEED) {
            motion = motion.normalize().scale(MAX_SPEED);
        }

        pegasus.setDeltaMovement(motion);
        pegasus.move(MoverType.SELF, motion);
    }

    private static Vec3 flightVector(Player rider) {
        Vec3 look = rider.getLookAngle();
        if (Math.abs(rider.getXRot()) >= LEVEL_PITCH_DEADZONE) {
            return look;
        }
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? look : flat.normalize();
    }

    public static void applyDash(PegasusEntity pegasus, Player rider) {
        Vec3 look = rider.getLookAngle().normalize();
        pegasus.setDeltaMovement(pegasus.getDeltaMovement().add(look.scale(DASH_IMPULSE)));
        pegasus.hurtMarked = true;
    }

    private PegasusFlightController() {}
}
