package net.darkblade.mythosmortals.entity.minotaur.routine;

import net.darkblade.deluxelib.entity.ai.cortex.routine.Routine;
import net.darkblade.deluxelib.entity.ai.cortex.Blackboard;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;


public class PushRoutine implements Routine<MinotaurEntity, MinotaurState> {

    @Override
    public void enter(MinotaurEntity entity, Blackboard bb) {
        entity.getNavigation().stop();
        entity.animator().play(entity.animator().getByName("attack_push"));
    }

    @Override
    public @Nullable MinotaurState run(MinotaurEntity entity, Blackboard bb) {
        final LivingEntity target = entity.getTarget();

        if (target != null && bb.stateAge() <= 3) {
            entity.getLookControl().setLookAt(target);
        }

        if (bb.stateAge() < MinotaurCtx.PUSH_TICKS) {
            return null;
        }

        final boolean chargeReady = MinotaurCtx.ENABLE_UNANIMATED_ATTACKS
                && target != null
                && target.isAlive()
                && entity.level().getGameTime() >= bb.get(MinotaurCtx.NEXT_CHARGE_TIME);

        if (chargeReady) {
            if (entity.distanceTo(target) < MinotaurCtx.CHARGE_MIN_RANGE) {
                bb.put(MinotaurCtx.NEXT_CHARGE_TIME,
                        entity.level().getGameTime() + MinotaurCtx.CHARGE_RETRY_AFTER_FAILED_PUSH);
                return MinotaurState.COMBAT_IDLE;
            }

            if (entity.getRandom().nextFloat() < MinotaurCtx.PUSH_TO_CHARGE_CHANCE) {
                return MinotaurState.CHARGE_WINDUP;
            }

            bb.put(MinotaurCtx.NEXT_CHARGE_TIME,
                    entity.level().getGameTime() + MinotaurCtx.CHARGE_SKIP_TICKS);
        }

        return MinotaurState.CHASE;
    }

    @Override
    public boolean allowsInterrupt(MinotaurEntity entity, Blackboard bb, MinotaurState incoming) {
        return false;
    }

    @Override
    public void exit(MinotaurEntity entity, Blackboard bb, boolean interrupted) {
        bb.put(MinotaurCtx.NEXT_MELEE_TIME, entity.level().getGameTime() + MinotaurCtx.MELEE_COOLDOWN);
    }
}
