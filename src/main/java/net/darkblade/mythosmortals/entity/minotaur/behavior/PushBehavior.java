package net.darkblade.mythosmortals.entity.minotaur.behavior;

import net.darkblade.deluxelib.entity.ai.cortex.behavior.Behavior;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.BehaviorContext;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;


public class PushBehavior implements Behavior<MinotaurEntity, MinotaurState> {

    @Override
    public void onEnter(MinotaurEntity entity, BehaviorContext context) {
        entity.getNavigation().stop();
        entity.animator().play(entity.animator().getByName("attack_push"));
    }

    @Override
    public @Nullable Integer tick(MinotaurEntity entity, BehaviorContext context) {
        final LivingEntity target = entity.getTarget();

        if (target != null && context.ticksInState() <= 3) {
            entity.getLookControl().setLookAt(target);
        }

        if (context.ticksInState() < MinotaurCtx.PUSH_TICKS) {
            return null;
        }

        final boolean chargeReady = MinotaurCtx.ENABLE_UNANIMATED_ATTACKS
                && target != null
                && target.isAlive()
                && entity.level().getGameTime() >= context.get(MinotaurCtx.NEXT_CHARGE_TIME);

        if (chargeReady) {
            if (entity.distanceTo(target) < MinotaurCtx.CHARGE_MIN_RANGE) {
                context.set(MinotaurCtx.NEXT_CHARGE_TIME,
                        entity.level().getGameTime() + MinotaurCtx.CHARGE_RETRY_AFTER_FAILED_PUSH);
                return MinotaurState.COMBAT_IDLE.id();
            }

            if (entity.getRandom().nextFloat() < MinotaurCtx.PUSH_TO_CHARGE_CHANCE) {
                return MinotaurState.CHARGE_WINDUP.id();
            }

            context.set(MinotaurCtx.NEXT_CHARGE_TIME,
                    entity.level().getGameTime() + MinotaurCtx.CHARGE_SKIP_TICKS);
        }

        return MinotaurState.CHASE.id();
    }

    @Override
    public boolean canBeInterrupted(MinotaurEntity entity, BehaviorContext context, int interruptingStateId) {
        return false;
    }

    @Override
    public void onExit(MinotaurEntity entity, BehaviorContext context, boolean interrupted) {
        context.set(MinotaurCtx.NEXT_MELEE_TIME, entity.level().getGameTime() + MinotaurCtx.MELEE_COOLDOWN);
    }
}
