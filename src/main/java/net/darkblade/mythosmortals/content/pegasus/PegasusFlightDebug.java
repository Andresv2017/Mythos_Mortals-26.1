package net.darkblade.mythosmortals.content.pegasus;

import net.darkblade.deluxelib.anim.Animation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Narrates the flight state machine, tick by tick, to the action bar and the server log.
 *
 * <p>Everything that decides how a pegasus flies is spread across three goals in the library, a
 * handful of timers, two navigations and a set of synced flags — and almost none of it is visible
 * from inside the game. This prints the lot on one line so a flight that looks wrong can be read
 * rather than guessed at.
 *
 * <pre>
 *   [peg 214] FLY     alt 14.2  spd 0.31 (fly)      leg 84/198  nav 22.4m  yaw 130→ 96  tilt  8/-12  hover -   idle_fly
 *   [peg 214] SEEK    alt  7.8  spd 0.28 (fly)      leg 198/198 nav done   yaw 130→130  tilt -6/  0  hover -   fly
 *   [peg 214] LAND    alt  1.9  spd 0.04 (fly_idle) leg 205/198 nav done   yaw 130→130  tilt 14/  0  hover -   landing
 * </pre>
 *
 * <p>Server-side, one instance per entity, off by default. Toggle it with
 * {@code /mythosmortals debug pegasusflight}.
 */
public final class PegasusFlightDebug {

    private static final Logger LOG = LoggerFactory.getLogger(PegasusFlightDebug.class);

    /** How close a player must be to see the readout on their action bar. */
    private static final double VIEW_RANGE = 48.0;
    /** Ticks between log lines. The action bar updates every tick; the console would drown. */
    private static final int LOG_INTERVAL = 5;

    private static boolean enabled;

    private final PegasusEntity pegasus;

    public PegasusFlightDebug(PegasusEntity pegasus) {
        this.pegasus = pegasus;
    }

    /** @return the new state */
    public static boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static Component helpMessage(boolean on) {
        return on
                ? Component.literal("[pegasusflight] on — mount or stand within 48 blocks of a pegasus. "
                        + "Columns: phase, altitude over terrain, speed (and the gait it selects), "
                        + "flight timer, navigation, yaw now→wanted, pitch/roll, hover, animation.")
                        .withStyle(ChatFormatting.GREEN)
                : Component.literal("[pegasusflight] off").withStyle(ChatFormatting.GRAY);
    }

    public void tick() {
        if (!enabled || this.pegasus.level().isClientSide()) {
            return;
        }
        String line = this.line();

        if (this.pegasus.tickCount % LOG_INTERVAL == 0) {
            LOG.info("{}", line);
        }
        for (Player player : this.pegasus.level().players()) {
            if (player instanceof ServerPlayer viewer && viewer.distanceTo(this.pegasus) <= VIEW_RANGE) {
                viewer.sendSystemMessage(Component.literal(line), true);
            }
        }
    }

    private @NotNull String line() {
        Vec3 motion = this.pegasus.getDeltaMovement();
        double altitude = this.pegasus.getY() - this.pegasus.groundHeightForDebug();

        return String.format(
                "[peg %d] %-7s alt %5.1f  spd %.2f (%s) v%+.2f  leg %d/%d rest %d  nav %-9s yaw %4.0f→%4.0f  "
                        + "tilt %+5.1f/%+5.1f  hover %s  gnd-nav %s  %s  %s",
                this.pegasus.getId(),
                this.phase(),
                altitude,
                this.pegasus.travelSpeed(),
                this.gait(),
                motion.y,
                this.pegasus.flightDurationForDebug(),
                this.pegasus.getMaxFlightTicks(),
                this.pegasus.groundRestForDebug(),
                this.navigation(),
                this.pegasus.getYRot(),
                this.bearingToTarget(),
                this.pegasus.flightPitch,
                this.pegasus.flightRoll,
                this.pegasus.isFlightHovering() ? "yes" : "-",
                this.pegasus.isUsingGroundNav() ? "yes" : "-",
                this.pegasus.tameState(),
                this.animation());
    }

    /** The phase the flight machinery believes it is in, in the order the goals test for it. */
    private String phase() {
        if (this.pegasus.isTakingOff()) return "TAKEOFF";
        if (this.pegasus.isLanding()) return "LAND";
        if (this.pegasus.isSeekingGround()) return "SEEK";
        if (this.pegasus.isFlying()) return this.pegasus.isVehicle() ? "FLY-RID" : "FLY";
        return this.pegasus.isVehicle() ? "GND-RID" : "GROUND";
    }

    /** Which gait the animation conditions will pick for the current speed and sprint flag. */
    private String gait() {
        boolean sprinting = this.pegasus.isSprinting();
        if (this.pegasus.isFlying()) {
            if (this.pegasus.travelSpeed() < PegasusEntity.FLY_MOVE_SPEED) return "fly_idle";
            return sprinting ? "fly_sprint" : "fly";
        }
        if (this.pegasus.travelSpeed() < PegasusEntity.WALK_SPEED) return "idle";
        return sprinting ? "sprint" : "walk";
    }

    /**
     * Compass bearing from the pegasus to whatever it is currently navigating toward, in the same
     * frame as {@code getYRot()}. Printed next to the current yaw so the two can be compared: if the
     * gap never closes, the body is not turning fast enough to converge on the target.
     *
     * <p>Returns the current yaw when there is nothing to steer toward, so the pair reads as "no
     * error" rather than as a wild number.
     */
    private float bearingToTarget() {
        var path = this.pegasus.getNavigation().getPath();
        if (this.pegasus.getNavigation().isDone() || path == null) {
            return this.pegasus.getYRot();
        }
        Vec3 target = Vec3.atCenterOf(path.getTarget());
        Vec3 to = target.subtract(this.pegasus.position());
        return (float) (Math.toDegrees(Math.atan2(to.z, to.x))) - 90.0F;
    }

    private String navigation() {
        if (this.pegasus.getNavigation().isDone()) {
            return "done";
        }
        var path = this.pegasus.getNavigation().getPath();
        if (path == null) {
            return "none";
        }
        Vec3 target = Vec3.atCenterOf(path.getTarget());
        return String.format("%.1fm", this.pegasus.position().distanceTo(target));
    }

    /** The clip actually playing, which is the thing a wrong-looking flight usually disagrees with. */
    private String animation() {
        Animation current = this.pegasus.animator().getCurrent(0);
        return current == null ? "-" : current.getName();
    }
}
