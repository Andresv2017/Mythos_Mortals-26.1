package net.darkblade.mythosmortals.content.minotaur;

import net.darkblade.deluxelib.entity.ai.cortex.behavior.BehaviorData;
import net.minecraft.world.phys.Vec3;

/** Behavior context keys and tuning constants for the minotaur. */
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

    /** Enables the attacks whose clips have no keyframes yet. */
    public static final boolean ENABLE_UNANIMATED_ATTACKS = true;

    // --- ranges ---
    //
    //   0 ─── 2.2 ────── 3.5 ─── 3.6 ─── 4.5 ─── 5.0 ─────── 12.0 ────►
    //   │  PUSH   │ PUSH (charge ready) │        │
    //   │         │ VERTICAL            │        │
    //   │         │ COMBO A→B ──────────┘        │
    //   │                        chase  │      CHARGE      │  chase

    public static final float MELEE_RANGE = 3.5F;

    /** Same number gates attacking and chasing, or a band opens where it does neither. */
    public static final float ATTACK_RANGE = MELEE_RANGE + 1.0F;

    // --- push ---

    public static final float PUSH_RANGE = MELEE_RANGE;

    /** Axe dead zone: the horizontal sector starts ahead of the mob and can't reach in here. */
    public static final float PUSH_CONTACT_RANGE = 2.2F;

    public static final int PUSH_TICKS = 12;

    /** Must clear CHARGE_MIN_RANGE or the push→charge chain never fires. Vanilla sword is ~0.4. */
    public static final float PUSH_KNOCKBACK = 2.6F;

    /** Push failed to open a gap (knockback-immune target, or a wall): park the charge. */
    public static final int CHARGE_RETRY_AFTER_FAILED_PUSH = 100;

    public static final float PUSH_TO_CHARGE_CHANCE = 0.5F;

    /** "Not now": without it pickAttack re-offers the charge next tick and the dice mean nothing. */
    public static final int CHARGE_SKIP_TICKS = 60;

    // --- vertical ---

    public static final float VERTICAL_RANGE = 3.6F;
    public static final float VERTICAL_CHANCE = 0.35F;
    public static final int VERTICAL_TICKS = 35;

    // --- charge ---

    public static final float CHARGE_MIN_RANGE = 5.0F;

    /** Aborts a charge mid-windup. Lower than CHARGE_MIN_RANGE so the band 3.0–5.0 is hysteresis. */
    public static final float CHARGE_ABORT_RANGE = 3.0F;

    /** Run budget is CHARGE_MAX_RUN_TICKS × CHARGE_VELOCITY = 20 blocks; beyond this it stops short. */
    public static final float CHARGE_MAX_RANGE = 12.0F;

    public static final int CHARGE_WINDUP_TICKS = 15;
    public static final int CHARGE_MAX_RUN_TICKS = 27;
    public static final int CHARGE_HIT_TICKS = 10;
    public static final int CHARGE_STUN_TICKS = 40;
    public static final float CHARGE_DAMAGE = 12.0F;

    // --- speeds ---

    public static final double WALK_SPEED = 1.0;
    public static final double RUN_SPEED = 1.35;

    /** Fraction of the remaining angle the body turns per tick while standing. Aims every hitbox. */
    public static final float BODY_TURN_STILL = 0.30F;

    /** Blocks per tick, NOT a MoveControl multiplier — ChargeRunBehavior writes velocity directly. */
    public static final double CHARGE_VELOCITY = 0.75;

    // --- cooldowns (ticks) ---

    public static final int MELEE_COOLDOWN = 30;
    public static final int CHARGE_COOLDOWN = 200;

    // --- horizontal combo ---

    public static final float COMBO_CLIP_SECONDS = 0.8333F;
    public static final int COMBO_A_TICKS = 14;
    public static final int COMBO_B_TICKS = 16;
    public static final float COMBO_CHAIN_CHANCE = 0.75F;

    /** Generous: A's own knockback pushes the target out before the chain is evaluated. */
    public static final float COMBO_CHAIN_RANGE = MELEE_RANGE + 2.0F;

    /** Rolled per branch in order, so it is not a split — see AnimatedMeleeBehavior#combo. */
    public static final float COMBO_FINISHER_CHANCE = 0.25F;

    // --- misc ---

    public static final int SPOTTED_ROAR_TICKS = 20;
}
