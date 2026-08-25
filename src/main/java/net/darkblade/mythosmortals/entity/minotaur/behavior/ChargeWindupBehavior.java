package net.darkblade.mythosmortals.entity.minotaur.behavior;

import net.darkblade.deluxelib.entity.ai.cortex.behavior.Behavior;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.BehaviorContext;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;


public class ChargeWindupBehavior implements Behavior<MinotaurEntity, MinotaurState> {

    @Override
    public void onEnter(MinotaurEntity entity, BehaviorContext context) {
        entity.getNavigation().stop();
        entity.animator().play(entity.animator().getByName("charge_start"));
    }

    @Override
    public @Nullable Integer tick(MinotaurEntity entity, BehaviorContext context) {
        final LivingEntity target = entity.getTarget();

        if (target == null) {
            return MinotaurState.COMBAT_IDLE.id();
        }

        entity.getLookControl().setLookAt(target);

        if (entity.distanceTo(target) < MinotaurCtx.CHARGE_ABORT_RANGE) {
            context.set(MinotaurCtx.NEXT_CHARGE_TIME,
                    entity.level().getGameTime() + MinotaurCtx.CHARGE_SKIP_TICKS);
            return MinotaurState.COMBAT_IDLE.id();
        }

        if (context.ticksInState() < MinotaurCtx.CHARGE_WINDUP_TICKS) {
            return null;
        }

        final Vec3 toTarget = target.position().subtract(entity.position());
        final Vec3 flat = new Vec3(toTarget.x, 0.0, toTarget.z);
        if (flat.lengthSqr() < 1.0E-4) {
            return MinotaurState.COMBAT_IDLE.id();
        }

        context.set(MinotaurCtx.CHARGE_DIRECTION, flat.normalize());
        return MinotaurState.CHARGE_RUN.id();
    }

    @Override
    public boolean canBeInterrupted(MinotaurEntity entity, BehaviorContext context, int interruptingStateId) {
        return false;
    }
}
