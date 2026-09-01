package net.darkblade.mythosmortals.entity.minotaur.routine;

import net.darkblade.deluxelib.entity.ai.cortex.routine.Routine;
import net.darkblade.deluxelib.entity.ai.cortex.Blackboard;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;


public class ChargeRunRoutine implements Routine<MinotaurEntity, MinotaurState> {

    @Override
    public @Nullable MinotaurState run(MinotaurEntity entity, Blackboard bb) {
        final Vec3 direction = bb.get(MinotaurCtx.CHARGE_DIRECTION);
        if (direction.lengthSqr() < 1.0E-4) {
            return MinotaurState.COMBAT_IDLE;
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
            return MinotaurState.CHARGE_STUN;
        }

        final boolean rammedSomeone = !entity.level().getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().inflate(0.6, 0.2, 0.6),
                t -> t != entity && t.isAlive() && !(t instanceof MinotaurEntity)
        ).isEmpty();
        if (rammedSomeone) {
            return MinotaurState.CHARGE_HIT;
        }

        if (bb.stateAge() >= MinotaurCtx.CHARGE_MAX_RUN_TICKS) {
            return MinotaurState.CHARGE_RECOVER;
        }

        return null;
    }

    @Override
    public boolean allowsInterrupt(MinotaurEntity entity, Blackboard bb, MinotaurState incoming) {
        return false;
    }

    @Override
    public void exit(MinotaurEntity entity, Blackboard bb, boolean interrupted) {
        final Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(0.0, motion.y, 0.0);

        bb.put(MinotaurCtx.NEXT_CHARGE_TIME, entity.level().getGameTime() + MinotaurCtx.CHARGE_COOLDOWN);
    }
}
