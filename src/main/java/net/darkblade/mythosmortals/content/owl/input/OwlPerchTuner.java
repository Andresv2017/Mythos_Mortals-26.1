package net.darkblade.mythosmortals.content.owl.input;

import net.darkblade.deluxelib.client.PerchClient;
import net.darkblade.deluxelib.client.render.NumpadAxisTuner;
import net.darkblade.deluxelib.entity.perch.PerchPlacement;
import net.darkblade.mythosmortals.content.owl.perch.OwlPerchPlacement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Live, in-game tuner for the perched-owl look — perch the owl on your arm, run
 * {@code /deluxelib debug owlperch}, and nudge with the NUMPAD until it sits right, then paste the
 * printed constants into {@link OwlPerchPlacement}. Debug tool: client-side only, local player only,
 * not persisted. Built on {@link NumpadAxisTuner} for the toggle/dispatch/status/print skeleton
 * shared with the other numpad tuners.
 *
 * <p>Unlike {@code RiderPoseTuner} (which layers an extra offset on top of a transform) this holds
 * the <em>actual values</em> the render path uses, lazily seeded from
 * {@link OwlPerchPlacement#compiled()} the first time each is read while active. So what you see is
 * exactly what the constants would produce, and {@code 0} prints lines you can paste over them
 * verbatim.
 *
 * <p><b>How the values reach the renderer.</b> While active, {@link #toggle()} installs
 * {@link #placement()} as {@code OwlPerchPlacement}'s live override, so {@code OwlEntity
 * .perchPlacement()} — and through it the library's render path — sees these values instead of the
 * compiled ones. The library never knows a tuner exists.
 *
 * <p><b>Tuning moves the visual, not the hitbox.</b> This override is client-side, and the server
 * places the owl's real position from its own read of the compiled values. So during a session the
 * drawn bird and its actual position drift apart until you paste the numbers back. Expected for a
 * debug tool.
 *
 * <p>Numpad controls while active (and perched):
 * <ul>
 *   <li>{@code 5} — cycle ARM mode (the host's arm angle) → OWL mode (where the bird sits in third
 *       person) → FP_POS mode (where it sits on your OWN first-person hand) → FP_ROT mode (its
 *       rotation + scale there) → back to ARM</li>
 *   <li>ARM: {@code 8}/{@code 2} pitch (xRot), {@code 4}/{@code 6} out/in (zRot),
 *       {@code 7}/{@code 1} twist (yRot)</li>
 *   <li>OWL: {@code 8}/{@code 2} forward/back, {@code 4}/{@code 6} right/left,
 *       {@code 9}/{@code 3} up/down, {@code 7}/{@code 1} third-person scale</li>
 *   <li>FP_POS: same layout as OWL, but feeds the first-person hand offset instead — an unrelated
 *       coordinate space (vanilla's first-person hand transform, not third-person entity space)</li>
 *   <li>FP_ROT: {@code 8}/{@code 2} xRot, {@code 4}/{@code 6} zRot, {@code 7}/{@code 1} yRot (same
 *       layout as ARM mode), {@code 9}/{@code 3} scale up/down</li>
 *   <li>{@code 0} — print the current values as ready-to-paste constants</li>
 *   <li>{@code .} — reset back to the values compiled into {@link OwlPerchPlacement}</li>
 * </ul>
 */
public final class OwlPerchTuner extends NumpadAxisTuner {
    private static final OwlPerchTuner INSTANCE = new OwlPerchTuner();

    /** Radians per nudge for the arm bone angles. ~2.9°, fine enough to land a bird on a hand. */
    private static final float ROT_STEP = 0.05F;
    /** Blocks per nudge for the owl's offset. Deliberately finer than the rider tuner's 0.05. */
    private static final float POS_STEP = 0.02F;
    /** Degrees per nudge for the first-person owl's rotation. */
    private static final float FP_ROT_STEP = 3.0F;
    /** Multiplier per nudge for the first-person owl's scale. */
    private static final float SCALE_STEP = 0.05F;

    private enum Mode { ARM, OWL, FP_POS, FP_ROT }

    private Mode mode = Mode.ARM;

    // Null = "not overridden yet, seed from the compiled default on first read".
    private @Nullable Float armXRot;
    private @Nullable Float armYRot;
    private @Nullable Float armZRot;
    private @Nullable Float side;
    private @Nullable Float height;
    private @Nullable Float forward;
    private @Nullable Float owlScale;
    private @Nullable Float fpX;
    private @Nullable Float fpY;
    private @Nullable Float fpZ;
    private @Nullable Float fpXRot;
    private @Nullable Float fpYRot;
    private @Nullable Float fpZRot;
    private @Nullable Float fpScale;

    private OwlPerchTuner() {}

    /** Toggles the tuner, installing or clearing its live override on {@link OwlPerchPlacement} so
     * the render path picks the values up (or stops seeing them). */
    public static boolean toggle() {
        INSTANCE.active = !INSTANCE.active;
        OwlPerchPlacement.setOverride(INSTANCE.active ? OwlPerchTuner::placement : null);
        return INSTANCE.active;
    }

    public static boolean isActive() {
        return INSTANCE.active;
    }

    /** Drops every override, so the next read re-seeds from {@link OwlPerchPlacement}'s constants. */
    public static void reset() {
        INSTANCE.resetValues();
    }

    public static void onKey(int key, int action) {
        INSTANCE.handleInput(key, action);
    }

    /** The values as the library consumes them — installed as {@code OwlPerchPlacement}'s override
     * while this tuner is active. Builds a fresh record per call because every value can change
     * between frames; outside a session the compiled singleton is returned instead. */
    public static PerchPlacement placement() {
        return new PerchPlacement(
                side(), height(), forward(),
                armXRot(), armYRot(), armZRot(),
                fpX(), fpY(), fpZ(),
                fpXRot(), fpYRot(), fpZRot(),
                owlScale(), fpScale());
    }

    @Override
    protected boolean canTune(Minecraft mc) {
        return PerchClient.perchedEntityIdFor(mc.player.getId()) != -1;
    }

    /** Drops every override, so the next read re-seeds from {@link OwlPerchPlacement}'s constants. */
    @Override
    protected void resetValues() {
        this.armXRot = null;
        this.armYRot = null;
        this.armZRot = null;
        this.side = null;
        this.height = null;
        this.forward = null;
        this.owlScale = null;
        this.fpX = null;
        this.fpY = null;
        this.fpZ = null;
        this.fpXRot = null;
        this.fpYRot = null;
        this.fpZRot = null;
        this.fpScale = null;
    }

    // -----------------------------------------------------------------------
    // Value reads — each returns the compiled constant unless the tuner is active, in which case the
    // (lazily seeded from that same constant) override wins. Seeding from the real constant rather
    // than from a passed-in default matters: a keypress that lands before the first read would
    // otherwise snap the value to whatever placeholder the caller used.
    // -----------------------------------------------------------------------
    public static float armXRot() {
        if (!INSTANCE.active) return OwlPerchPlacement.compiled().armXRot();
        if (INSTANCE.armXRot == null) INSTANCE.armXRot = OwlPerchPlacement.compiled().armXRot();
        return INSTANCE.armXRot;
    }

    public static float armYRot() {
        if (!INSTANCE.active) return OwlPerchPlacement.compiled().armYRot();
        if (INSTANCE.armYRot == null) INSTANCE.armYRot = OwlPerchPlacement.compiled().armYRot();
        return INSTANCE.armYRot;
    }

    public static float armZRot() {
        if (!INSTANCE.active) return OwlPerchPlacement.compiled().armZRot();
        if (INSTANCE.armZRot == null) INSTANCE.armZRot = OwlPerchPlacement.compiled().armZRot();
        return INSTANCE.armZRot;
    }

    public static float side() {
        if (!INSTANCE.active) return OwlPerchPlacement.compiled().side();
        if (INSTANCE.side == null) INSTANCE.side = OwlPerchPlacement.compiled().side();
        return INSTANCE.side;
    }

    public static float height() {
        if (!INSTANCE.active) return OwlPerchPlacement.compiled().height();
        if (INSTANCE.height == null) INSTANCE.height = OwlPerchPlacement.compiled().height();
        return INSTANCE.height;
    }

    public static float forward() {
        if (!INSTANCE.active) return OwlPerchPlacement.compiled().forward();
        if (INSTANCE.forward == null) INSTANCE.forward = OwlPerchPlacement.compiled().forward();
        return INSTANCE.forward;
    }

    /** The owl model's scale. Previewed here on the perched bird only, but the value drives the
     * free-flying one too (via {@code OwlRenderer.setupRotations}, which reads the same constant), so
     * pasting the printed number back keeps both consistent. */
    public static float owlScale() {
        if (!INSTANCE.active) return OwlPerchPlacement.compiled().modelScale();
        if (INSTANCE.owlScale == null) INSTANCE.owlScale = OwlPerchPlacement.compiled().modelScale();
        return INSTANCE.owlScale;
    }

    public static float fpX() {
        if (!INSTANCE.active) return OwlPerchPlacement.compiled().fpX();
        if (INSTANCE.fpX == null) INSTANCE.fpX = OwlPerchPlacement.compiled().fpX();
        return INSTANCE.fpX;
    }

    public static float fpY() {
        if (!INSTANCE.active) return OwlPerchPlacement.compiled().fpY();
        if (INSTANCE.fpY == null) INSTANCE.fpY = OwlPerchPlacement.compiled().fpY();
        return INSTANCE.fpY;
    }

    public static float fpZ() {
        if (!INSTANCE.active) return OwlPerchPlacement.compiled().fpZ();
        if (INSTANCE.fpZ == null) INSTANCE.fpZ = OwlPerchPlacement.compiled().fpZ();
        return INSTANCE.fpZ;
    }

    public static float fpXRot() {
        if (!INSTANCE.active) return OwlPerchPlacement.compiled().fpXRot();
        if (INSTANCE.fpXRot == null) INSTANCE.fpXRot = OwlPerchPlacement.compiled().fpXRot();
        return INSTANCE.fpXRot;
    }

    public static float fpYRot() {
        if (!INSTANCE.active) return OwlPerchPlacement.compiled().fpYRot();
        if (INSTANCE.fpYRot == null) INSTANCE.fpYRot = OwlPerchPlacement.compiled().fpYRot();
        return INSTANCE.fpYRot;
    }

    public static float fpZRot() {
        if (!INSTANCE.active) return OwlPerchPlacement.compiled().fpZRot();
        if (INSTANCE.fpZRot == null) INSTANCE.fpZRot = OwlPerchPlacement.compiled().fpZRot();
        return INSTANCE.fpZRot;
    }

    public static float fpScale() {
        if (!INSTANCE.active) return OwlPerchPlacement.compiled().fpScale();
        if (INSTANCE.fpScale == null) INSTANCE.fpScale = OwlPerchPlacement.compiled().fpScale();
        return INSTANCE.fpScale;
    }

    // -----------------------------------------------------------------------
    // Input — key bindings, steps and signs are unchanged from before the perch refactor on purpose:
    // this is a tool with muscle memory attached, and nothing here needed to move.
    // -----------------------------------------------------------------------
    @Override
    protected boolean handleKey(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_KP_5 -> {
                this.mode = switch (this.mode) {
                    case ARM -> Mode.OWL;
                    case OWL -> Mode.FP_POS;
                    case FP_POS -> Mode.FP_ROT;
                    case FP_ROT -> Mode.ARM;
                };
                yield true;
            }
            default -> switch (this.mode) {
                case ARM -> nudgeArm(key);
                case OWL -> nudgeOwl(key);
                case FP_POS -> nudgeFpPos(key);
                case FP_ROT -> nudgeFpRot(key);
            };
        };
    }

    /** Arm bone angles, in radians. zRot is the "out from the body" one — positive opens it outward,
     * and past ~1.571 (90°) it rises above horizontal. */
    private boolean nudgeArm(int key) {
        return nudge(key,
            new KeyAxis(GLFW.GLFW_KEY_KP_8, GLFW.GLFW_KEY_KP_2, ROT_STEP, d -> this.armXRot = armXRot() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_4, GLFW.GLFW_KEY_KP_6, ROT_STEP, d -> this.armZRot = armZRot() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_7, GLFW.GLFW_KEY_KP_1, ROT_STEP, d -> this.armYRot = armYRot() + d));
    }

    /** The owl's offset from the host, in blocks — same axes the constants use (side = out to the
     * host's right, height = up from their feet, forward = ahead of the shoulder line) — plus its
     * third-person scale on the otherwise-unused 7/1, the same "spare axis" trick FP_ROT uses for
     * its own scale on 9/3. */
    private boolean nudgeOwl(int key) {
        return nudge(key,
            new KeyAxis(GLFW.GLFW_KEY_KP_2, GLFW.GLFW_KEY_KP_8, POS_STEP, d -> this.forward = forward() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_4, GLFW.GLFW_KEY_KP_6, POS_STEP, d -> this.side = side() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_3, GLFW.GLFW_KEY_KP_9, POS_STEP, d -> this.height = height() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_1, GLFW.GLFW_KEY_KP_7, SCALE_STEP,
                d -> this.owlScale = Math.max(0.05F, owlScale() + d)));
    }

    /** The owl's offset from your own first-person hand mesh — same layout as OWL mode, but an
     * unrelated coordinate space (vanilla's hardcoded first-person hand transform, not third-person
     * entity space), so these numbers have no relationship to {@link #side}/{@link #height}/
     * {@link #forward} at all. */
    private boolean nudgeFpPos(int key) {
        return nudge(key,
            new KeyAxis(GLFW.GLFW_KEY_KP_8, GLFW.GLFW_KEY_KP_2, POS_STEP, d -> this.fpZ = fpZ() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_4, GLFW.GLFW_KEY_KP_6, POS_STEP, d -> this.fpX = fpX() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_3, GLFW.GLFW_KEY_KP_9, POS_STEP, d -> this.fpY = fpY() + d));
    }

    /** The first-person owl's own rotation (degrees, same layout as ARM mode) plus its scale
     * multiplier on 9/3, since first person has no natural third axis left over to give scale its
     * own mode. */
    private boolean nudgeFpRot(int key) {
        return nudge(key,
            new KeyAxis(GLFW.GLFW_KEY_KP_8, GLFW.GLFW_KEY_KP_2, FP_ROT_STEP, d -> this.fpXRot = fpXRot() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_4, GLFW.GLFW_KEY_KP_6, FP_ROT_STEP, d -> this.fpZRot = fpZRot() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_7, GLFW.GLFW_KEY_KP_1, FP_ROT_STEP, d -> this.fpYRot = fpYRot() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_3, GLFW.GLFW_KEY_KP_9, SCALE_STEP,
                d -> this.fpScale = Math.max(0.05F, fpScale() + d)));
    }

    @Override
    protected Component status() {
        return switch (this.mode) {
            case OWL -> Component.literal(String.format("[OWL] side %.2f | height %.2f | forward %.2f | scale %.2f",
                            side(), height(), forward(), owlScale()))
                    .withStyle(ChatFormatting.AQUA);
            case FP_POS -> Component.literal(String.format("[FP_POS] x %.2f | y %.2f | z %.2f",
                            fpX(), fpY(), fpZ()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
            case FP_ROT -> Component.literal(String.format("[FP_ROT] xRot %.0f° | yRot %.0f° | zRot %.0f° | scale %.2f",
                            fpXRot(), fpYRot(), fpZRot(), fpScale()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
            // Degrees alongside radians: the constants are radians, but nobody thinks in radians.
            case ARM -> Component.literal(String.format("[ARM] xRot %.2f (%.0f°) | zRot %.2f (%.0f°) | yRot %.2f (%.0f°)",
                            armXRot(), Math.toDegrees(armXRot()),
                            armZRot(), Math.toDegrees(armZRot()),
                            armYRot(), Math.toDegrees(armYRot())))
                    .withStyle(ChatFormatting.YELLOW);
        };
    }

    @Override
    protected void printValues(@NotNull LocalPlayer player) {
        // Every line pastes into ONE file now. Before the perch refactor the PERCH_* trio also had to
        // be mirrored by hand into OwlEntity, which placed the hitbox from its own copy.
        player.sendSystemMessage(Component.literal("[owlperch] paste over the constants in OwlPerchPlacement:")
                .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(String.format(
                        "ARM_X_ROT = %.3fF;  ARM_Y_ROT = %.3fF;  ARM_Z_ROT = %.3fF;",
                        armXRot(), armYRot(), armZRot()))
                .withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal(String.format(
                        "PERCH_SIDE = %.3fF;  PERCH_HEIGHT = %.3fF;  PERCH_FORWARD = %.3fF;",
                        side(), height(), forward()))
                .withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal(String.format(
                        "MODEL_SCALE = %.3fF;",
                        owlScale()))
                .withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal(String.format(
                        "FP_OWL_X = %.3fF;  FP_OWL_Y = %.3fF;  FP_OWL_Z = %.3fF;",
                        fpX(), fpY(), fpZ()))
                .withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal(String.format(
                        "FP_OWL_X_ROT = %.1fF;  FP_OWL_Y_ROT = %.1fF;  FP_OWL_Z_ROT = %.1fF;  FP_OWL_SCALE = %.3fF;",
                        fpXRot(), fpYRot(), fpZRot(), fpScale()))
                .withStyle(ChatFormatting.GREEN));
    }

    public static Component helpMessage(boolean enabled) {
        if (!enabled) {
            return Component.literal("[owlperch] tuner OFF").withStyle(ChatFormatting.GRAY);
        }
        return Component.literal("[owlperch] tuner ON — perch the owl, then NUMPAD: "
                        + "5 cycles ARM/OWL/FP_POS/FP_ROT mode. ARM: 8/2 pitch, 4/6 in/out, 7/1 twist. "
                        + "OWL: 8/2 fwd/back, 4/6 left/right, 9/3 up/down, 7/1 scale. "
                        + "FP_POS: 8/2 fwd/back, 4/6 left/right, 9/3 up/down (first-person hand). "
                        + "FP_ROT: 8/2 xRot, 4/6 zRot, 7/1 yRot, 9/3 scale. "
                        + "0 prints, . resets")
                .withStyle(ChatFormatting.GOLD);
    }
}
