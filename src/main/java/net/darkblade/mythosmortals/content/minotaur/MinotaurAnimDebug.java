package net.darkblade.mythosmortals.content.minotaur;

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

/**
 * Narrates, tick by tick, which clip should be playing and where its hit window falls — to the
 * action bar and to the server log. Half the repertoire still has no keyframes, so without this
 * those attacks land from invisible poses.
 *
 * <p>Server-side, one instance per entity. Impact ticks come from
 * {@link BaseAnimation#getFrameEvents()}, exactly where {@code HitWindow#applyTo} registered its
 * sweep, so the bar cannot drift from the real damage window.</p>
 *
 * <pre>
 *   [mino 41] attack_horizontal_1  ....X####.......  5/16  hit 4-8  | ATTACK_HORIZONTAL_1 t5 d2.91 y-3 cd 0.0s
 * </pre>
 */
public final class MinotaurAnimDebug {

    private static final Logger LOG = LoggerFactory.getLogger(MinotaurAnimDebug.class);

    /** Radio en el que un jugador ve el HUD cuando el minotauro no lo tiene como objetivo. */
    public static final double VIEW_RANGE = 24.0;

    /** Tope de celdas de la barra: los clips largos (muerte, stun) se comprimen en vez de crecer. */
    private static final int MAX_BAR_CELLS = 22;

    // Action bar (glifos de bloque, se ven bien en la fuente de MC)
    private static final char CELL_HIT = '█';
    private static final char CELL_PLAIN = '▬';
    private static final char CELL_CURSOR = '▉';

    // Consola (ASCII: cualquier terminal, y alineado en monoespaciada)
    private static final char LOG_CELL_HIT = '#';
    private static final char LOG_CELL_PLAIN = '.';
    private static final char LOG_CELL_CURSOR = 'X';

    private final MinotaurEntity mino;

    /**
     * Clips whose {@code AnimSource} is still {@code () -> null}, filled by
     * {@code MinotaurEntity#sinKeyframes}. Cannot be derived at runtime: invoking the supplier
     * would classload {@link MinotaurAnimation}, which is client-only.
     */
    private final Set<String> clipsSinKeyframes = new HashSet<>();

    /** Estado del FSM en el tick anterior, para detectar y loguear las transiciones. */
    private int lastStateId = Integer.MIN_VALUE;
    /** Ticks que duró el estado del que se acaba de salir. */
    private int lastStateTicks;

    public MinotaurAnimDebug(@NotNull MinotaurEntity mino) {
        this.mino = mino;
    }

    /** Marca un clip como todavía sin keyframes. La llama {@code MinotaurEntity#sinKeyframes}. */
    public void markMissing(@NotNull String clipName) {
        this.clipsSinKeyframes.add(clipName);
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    /** Llamado una vez por tick de servidor desde {@link MinotaurEntity#tick()}. */
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

        // En reposo y sin nadie a quien perseguir no hay nada que analizar: no se ensucia el log.
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

    /** El clip de mayor prioridad que está sonando ahora mismo en la capa 0, o null. */
    @Nullable
    private BaseAnimation currentClip() {
        final Animation current = this.mino.animator().getCurrent(0);
        return current instanceof BaseAnimation base && base.isPlaying() ? base : null;
    }

    /**
     * The player it is fighting, or the nearest one in range. Two minotaurs on the same player
     * would overwrite each other's line — accepted: this is a
     * herramienta de banco de pruebas, no de partida.
     */
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

        // --- estado del FSM, solo cuando aporta ---
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

        // --- cooldown de melee: el hueco de castigo del jugador ---
        if (cortex != null) {
            line.append(" §8│ ").append(meleeCooldown(cortex) > 0
                    ? "§b⏳" + String.format("%.1f", meleeCooldown(cortex) / 20.0F) + "s"
                    : "§a✔");
        }

        return line.toString();
    }

    /** Misma información que la action bar, en ASCII y con columnas de ancho fijo. */
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

    /**
     * Degrees between the body yaw and the direction to the target, signed like {@code sweepAngle}
     * (positive = target to the mob's left). Every hitbox aims along this yaw, so past half the
     * sector's arc the attack misses no matter how good the range and cooldown look.
     */
    private float yawError(LivingEntity target) {
        final double dx = target.getX() - this.mino.getX();
        final double dz = target.getZ() - this.mino.getZ();
        final float toTarget = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        return -Mth.degreesDifference(AttackAnchor.bodyYaw(this.mino), toTarget);
    }

    /** Transition line: from, to, how long the outgoing state ran, and the distance right then. */
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

    /** Rango de ticks de la ventana de daño, leído de los frame events reales. */
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

    /**
     * One cell per tick (or per block of ticks when the clip is longer than {@link #MAX_BAR_CELLS}).
     *
     * @param colored true for the action bar (§ codes), false for the console
     */
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
            final int to = (cell + 1) * duration / cells; // exclusivo

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
