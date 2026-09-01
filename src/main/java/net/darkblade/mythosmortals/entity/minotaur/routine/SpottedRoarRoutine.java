package net.darkblade.mythosmortals.entity.minotaur.routine;

import net.darkblade.deluxelib.entity.ai.cortex.routine.Routine;
import net.darkblade.deluxelib.entity.ai.cortex.Blackboard;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class SpottedRoarRoutine implements Routine<MinotaurEntity, MinotaurState> {

    @Override
    public void enter(MinotaurEntity entity, Blackboard bb) {
        entity.getNavigation().stop();
        entity.animator().play(entity.animator().getByName("target_spotted"));
    }

    @Override
    public @Nullable MinotaurState run(MinotaurEntity entity, Blackboard bb) {
        final LivingEntity target = entity.getTarget();

        if (target != null) {
            entity.getLookControl().setLookAt(target);
        }

        if (bb.stateAge() < MinotaurCtx.SPOTTED_ROAR_TICKS) {
            return null;
        }

        if (target == null) {
            return MinotaurState.COMBAT_IDLE;
        }

        final double distance = entity.distanceTo(target);
        final boolean chargeable = distance >= MinotaurCtx.CHARGE_ABORT_RANGE
                && distance <= MinotaurCtx.CHARGE_MAX_RANGE;

        return chargeable ? MinotaurState.CHARGE_WINDUP : MinotaurState.CHASE;
    }

    @Override
    public boolean allowsInterrupt(MinotaurEntity entity, Blackboard bb, MinotaurState incoming) {
        return false;
    }

    @Override
    public void exit(MinotaurEntity entity, Blackboard bb, boolean interrupted) {
        entity.animator().stop(entity.animator().getByName("target_spotted"));
    }
}
