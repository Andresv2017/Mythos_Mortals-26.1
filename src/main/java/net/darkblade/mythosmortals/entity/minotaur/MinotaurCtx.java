package net.darkblade.mythosmortals.entity.minotaur;

import net.darkblade.deluxelib.entity.ai.cortex.behavior.BehaviorData;
import net.minecraft.world.phys.Vec3;

public final class MinotaurCtx {

    private MinotaurCtx() {
    }

    // --- context keys ---

    public static final BehaviorData<Long> NEXT_MELEE_TIME = BehaviorData.longKey("next_melee_time");

    public static final BehaviorData<Long> NEXT_CHARGE_TIME = BehaviorData.longKey("next_charge_time");

    public static final BehaviorData<Vec3> CHARGE_DIRECTION = BehaviorData.vec3Key("charge_direction");

    // --- toggles ---

    public static final boolean ENABLE_RIDING = false;

    public static final boolean DEBUG_ANIM_ACTION_BAR = true;

    public static final boolean DEBUG_ANIM_CONSOLE = true;

    public static final boolean ENABLE_UNANIMATED_ATTACKS = true;

    // --- ranges ---
    //
    //   0 ─── 2.2 ────── 3.5 ─── 3.6 ─── 4.5 ─── 5.0 ─────── 12.0 ────►
    //   │  PUSH   │ PUSH (charge ready) │        │
    //   │         │ VERTICAL            │        │
    //   │         │ COMBO A→B ──────────┘        │
    //   │                        chase  │      CHARGE      │  chase

    public static final float MELEE_RANGE = 3.5F;

    public static final float ATTACK_RANGE = MELEE_RANGE + 1.0F;

    // --- boss bar ---

    /**
     * Radius within which players are shown the boss bar. Well inside the entity's
     * clientTrackingRange(10) = 160 blocks, so anyone who sees the bar has the mob loaded.
     */
    public static final double BOSS_BAR_RADIUS = 48.0;

    public static final double BOSS_BAR_RADIUS_SQR = BOSS_BAR_RADIUS * BOSS_BAR_RADIUS;

    // --- push ---

    public static final float PUSH_RANGE = MELEE_RANGE;

    public static final float PUSH_CONTACT_RANGE = 2.2F;

    /** Matches FRONT_PUSH's clip: 0.9583s * 20 = 19 ticks. Was 12 while the clip was a placeholder. */
    public static final int PUSH_TICKS = 19;

    public static final float PUSH_KNOCKBACK = 2.6F;

    public static final int CHARGE_RETRY_AFTER_FAILED_PUSH = 100;

    public static final float PUSH_TO_CHARGE_CHANCE = 0.5F;

    public static final int CHARGE_SKIP_TICKS = 60;

    // --- vertical ---

    public static final float VERTICAL_RANGE = 3.6F;
    public static final float VERTICAL_CHANCE = 0.35F;
    /** Matches COMBO_C's clip: 1.864s * 20 = 37 ticks. Was 35 while the clip was a placeholder. */
    public static final int VERTICAL_TICKS = 37;

    // --- charge ---

    public static final float CHARGE_MIN_RANGE = 5.0F;

    public static final float CHARGE_ABORT_RANGE = 3.0F;

    public static final float CHARGE_MAX_RANGE = 12.0F;

    public static final int CHARGE_WINDUP_TICKS = 15;
    public static final int CHARGE_MAX_RUN_TICKS = 27;
    public static final int CHARGE_HIT_TICKS = 10;
    public static final int CHARGE_STUN_TICKS = 40;
    public static final float CHARGE_DAMAGE = 12.0F;

    // --- speeds ---

    public static final double WALK_SPEED = 1.0;
    public static final double RUN_SPEED = 1.35;

    public static final float BODY_TURN_STILL = 0.30F;

    public static final double CHARGE_VELOCITY = 0.75;

    // --- cooldowns (ticks) ---

    public static final int MELEE_COOLDOWN = 30;
    public static final int CHARGE_COOLDOWN = 200;

    // --- horizontal combo ---

    public static final float COMBO_CLIP_SECONDS = 0.8333F;
    public static final int COMBO_A_TICKS = 14;
    public static final int COMBO_B_TICKS = 16;
    public static final float COMBO_CHAIN_CHANCE = 0.75F;

    public static final float COMBO_CHAIN_RANGE = MELEE_RANGE + 2.0F;

    public static final float COMBO_FINISHER_CHANCE = 0.25F;

    // --- misc ---

    public static final int SPOTTED_ROAR_TICKS = 20;
}
