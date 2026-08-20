package net.darkblade.mythosmortals.content.minotaur.behavior;

import net.darkblade.deluxelib.entity.ai.cortex.behavior.Behavior;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.BehaviorContext;
import net.darkblade.mythosmortals.content.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.content.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.content.minotaur.MinotaurState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Embestida B) Carrera: línea recta en la dirección bloqueada por el windup.
 * Tres salidas: atropella a alguien (CHARGE_HIT), choca contra pared (CHARGE_STUN),
 * o se agota el tiempo y vuelve a la guardia.
 */
public class ChargeRunBehavior implements Behavior<MinotaurEntity, MinotaurState> {

    @Override
    public @Nullable Integer tick(MinotaurEntity entity, BehaviorContext context) {
        final Vec3 direction = context.get(MinotaurCtx.CHARGE_DIRECTION);
        if (direction.lengthSqr() < 1.0E-4) {
            return MinotaurState.COMBAT_IDLE.id(); // sin dirección válida, aborta
        }

        // Avanza recto: waypoint unos bloques por delante en la dirección bloqueada
        entity.getMoveControl().setWantedPosition(
                entity.getX() + direction.x * 4.0,
                entity.getY(),
                entity.getZ() + direction.z * 4.0,
                MinotaurCtx.CHARGE_SPEED
        );

        // Choque contra pared → stun
        if (entity.horizontalCollision) {
            return MinotaurState.CHARGE_STUN.id();
        }

        // Atropello: cualquier entidad viva pegada a su hitbox
        final boolean rammedSomeone = !entity.level().getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().inflate(0.6, 0.2, 0.6),
                t -> t != entity && t.isAlive() && !(t instanceof MinotaurEntity)
        ).isEmpty();
        if (rammedSomeone) {
            return MinotaurState.CHARGE_HIT.id();
        }

        // Whiff: se pasó de largo
        if (context.ticksInState() >= MinotaurCtx.CHARGE_MAX_RUN_TICKS) {
            return MinotaurState.COMBAT_IDLE.id();
        }

        return null;
    }

    @Override
    public boolean canBeInterrupted(MinotaurEntity entity, BehaviorContext context, int interruptingStateId) {
        return false;
    }

    @Override
    public void onExit(MinotaurEntity entity, BehaviorContext context, boolean interrupted) {
        // El cooldown de la embestida corre termine como termine (hit, stun o whiff)
        context.set(MinotaurCtx.NEXT_CHARGE_TIME, entity.level().getGameTime() + MinotaurCtx.CHARGE_COOLDOWN);
    }
}
