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

public final class PegasusFlightDebug {

    private static final Logger LOG = LoggerFactory.getLogger(PegasusFlightDebug.class);

    private static final double VIEW_RANGE = 48.0;
    private static final int LOG_INTERVAL = 5;

    private static boolean enabled;

    private final PegasusEntity pegasus;

    public PegasusFlightDebug(PegasusEntity pegasus) {
        this.pegasus = pegasus;
    }

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

    private String phase() {
        if (this.pegasus.isTakingOff()) return "TAKEOFF";
        if (this.pegasus.isLanding()) return "LAND";
        if (this.pegasus.isSeekingGround()) return "SEEK";
        if (this.pegasus.isFlying()) return this.pegasus.isVehicle() ? "FLY-RID" : "FLY";
        return this.pegasus.isVehicle() ? "GND-RID" : "GROUND";
    }

    private String gait() {
        boolean sprinting = this.pegasus.isSprinting();
        if (this.pegasus.isFlying()) {
            if (this.pegasus.travelSpeed() < PegasusEntity.FLY_MOVE_SPEED) return "fly_idle";
            return sprinting ? "fly_sprint" : "fly";
        }
        if (this.pegasus.travelSpeed() < PegasusEntity.WALK_SPEED) return "idle";
        return sprinting ? "sprint" : "walk";
    }

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

    private String animation() {
        Animation current = this.pegasus.animator().getCurrent(0);
        return current == null ? "-" : current.getName();
    }
}
