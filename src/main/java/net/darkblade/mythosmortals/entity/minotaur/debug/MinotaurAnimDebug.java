package net.darkblade.mythosmortals.entity.minotaur.debug;

import net.darkblade.deluxelib.anim.Animation;
import net.darkblade.deluxelib.anim.BaseAnimation;
import net.darkblade.deluxelib.combat.AttackAnchor;
import net.darkblade.deluxelib.entity.ai.cortex.Cortex;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;

public final class MinotaurAnimDebug {

    private static final Logger LOG = LoggerFactory.getLogger(MinotaurAnimDebug.class);

    public static final double VIEW_RANGE = 24.0;

    private static final int MAX_BAR_CELLS = 22;

    private static final char CELL_HIT = '█';
    private static final char CELL_PLAIN = '▬';
    private static final char CELL_CURSOR = '▉';

    private static final char LOG_CELL_HIT = '#';
    private static final char LOG_CELL_PLAIN = '.';
    private static final char LOG_CELL_CURSOR = 'X';

    private final MinotaurEntity mino;

    private final Set<String> clipsSinKeyframes = new HashSet<>();

    private int lastStateId = Integer.MIN_VALUE;
    private int lastStateTicks;

    public MinotaurAnimDebug(@NotNull MinotaurEntity mino) {
        this.mino = mino;
    }

