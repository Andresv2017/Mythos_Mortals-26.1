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

    // The dash writes into the same deltaMovement that travel() clamps to MAX_SPEED every tick, so
    // an impulse on its own is erased before it can express itself: sprint cruise already sits at
    // ~1.375 (FORWARD_ACCEL * SPRINT_MULTIPLIER against DRAG), and clamping the impulse to 1.6 left
    // a 16% bump that DRAG ate within a few ticks. That is why the dash "did nothing". The fix is a
    // boost window that raises the ceiling for as long as the lunge lasts, then decays back into
    // normal flight instead of being cut off.
    public static final double DASH_IMPULSE = 2.4;
    public static final int DASH_COOLDOWN_TICKS = 100;
    /** Ticks the raised speed ceiling lasts. Also the window in which the dash deals damage. */
    public static final int DASH_BOOST_TICKS = 12;
    private static final double DASH_MAX_SPEED = 3.4;
    public static final float DASH_DAMAGE = 8.0F;
    public static final double DASH_KNOCKBACK = 1.1;

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
        double cap = speedCap(pegasus);
        if (motion.length() > cap) {
            motion = motion.normalize().scale(cap);
        }

        pegasus.setDeltaMovement(motion);
        pegasus.move(MoverType.SELF, motion);
    }

    /**
     * Normal flight is capped at {@link #MAX_SPEED}; a dash raises the ceiling and lets it fall back
     * over {@link #DASH_BOOST_TICKS}, so the lunge decays into ordinary flight rather than hitting a
     * wall. The window is derived from the synced dash cooldown rather than a private field, so the
     * client predicts the same ceiling the server enforces and the dash does not rubber-band.
     */
    private static double speedCap(PegasusEntity pegasus) {
        int boost = pegasus.dashBoostTicks();
        if (boost <= 0) {
            return MAX_SPEED;
        }
        return MAX_SPEED + (DASH_MAX_SPEED - MAX_SPEED) * (boost / (double) DASH_BOOST_TICKS);
    }

    private static Vec3 flightVector(Player rider) {
        Vec3 look = rider.getLookAngle();
        if (Math.abs(rider.getXRot()) >= LEVEL_PITCH_DEADZONE) {
            return look;
        }
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? look : flat.normalize();
    }

    /**
     * Adds the lunge to the current velocity. Only ever called on the side that owns the mount's
     * movement — see {@code PegasusEntity#applyDashImpulse}.
     *
     * <p>Deliberately does NOT set {@code hurtMarked}. On the server that flag makes ServerEntity
     * broadcast a ClientboundSetEntityMotionPacket to everyone tracking the entity <em>and the
     * rider</em>, carrying the server's own velocity — which for a ridden mount is zero, because
     * {@code LivingEntity#travelRidden} zeroes it every tick when {@code canSimulateMovement()} is
     * false. The rider's client would then {@code lerpMotion(0,0,0)} and kill its own dash one to
     * three ticks in, which is exactly what the trace showed it doing.
     */
    public static void applyDash(PegasusEntity pegasus, Player rider) {
        Vec3 look = rider.getLookAngle().normalize();
        pegasus.setDeltaMovement(pegasus.getDeltaMovement().add(look.scale(DASH_IMPULSE)));
    }

    private PegasusFlightController() {}
}
