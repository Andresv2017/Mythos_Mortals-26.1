package net.darkblade.mythosmortals.entity.owl;

import net.darkblade.deluxelib.entity.perch.PerchPlacement;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class OwlPerchPlacement {

    static final float ARM_X_ROT = -0.05F;
    static final float ARM_Y_ROT = 0.00F;
    static final float ARM_Z_ROT = 1.60F;

    static final float PERCH_SIDE = 0.66F;
    static final float PERCH_HEIGHT = 2.78F;
    static final float PERCH_FORWARD = -0.02F;

    public static final float MODEL_SCALE = 0.90F;

    static final float FP_OWL_X = -1.50F;
    static final float FP_OWL_Y = 1.10F;
    static final float FP_OWL_Z = -0.70F;
    static final float FP_OWL_X_ROT = -78.0F;
    static final float FP_OWL_Y_ROT = -348.0F;
    static final float FP_OWL_Z_ROT = -123.0F;
    static final float FP_OWL_SCALE = 1.0F;

    private static final PerchPlacement COMPILED = new PerchPlacement(
            PERCH_SIDE, PERCH_HEIGHT, PERCH_FORWARD,
            ARM_X_ROT, ARM_Y_ROT, ARM_Z_ROT,
            FP_OWL_X, FP_OWL_Y, FP_OWL_Z,
            FP_OWL_X_ROT, FP_OWL_Y_ROT, FP_OWL_Z_ROT,
            MODEL_SCALE, FP_OWL_SCALE);

    private static volatile @Nullable Supplier<PerchPlacement> override;

    public static void setOverride(@Nullable Supplier<PerchPlacement> supplier) {
        override = supplier;
    }

    public static PerchPlacement current() {
        Supplier<PerchPlacement> supplier = override;
        return supplier != null ? supplier.get() : COMPILED;
    }

    public static PerchPlacement compiled() {
        return COMPILED;
    }

    private OwlPerchPlacement() {}
}
