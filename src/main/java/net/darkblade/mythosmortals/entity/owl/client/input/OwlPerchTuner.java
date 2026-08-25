package net.darkblade.mythosmortals.entity.owl.client.input;

import net.darkblade.deluxelib.client.PerchClient;
import net.darkblade.deluxelib.client.render.NumpadAxisTuner;
import net.darkblade.deluxelib.entity.perch.PerchPlacement;
import net.darkblade.mythosmortals.entity.owl.OwlPerchPlacement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;


public final class OwlPerchTuner extends NumpadAxisTuner {
    private static final OwlPerchTuner INSTANCE = new OwlPerchTuner();

    private static final float ROT_STEP = 0.05F;
    private static final float POS_STEP = 0.02F;
    private static final float FP_ROT_STEP = 3.0F;
    private static final float SCALE_STEP = 0.05F;

    private enum Mode { ARM, OWL, FP_POS, FP_ROT }

    private Mode mode = Mode.ARM;

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


    public static boolean toggle() {
        INSTANCE.active = !INSTANCE.active;
        OwlPerchPlacement.setOverride(INSTANCE.active ? OwlPerchTuner::placement : null);
        return INSTANCE.active;
    }

    public static boolean isActive() {
        return INSTANCE.active;
    }

    public static void reset() {
        INSTANCE.resetValues();
    }

    public static void onKey(int key, int action) {
        INSTANCE.handleInput(key, action);
    }

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

    private boolean nudgeArm(int key) {
        return nudge(key,
            new KeyAxis(GLFW.GLFW_KEY_KP_8, GLFW.GLFW_KEY_KP_2, ROT_STEP, d -> this.armXRot = armXRot() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_4, GLFW.GLFW_KEY_KP_6, ROT_STEP, d -> this.armZRot = armZRot() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_7, GLFW.GLFW_KEY_KP_1, ROT_STEP, d -> this.armYRot = armYRot() + d));
    }

    private boolean nudgeOwl(int key) {
        return nudge(key,
            new KeyAxis(GLFW.GLFW_KEY_KP_2, GLFW.GLFW_KEY_KP_8, POS_STEP, d -> this.forward = forward() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_4, GLFW.GLFW_KEY_KP_6, POS_STEP, d -> this.side = side() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_3, GLFW.GLFW_KEY_KP_9, POS_STEP, d -> this.height = height() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_1, GLFW.GLFW_KEY_KP_7, SCALE_STEP,
                d -> this.owlScale = Math.max(0.05F, owlScale() + d)));
    }

    private boolean nudgeFpPos(int key) {
        return nudge(key,
            new KeyAxis(GLFW.GLFW_KEY_KP_8, GLFW.GLFW_KEY_KP_2, POS_STEP, d -> this.fpZ = fpZ() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_4, GLFW.GLFW_KEY_KP_6, POS_STEP, d -> this.fpX = fpX() + d),
            new KeyAxis(GLFW.GLFW_KEY_KP_3, GLFW.GLFW_KEY_KP_9, POS_STEP, d -> this.fpY = fpY() + d));
    }

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
            case ARM -> Component.literal(String.format("[ARM] xRot %.2f (%.0f°) | zRot %.2f (%.0f°) | yRot %.2f (%.0f°)",
                            armXRot(), Math.toDegrees(armXRot()),
                            armZRot(), Math.toDegrees(armZRot()),
                            armYRot(), Math.toDegrees(armYRot())))
                    .withStyle(ChatFormatting.YELLOW);
        };
    }

    @Override
    protected void printValues(@NotNull LocalPlayer player) {
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
