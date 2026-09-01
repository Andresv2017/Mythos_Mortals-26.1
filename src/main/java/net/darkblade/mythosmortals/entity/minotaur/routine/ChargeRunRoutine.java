package net.darkblade.mythosmortals.entity.minotaur.behavior;

import net.darkblade.deluxelib.entity.ai.cortex.behavior.Behavior;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.BehaviorContext;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;


public class ChargeRunBehavior implements Behavior<MinotaurEntity, MinotaurState> {

    @Override
    public @Nullable Integer tick(MinotaurEntity entity, BehaviorContext context) {
        final Vec3 direction = context.get(MinotaurCtx.CHARGE_DIRECTION);
        if (direction.lengthSqr() < 1.0E-4) {
            return MinotaurState.COMBAT_IDLE.id();
        }

        final Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(
                direction.x * MinotaurCtx.CHARGE_VELOCITY,
                motion.y,
                direction.z * MinotaurCtx.CHARGE_VELOCITY
        );

        final float yaw = (float) (Mth.atan2(direction.z, direction.x) * (180.0 / Math.PI)) - 90.0F;
        entity.setYRot(yaw);
        entity.yBodyRot = yaw;
        entity.yHeadRot = yaw;

        if (entity.horizontalCollision) {
            return MinotaurState.CHARGE_STUN.id();
        }

        final boolean rammedSomeone = !entity.level().getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().inflate(0.6, 0.2, 0.6),
                t -> t != entity && t.isAlive() && !(t instanceof MinotaurEntity)
        ).isEmpty();
        if (rammedSomeone) {
            return MinotaurState.CHARGE_HIT.id();
        }

        if (context.ticksInState() >= MinotaurCtx.CHARGE_MAX_RUN_TICKS) {
            return MinotaurState.CHARGE_RECOVER.id();
        }

        return null;
    }

    @Override
    public boolean canBeInterrupted(MinotaurEntity entity, BehaviorContext context, int interruptingStateId) {
        return false;
    }

    @Override
    public void onExit(MinotaurEntity entity, BehaviorContext context, boolean interrupted) {
        final Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(0.0, motion.y, 0.0);

        context.set(MinotaurCtx.NEXT_CHARGE_TIME, entity.level().getGameTime() + MinotaurCtx.CHARGE_COOLDOWN);
    }
}
