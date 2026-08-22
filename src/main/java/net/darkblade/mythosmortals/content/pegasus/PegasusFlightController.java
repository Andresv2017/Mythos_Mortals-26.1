package net.darkblade.mythosmortals.content.pegasus;

import net.darkblade.mythosmortals.content.pegasus.input.PegasusClientInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Free three-dimensional flight for a ridden pegasus.
 *
 * <p>Where the rider looks is where the pegasus goes: the look vector carries the pitch, so climbing
 * and diving are steering rather than separate controls. Space and shift add pure vertical trim on
 * top of that, and the wind dash is a one-off impulse along the same look vector.
 *
 * <p>Both sides run this identically. The rider's key state comes from the {@link Input} record,
 * which exists on the client (local key presses) and on the server (last input packet) alike —
 * unlike {@code zza}/{@code xxa}, which are only ever populated client-side.
 */
public final class PegasusFlightController {

    /** Acceleration applied along the look vector while holding forward. */
    private static final double FORWARD_ACCEL = 0.055;
    /** Velocity multiplier while holding back — a brake, not a reverse. */
    private static final double BRAKE = 0.80;
    private static final double STRAFE_ACCEL = 0.030;
    private static final double CLIMB_ACCEL = 0.045;
    private static final double DESCEND_ACCEL = 0.055;
    /** Per-tick drag. Low enough to keep momentum, high enough to bound top speed. */
    private static final double DRAG = 0.91;
    private static final double MAX_SPEED = 1.6;
    /** Passive sink when neither climbing nor accelerating — a glide, so hovering costs input. */
    private static final double GLIDE_SINK = 0.006;

    public static final double DASH_IMPULSE = 1.8;
    public static final int DASH_COOLDOWN_TICKS = 100;

    /**
     * The rider's key state, from whichever side is asking.
     *
     * <p>{@link PegasusClientInput} is only reached for a non-{@code ServerPlayer} rider, which is
     * impossible server-side, so the client-only class never loads on a dedicated server.
     */
    public static Input riderInput(Player rider) {
        return rider instanceof ServerPlayer server ? server.getLastClientInput() : PegasusClientInput.of(rider);
    }

    /** Applies one tick of ridden flight. Replaces the vanilla travel path entirely. */
    public static void travel(PegasusEntity pegasus, Player rider) {
        Input input = riderInput(rider);
        Vec3 look = rider.getLookAngle();
        Vec3 motion = pegasus.getDeltaMovement();

        if (input.forward()) {
            motion = motion.add(look.scale(FORWARD_ACCEL));
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
        }
        if (input.shift()) {
            motion = motion.add(0.0, -DESCEND_ACCEL, 0.0);
        } else if (!input.jump() && !input.forward()) {
            motion = motion.add(0.0, -GLIDE_SINK, 0.0);
        }

        motion = motion.scale(DRAG);
        if (motion.length() > MAX_SPEED) {
            motion = motion.normalize().scale(MAX_SPEED);
        }

        pegasus.setDeltaMovement(motion);
        pegasus.move(MoverType.SELF, motion);
    }

    /** The Wind Surge: a single impulse along the rider's look vector. */
    public static void applyDash(PegasusEntity pegasus, Player rider) {
        Vec3 look = rider.getLookAngle().normalize();
        pegasus.setDeltaMovement(pegasus.getDeltaMovement().add(look.scale(DASH_IMPULSE)));
        pegasus.hurtMarked = true;
    }

    private PegasusFlightController() {}
}
