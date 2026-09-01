package net.darkblade.mythosmortals.entity.minotaur.routine;

import net.darkblade.deluxelib.entity.ai.cortex.routine.Routine;
import net.darkblade.deluxelib.entity.ai.cortex.Blackboard;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;


public class ChargeWindupRoutine implements Routine<MinotaurEntity, MinotaurState> {

    @Override
    public void enter(MinotaurEntity entity, Blackboard bb) {
        entity.getNavigation().stop();
        entity.animator().play(entity.animator().getByName("charge_start"));
    }

    @Override
    public @Nullable MinotaurState run(MinotaurEntity entity, Blackboard bb) {
        final LivingEntity target = entity.getTarget();

        if (target == null) {
            return MinotaurState.COMBAT_IDLE;
        }

        entity.getLookControl().setLookAt(target);

        if (entity.distanceTo(target) < MinotaurCtx.CHARGE_ABORT_RANGE) {
            bb.put(MinotaurCtx.NEXT_CHARGE_TIME,
                    entity.level().getGameTime() + MinotaurCtx.CHARGE_SKIP_TICKS);
            return MinotaurState.COMBAT_IDLE;
        }

        if (bb.stateAge() < MinotaurCtx.CHARGE_WINDUP_TICKS) {
            return null;
        }

        final Vec3 toTarget = target.position().subtract(entity.position());
        final Vec3 flat = new Vec3(toTarget.x, 0.0, toTarget.z);
        if (flat.lengthSqr() < 1.0E-4) {
            return MinotaurState.COMBAT_IDLE;
        }

        bb.put(MinotaurCtx.CHARGE_DIRECTION, flat.normalize());
        return MinotaurState.CHARGE_RUN;
    }

    @Override
    public boolean allowsInterrupt(MinotaurEntity entity, Blackboard bb, MinotaurState incoming) {
        return false;
    }

    @Override
    public void exit(MinotaurEntity entity, Blackboard bb, boolean interrupted) {
        entity.animator().stop(entity.animator().getByName("charge_start"));
    }
}
