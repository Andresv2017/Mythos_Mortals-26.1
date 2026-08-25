package net.darkblade.mythosmortals.entity.minotaur.behavior;

import net.darkblade.deluxelib.entity.ai.cortex.behavior.Behavior;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.BehaviorContext;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class SpottedRoarBehavior implements Behavior<MinotaurEntity, MinotaurState> {

    @Override
    public void onEnter(MinotaurEntity entity, BehaviorContext context) {
        entity.getNavigation().stop();
        entity.animator().play(entity.animator().getByName("target_spotted"));
    }

    @Override
    public @Nullable Integer tick(MinotaurEntity entity, BehaviorContext context) {
        final LivingEntity target = entity.getTarget();

        if (target != null) {
            entity.getLookControl().setLookAt(target);
        }

        if (context.ticksInState() < MinotaurCtx.SPOTTED_ROAR_TICKS) {
            return null;
        }

        if (target == null) {
            return MinotaurState.COMBAT_IDLE.id();
        }

        final double distance = entity.distanceTo(target);
        final boolean chargeable = distance >= MinotaurCtx.CHARGE_ABORT_RANGE
                && distance <= MinotaurCtx.CHARGE_MAX_RANGE;

        return chargeable ? MinotaurState.CHARGE_WINDUP.id() : MinotaurState.CHASE.id();
    }

    @Override
    public boolean canBeInterrupted(MinotaurEntity entity, BehaviorContext context, int interruptingStateId) {
        return false;
    }

    @Override
    public void onExit(MinotaurEntity entity, BehaviorContext context, boolean interrupted) {
        if (interrupted) {
            entity.animator().stop(entity.animator().getByName("target_spotted"));
        }
    }
}
