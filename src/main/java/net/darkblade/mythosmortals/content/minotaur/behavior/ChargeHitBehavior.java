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
 * Embestida C) Impacto exitoso: cabezazo ascendente que lanza por los aires a
 * quien atropelló, mientras el minotauro frena su inercia.
 */
public class ChargeHitBehavior implements Behavior<MinotaurEntity, MinotaurState> {

    @Override
    public void onEnter(MinotaurEntity entity, BehaviorContext context) {
        entity.animator().play(entity.animator().getByName("charge_hit"));

        final Vec3 direction = context.get(MinotaurCtx.CHARGE_DIRECTION);

        // Daña y lanza a todos los que quedaron delante de los cuernos
        for (final LivingEntity victim : entity.level().getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().inflate(1.2, 0.5, 1.2).move(direction.scale(0.8)),
                t -> t != entity && t.isAlive() && !(t instanceof MinotaurEntity))) {

            victim.hurt(entity.damageSources().mobAttack(entity), MinotaurCtx.CHARGE_DAMAGE);
            victim.setDeltaMovement(direction.x * 1.1, 0.85, direction.z * 1.1);
            victim.hurtMarked = true; // fuerza el sync de velocity al cliente (lanzamiento visible)
        }
    }

    @Override
    public @Nullable Integer tick(MinotaurEntity entity, BehaviorContext context) {
        // Frenado paulatino de la inercia de la carrera
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.8, 1.0, 0.8));

        if (context.ticksInState() >= MinotaurCtx.CHARGE_HIT_TICKS) {
            return MinotaurState.COMBAT_IDLE.id();
        }

        return null;
    }

    @Override
    public boolean canBeInterrupted(MinotaurEntity entity, BehaviorContext context, int interruptingStateId) {
        return false;
    }
}
