package net.darkblade.mythosmortals.entity.minotaur.behavior;

import net.darkblade.deluxelib.entity.ai.cortex.behavior.Behavior;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.BehaviorContext;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;


public class ChargeHitBehavior implements Behavior<MinotaurEntity, MinotaurState> {

    @Override
    public void onEnter(MinotaurEntity entity, BehaviorContext context) {
        entity.animator().play(entity.animator().getByName("charge_hit"));

        final Vec3 direction = context.get(MinotaurCtx.CHARGE_DIRECTION);

        for (final LivingEntity victim : entity.level().getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().inflate(1.2, 0.5, 1.2).move(direction.scale(0.8)),
                t -> t != entity && t.isAlive() && !(t instanceof MinotaurEntity))) {

            victim.hurt(entity.damageSources().mobAttack(entity), MinotaurCtx.CHARGE_DAMAGE);
            victim.setDeltaMovement(direction.x * 1.1, 0.85, direction.z * 1.1);
            victim.hurtMarked = true;
        }
    }

    @Override
    public @Nullable Integer tick(MinotaurEntity entity, BehaviorContext context) {
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
