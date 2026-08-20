package net.darkblade.mythosmortals.content.minotaur;

import net.darkblade.deluxelib.entity.ai.cortex.behavior.BehaviorData;
import net.minecraft.world.phys.Vec3;

/**
 * Claves del {@link net.darkblade.deluxelib.entity.ai.cortex.behavior.BehaviorContext}
 * y constantes de tuning del minotauro, en un solo lugar.
 */
public final class MinotaurCtx {

    private MinotaurCtx() {
    }

    // --- claves de contexto (persistentes entre estados) ---

    /** Game time absoluto a partir del cual puede volver a lanzar un melee. */
    public static final BehaviorData<Long> NEXT_MELEE_TIME = BehaviorData.longKey("next_melee_time");
    /** Game time absoluto a partir del cual puede volver a embestir. */
    public static final BehaviorData<Long> NEXT_CHARGE_TIME = BehaviorData.longKey("next_charge_time");
    /** Dirección horizontal bloqueada de la embestida (la fija el windup, la lee la carrera). */
    public static final BehaviorData<Vec3> CHARGE_DIRECTION = BehaviorData.vec3Key("charge_direction");

    // --- rangos ---

    public static final float MELEE_RANGE = 3.5F;
    public static final float PUSH_RANGE = 2.0F;
    public static final float CHARGE_MIN_RANGE = 8.0F;

    // --- velocidades (multiplicador sobre MOVEMENT_SPEED / speed del MoveControl) ---

    public static final double WALK_SPEED = 1.0;
    public static final double RUN_SPEED = 1.35;
    public static final double CHARGE_SPEED = 2.4;

    // --- cooldowns (ticks) ---

    public static final int MELEE_COOLDOWN = 30;
    public static final int CHARGE_COOLDOWN = 300;

    // --- embestida ---

    public static final int CHARGE_WINDUP_TICKS = 15;
    public static final int CHARGE_MAX_RUN_TICKS = 60;
    public static final int CHARGE_HIT_TICKS = 10;
    public static final int CHARGE_STUN_TICKS = 40;
    public static final float CHARGE_DAMAGE = 12.0F;

    // --- otros ---

    public static final int SPOTTED_ROAR_TICKS = 20;
}
