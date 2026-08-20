package net.darkblade.mythosmortals.content.owl.perch;

import net.darkblade.deluxelib.entity.perch.PerchPlacement;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The owl's own perch tuning: where it sits on its owner's arm, how their arm is posed to hold it,
 * and how it is drawn on your first-person hand. Content, not mechanism — the library asks
 * {@code OwlEntity.perchPlacement()} for these and knows nothing else about them.
 *
 * <p>Dial them in live with {@code /deluxelib debug owlperch} + numpad (see {@code OwlPerchTuner}),
 * then paste the printed values back over the constants below.
 *
 * <p><b>Common-side, and deliberately free of any client import.</b> The server reads
 * {@link #PERCH_SIDE}/{@link #PERCH_HEIGHT}/{@link #PERCH_FORWARD} to place the owl's hitbox while
 * the client reads the same three to draw it, which is exactly why they live in one place now — they
 * used to be two hand-synced copies, one here and one in the renderer-facing client class, and
 * tuning one without the other silently desynced what you saw from what you could touch. The live
 * tuner is client-only, so it cannot be named from here: it registers itself through
 * {@link #setOverride} instead, and a dedicated server never loads it.
 */
public final class OwlPerchPlacement {

    /**
     * Right arm swung out to the side and up — the classic falconry "bird on the arm" pose, in
     * radians.
     *
     * <p>Sign matters and is counter-intuitive: for the right arm a NEGATIVE {@code zRot} folds it in
     * across the chest, so opening it outward needs a positive value. It also has to pass 90° (1.571)
     * to get ABOVE horizontal — 0 is hanging straight down, 90° is straight out to the side, so ~92°
     * is out and just above horizontal. {@code xRot} sits near 0 (a slight -3° tilt) to keep the arm
     * close to the frontal plane instead of swinging it forward.
     */
    static final float ARM_X_ROT = -0.05F;
    static final float ARM_Y_ROT = 0.00F;
    static final float ARM_Z_ROT = 1.60F;

    /** Where the owl sits relative to its host, in blocks: out to their right, up from their feet,
     * and ahead of their shoulder line. Tuned live against {@link #ARM_Z_ROT} rather than computed
     * from arm geometry — redial the same way if the arm pose above ever changes. */
    static final float PERCH_SIDE = 0.66F;
    static final float PERCH_HEIGHT = 2.78F;
    static final float PERCH_FORWARD = -0.02F;

    /**
     * The owl model's scale, everywhere: {@code OwlRenderer} uses it for the free-flying bird, and it
     * rides the placement because drawing the owl inside its host's render pass skips
     * {@code setupRotations}, so the renderer's own shrink never runs on that path.
     *
     * <p>It lives here rather than in {@code OwlRenderer} for exactly one reason: this class is
     * common-side and that one is client-only, so the placement handed to the library can carry the
     * value without a server ever touching a renderer class.
     */
    public static final float MODEL_SCALE = 0.90F;

    /**
     * Offset (in the same model-local space {@code ItemInHandRendererMixin} leaves the
     * {@code PoseStack} in, right after vanilla draws your own first-person hand mesh) from that hand
     * to where the owl sits on it. Wholly unrelated to {@link #ARM_Z_ROT}/{@link #PERCH_SIDE} above:
     * those pose the THIRD-person arm bone via {@code HumanoidModel#setupAnim}, which first person
     * never runs — vanilla's first-person hand is a completely separate hardcoded transform chain in
     * {@code ItemInHandRenderer}, so this needs its own live tuning pass with numbers that don't
     * relate to the third-person ones at all.
     */
    static final float FP_OWL_X = -1.50F;
    static final float FP_OWL_Y = 1.10F;
    static final float FP_OWL_Z = -0.70F;
    /** Owl body rotation on top of the position offset above, in degrees. */
    static final float FP_OWL_X_ROT = -78.0F;
    static final float FP_OWL_Y_ROT = -348.0F;
    static final float FP_OWL_Z_ROT = -123.0F;
    /** Multiplies {@link #MODEL_SCALE} — first person sits much closer to the camera than the
     * third-person arm view, so the same absolute scale can read as the wrong size. This constant
     * isn't independent of {@code MODEL_SCALE}, so re-tune it too any time that one changes. */
    static final float FP_OWL_SCALE = 1.0F;

    /** The compiled values, built once. Returned by {@link #current()} whenever no live tuner is
     * running, which is always outside a debug session and always on a server. */
    private static final PerchPlacement COMPILED = new PerchPlacement(
            PERCH_SIDE, PERCH_HEIGHT, PERCH_FORWARD,
            ARM_X_ROT, ARM_Y_ROT, ARM_Z_ROT,
            FP_OWL_X, FP_OWL_Y, FP_OWL_Z,
            FP_OWL_X_ROT, FP_OWL_Y_ROT, FP_OWL_Z_ROT,
            MODEL_SCALE, FP_OWL_SCALE);

    /** Set by the client-only tuner while it is active; {@code null} otherwise. {@code volatile}
     * because the render thread reads it while the tuner's key handler writes it. */
    private static volatile @Nullable Supplier<PerchPlacement> override;

    /**
     * Installs (or clears, with {@code null}) a live override of the compiled values.
     *
     * <p>The hook exists so the client-side tuner can feed its in-progress values into the render
     * path without this common class ever naming a client class — a dedicated server would fail to
     * load one. Nothing else should call this.
     */
    public static void setOverride(@Nullable Supplier<PerchPlacement> supplier) {
        override = supplier;
    }

    /** The placement to use right now: the tuner's live values during a debug session, the compiled
     * constants otherwise. */
    public static PerchPlacement current() {
        Supplier<PerchPlacement> supplier = override;
        return supplier != null ? supplier.get() : COMPILED;
    }

    /** The compiled values, ignoring any live override — what the tuner seeds itself from and what
     * {@code .} resets it back to. */
    public static PerchPlacement compiled() {
        return COMPILED;
    }

    private OwlPerchPlacement() {}
}