    public void markMissing(@NotNull String clipName) {
        this.clipsSinKeyframes.add(clipName);
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public void tick() {
        final Cortex<MinotaurEntity, MinotaurState> cortex = this.mino.getCortex();
        final MinotaurState state = this.mino.serverState();
        final BaseAnimation clip = currentClip();

        // Logged whether or not a player is nearby: this is the line that explains the combo.
        final int stateId = state.id();
        if (stateId != this.lastStateId) {
            if (MinotaurCtx.DEBUG_ANIM_CONSOLE && this.lastStateId != Integer.MIN_VALUE) {
                logTransition(stateId);
            }
            this.lastStateId = stateId;
            this.lastStateTicks = 0;
        } else if (cortex != null) {
            this.lastStateTicks = cortex.ticksInState();
        }

        final boolean inCombat = this.mino.getTarget() != null || state != MinotaurState.IDLE;

        if (MinotaurCtx.DEBUG_ANIM_CONSOLE && inCombat) {
            LOG.info("{}", logLine(state, clip, cortex));
        }

        if (MinotaurCtx.DEBUG_ANIM_ACTION_BAR) {
            final ServerPlayer viewer = pickViewer();
            if (viewer != null) {
                // overlay = true is the action bar; the plain overload goes to chat.
                viewer.sendSystemMessage(Component.literal(actionBarLine(state, clip, cortex)), true);
            }
        }
    }

    /**
     * Death trace. Separate from {@link #tick()} because a dying entity runs {@code tickDeath()}
     * instead of {@code aiStep()}, so the normal per-tick line stops exactly when the death
     * animation starts — which is the window we need to see.
     *
     * <p>Read {@code tick} against {@code dur}: if it stalls below {@code dur} while
     * {@code deathTime} keeps climbing, something stopped the animation. If {@code layer0} is not
     * the death clip, another animation took the layer. If {@code deathTime} stops climbing before
     * {@code dur}, the entity was removed too early.
     */
    public void tickDeath() {
        if (!MinotaurCtx.DEBUG_ANIM_CONSOLE) {
            return;
        }
        final var animator = this.mino.animator();
        final String name = animator.getCurrentDeathAnimation();
        final Animation layer0 = animator.getCurrent(0);
        final Animation dead = name == null ? null : animator.getByName(name);
        final int dur = dead instanceof BaseAnimation base ? base.getDuration() : -1;

        LOG.info("[mino {}] DEATH deathTime={} anim={} tick={}/{} playing={} layer0={} state={}",
                this.mino.getId(),
                this.mino.deathTime,
                name == null ? "<null>" : name,
                name == null ? -1 : animator.getAnimationTick(name),
                dur,
                name != null && animator.isPlaying(name),
                layer0 == null ? "-" : layer0.getName(),
                this.mino.serverState());
    }

    @Nullable
    private BaseAnimation currentClip() {
        final Animation current = this.mino.animator().getCurrent(0);
        return current instanceof BaseAnimation base && base.isPlaying() ? base : null;
    }

    @Nullable
    private ServerPlayer pickViewer() {
        final LivingEntity target = this.mino.getTarget();
        final Player viewer = target instanceof Player player
                ? player
                : this.mino.level().getNearestPlayer(this.mino, VIEW_RANGE);
        if (!(viewer instanceof ServerPlayer serverPlayer)
                || viewer.distanceToSqr(this.mino) > VIEW_RANGE * VIEW_RANGE) {
            return null;
        }
        return serverPlayer;
    }

    // ------------------------------------------------------------------
    // ------------------------------------------------------------------

    private @NotNull String actionBarLine(@NotNull MinotaurState state,
                                          @Nullable BaseAnimation clip,
                                          @Nullable Cortex<MinotaurEntity, MinotaurState> cortex) {
        final StringBuilder line = new StringBuilder();

        if (clip == null) {
            line.append("§8(sin clip)");
        } else {
            line.append(this.clipsSinKeyframes.contains(clip.getName()) ? "§c⚠" : "§a").append(clip.getName());
            line.append(" ").append(bar(clip, CELL_PLAIN, CELL_HIT, CELL_CURSOR, true));
            line.append(" §7").append(clip.getTick()).append("§8/").append(clip.getDurationTicks());
        }

        // Only printed when state and clip differ (CHASE→run, IDLE→walk); otherwise it is
        // 20 wasted characters of action bar.
        line.append(" §8│");
        if (clip == null || !state.name().equalsIgnoreCase(clip.getName())) {
            line.append(" §b").append(state.name());
        }

        if (cortex != null) {
            line.append(" §7t").append(cortex.ticksInState());
        }

        final LivingEntity target = this.mino.getTarget();
        if (target != null) {
            line.append(" §7d").append(String.format("%.1f", this.mino.distanceTo(target)));
        }

        if (cortex != null) {
            line.append(" §8│ ").append(meleeCooldown(cortex) > 0
                    ? "§b⏳" + String.format("%.1f", meleeCooldown(cortex) / 20.0F) + "s"
                    : "§a✔");
        }

        return line.toString();
    }

    private @NotNull String logLine(@NotNull MinotaurState state,
                                    @Nullable BaseAnimation clip,
                                    @Nullable Cortex<MinotaurEntity, MinotaurState> cortex) {
        final StringBuilder line = new StringBuilder(prefix());

        if (clip == null) {
            line.append(String.format("%-22s", "(sin clip)"));
        } else {
            final String name = (this.clipsSinKeyframes.contains(clip.getName()) ? "!" : " ") + clip.getName();
            line.append(String.format("%-22s", name));
            line.append(" ").append(bar(clip, LOG_CELL_PLAIN, LOG_CELL_HIT, LOG_CELL_CURSOR, false));
            line.append(String.format(" %2d/%-2d", clip.getTick(), clip.getDurationTicks()));
            line.append(" hit ").append(hitRange(clip));
        }

        line.append(" | ").append(state.name());
        if (cortex != null) {
            line.append(" t").append(cortex.ticksInState());
        }

        final LivingEntity target = this.mino.getTarget();
        line.append(target != null ? String.format(" d%.2f", this.mino.distanceTo(target)) : " d--");
        if (target != null) {
            line.append(String.format(" y%+.0f", yawError(target)));
        }

        if (cortex != null) {
            line.append(String.format(" cd %.1fs", Math.max(0L, meleeCooldown(cortex)) / 20.0F));
        }

        return line.toString();
    }

    private float yawError(LivingEntity target) {
        final double dx = target.getX() - this.mino.getX();
        final double dz = target.getZ() - this.mino.getZ();
        final float toTarget = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        return -Mth.degreesDifference(AttackAnchor.bodyYaw(this.mino), toTarget);
    }

    private void logTransition(int newStateId) {
        final MinotaurState from = resolve(this.lastStateId);
        final MinotaurState to = resolve(newStateId);
        final LivingEntity target = this.mino.getTarget();
        final double distance = target != null ? this.mino.distanceTo(target) : -1.0;

        final StringBuilder line = new StringBuilder(prefix());
        // +1: lastStateTicks was read on the previous tick, and the transition lands on the next.
        line.append(">>> ").append(from != null ? from.name() : this.lastStateId)
                .append(" --(").append(this.lastStateTicks + 1).append(" tk)--> ")
                .append(to != null ? to.name() : newStateId);
        line.append(String.format("   d=%.2f", distance));

        if (from == MinotaurState.ATTACK_HORIZONTAL_1) {
            line.append("  [combo A->B ");
            if (to == MinotaurState.ATTACK_HORIZONTAL_2) {
                line.append("SÍ encadenó");
            } else {
                final boolean inRange = distance >= 0 && distance <= MinotaurCtx.COMBO_CHAIN_RANGE;
                line.append("NO encadenó; rango ").append(MinotaurCtx.COMBO_CHAIN_RANGE)
                        .append(inRange ? " -> dentro, falló el dado" : " -> FUERA de rango");
            }
            line.append("]");
        }

        LOG.info("{}", line);
    }

    private @NotNull String prefix() {
        return "[mino " + this.mino.getId() + "] ";
    }

    @Nullable
    private MinotaurState resolve(int id) {
        for (MinotaurState state : MinotaurState.values()) {
            if (state.id() == id) {
                return state;
            }
        }
        return null;
    }

    private long meleeCooldown(@NotNull Cortex<MinotaurEntity, MinotaurState> cortex) {
        return cortex.context().get(MinotaurCtx.NEXT_MELEE_TIME) - this.mino.level().getGameTime();
    }

    private static @NotNull String hitRange(@NotNull BaseAnimation clip) {
        final Set<Integer> ticks = clip.getFrameEvents().keySet();
        if (ticks.isEmpty()) {
            return "--";
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int tick : ticks) {
            min = Math.min(min, tick);
            max = Math.max(max, tick);
        }
        return min + "-" + max;
    }

    private static @NotNull String bar(@NotNull BaseAnimation clip,
                                       char plain, char hitChar, char cursorChar, boolean colored) {
        final int duration = Math.max(1, clip.getDurationTicks());
        final int cells = Math.min(duration, MAX_BAR_CELLS);
        final int now = clip.getTick();
        final Set<Integer> hitTicks = clip.getFrameEvents().keySet();

        final StringBuilder bar = new StringBuilder();
        String lastColor = "";
        for (int cell = 0; cell < cells; cell++) {
            final int from = cell * duration / cells;
            final int to = (cell + 1) * duration / cells;

            boolean hit = false;
            for (int t = from; t < to; t++) {
                if (hitTicks.contains(t)) {
                    hit = true;
                    break;
                }
            }
            final boolean cursor = now >= from && now < to;
            final boolean past = now >= to;

            if (colored) {
                final String color = cursor ? "§f" : hit ? (past ? "§4" : "§c") : (past ? "§7" : "§8");
                if (!color.equals(lastColor)) {
                    bar.append(color);
                    lastColor = color;
                }
            }
            bar.append(cursor ? cursorChar : hit ? hitChar : plain);
        }
        return bar.toString();
    }
}
