package net.darkblade.mythosmortals.entity.pegasus.debug;

import net.darkblade.mythosmortals.entity.pegasus.PegasusEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Traces one dash from the keypress to the movement it does or does not produce.
 *
 * <p>The dash is a multi-component path — client keypress, client impulse, client flight maths,
 * packet, server validation, server position reconciliation — and a failure anywhere in it looks
 * identical from the saddle: nothing happens. This logs each boundary separately so the failing one
 * can be read off instead of guessed at.
 *
 * <p><b>The column that matters is {@code moved}</b>: the entity's real position delta for that
 * tick, independent of what any velocity field claims. Read it against {@code |v|}:
 * <ul>
 *   <li>{@code |v|} low on the client → the impulse never landed; check the gate line.
 *   <li>{@code |v|} high but {@code moved} ~0 → something is eating the movement (collision,
 *       {@code travel()} not running, or the cap).
 *   <li>client {@code moved} high, server {@code moved} ~0 → the two sides disagree and the
 *       server is not accepting the ride.
 * </ul>
 *
 * <p>{@code canSim} is {@code canSimulateMovement()}. Expect {@code N} on the server whenever a
 * player is aboard: {@code LivingEntity#travelRidden} then skips {@code travel()} entirely and
 * calls {@code setDeltaMovement(Vec3.ZERO)} instead, so a server-side impulse cannot survive to the
 * next tick. That is not a bug in this mod, it is what client-authoritative mounts do.
 *
 * <p>The toggle is a static shared by both sides, so the trace covers client and server in
 * singleplayer. On a dedicated server the two run in separate JVMs and only the server half prints.
 */
public final class PegasusDashDebug {

    private static final Logger LOG = LoggerFactory.getLogger(PegasusDashDebug.class);

    /** Ticks to keep tracing after the boost window closes, to catch a snap-back. */
    private static final int TAIL_TICKS = 10;

    private static boolean enabled;

    private final PegasusEntity pegasus;
    private int tail;
    private int tickInDash;
    /**
     * Previous sampled position. Tracked here rather than read from {@code Entity.xo}, which in
     * 26.1 is only written by the teleport paths and is not the previous tick's position.
     */
    private Vec3 lastPos;

    public PegasusDashDebug(PegasusEntity pegasus) {
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
                ? Component.literal("[pegasusdash] on — mount a tamed, bridled pegasus, take off and press R. "
                        + "Traces the keypress gate, the impulse, and then every tick of the lunge on both "
                        + "sides. Key column is 'moved' (real position delta) read against '|v|' (velocity). "
                        + "Output goes to the log, not the chat.")
                        .withStyle(ChatFormatting.GREEN)
                : Component.literal("[pegasusdash] off").withStyle(ChatFormatting.GRAY);
    }

    private String side() {
        return this.pegasus.level().isClientSide() ? "CLIENT" : "SERVER";
    }

    /** Logged on the client the moment R is consumed, before anything else happens. */
    public static void gate(PegasusEntity pegasus, Player rider) {
        if (!enabled) {
            return;
        }
        LOG.info("[dash GATE  ] peg {} tamed={} bridle={} flying={} controller={} cd={} boost={} => canDash={}",
                pegasus.getId(),
                yn(pegasus.isTamed()),
                yn(pegasus.hasBridle()),
                yn(pegasus.isFlying()),
                yn(pegasus.getControllingPassenger() == rider),
                pegasus.dashCooldown(),
                pegasus.dashBoostTicks(),
                yn(pegasus.canDash(rider)));
    }

    /** Logged wherever the impulse is applied, so the two sides can be compared. */
    public void impulse(Vec3 before, Vec3 after, Vec3 look) {
        if (!enabled) {
            return;
        }
        this.tickInDash = 0;
        this.tail = TAIL_TICKS;
        // Seeded here so the first traced tick measures the impulse's own tick, not a stale delta.
        this.lastPos = this.pegasus.position();
        LOG.info("[dash IMPULSE] {} peg {} |v| {} -> {} look({},{},{})",
                this.side(),
                this.pegasus.getId(),
                String.format("%.3f", before.length()),
                String.format("%.3f", after.length()),
                String.format("%.2f", look.x),
                String.format("%.2f", look.y),
                String.format("%.2f", look.z));
    }

    /** Logged on the server when the packet lands, so a dropped or rejected packet is visible. */
    public void packet(boolean accepted) {
        if (!enabled) {
            return;
        }
        LOG.info("[dash PACKET ] {} peg {} accepted={}", this.side(), this.pegasus.getId(), yn(accepted));
    }

    /** Per-tick trace, called from aiStep on both sides. */
    public void tick() {
        if (!enabled) {
            return;
        }
        int boost = this.pegasus.dashBoostTicks();
        if (boost <= 0 && this.tail <= 0) {
            return;
        }
        if (boost <= 0) {
            this.tail--;
        }

        Vec3 motion = this.pegasus.getDeltaMovement();
        // Ground truth: how far the entity actually got, whatever the velocity field claims. Sampled
        // at the top of aiStep, so this is the distance the PREVIOUS tick's travel() delivered —
        // one tick behind the other columns on the same line, which is what you want when asking
        // "did last tick's velocity turn into movement?".
        Vec3 pos = this.pegasus.position();
        double moved = this.lastPos == null ? 0.0 : pos.distanceTo(this.lastPos);
        this.lastPos = pos;

        LOG.info("[dash {}] t+{} canSim={} boost={} cd={} |v|={} moved={} fly={} land={} rider={}",
                String.format("%-6s", this.side()),
                String.format("%-2d", this.tickInDash++),
                yn(this.pegasus.canSimulateMovement()),
                boost,
                this.pegasus.dashCooldown(),
                String.format("%.3f", motion.length()),
                String.format("%.3f", moved),
                yn(this.pegasus.isFlying()),
                yn(this.pegasus.isLanding()),
                this.pegasus.getControllingPassenger() == null ? "none" : "yes");
    }

    private static String yn(boolean value) {
        return value ? "Y" : "N";
    }
}
