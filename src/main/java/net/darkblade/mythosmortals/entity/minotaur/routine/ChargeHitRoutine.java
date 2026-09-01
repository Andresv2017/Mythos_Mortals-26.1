package net.darkblade.mythosmortals.entity.minotaur.routine;

import net.darkblade.deluxelib.entity.ai.cortex.routine.Routine;
import net.darkblade.deluxelib.entity.ai.cortex.Blackboard;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;
import net.darkblade.mythosmortals.registry.MythosMortalsDamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;


public class ChargeHitRoutine implements Routine<MinotaurEntity, MinotaurState> {

    @Override
    public void enter(MinotaurEntity entity, Blackboard bb) {
        entity.animator().play(entity.animator().getByName("charge_hit"));

        final Vec3 direction = bb.get(MinotaurCtx.CHARGE_DIRECTION);

        for (final LivingEntity victim : entity.level().getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().inflate(1.2, 0.5, 1.2).move(direction.scale(0.8)),
                t -> t != entity && t.isAlive() && !(t instanceof MinotaurEntity))) {

            victim.hurt(MythosMortalsDamageTypes.minotaurGore(entity), MinotaurCtx.CHARGE_DAMAGE);
            victim.setDeltaMovement(direction.x * 1.1, 0.85, direction.z * 1.1);
            victim.hurtMarked = true;
        }
    }

    @Override
    public @Nullable MinotaurState run(MinotaurEntity entity, Blackboard bb) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.8, 1.0, 0.8));

        if (bb.stateAge() >= MinotaurCtx.CHARGE_HIT_TICKS) {
            return MinotaurState.COMBAT_IDLE;
        }

        return null;
    }

    @Override
    public boolean allowsInterrupt(MinotaurEntity entity, Blackboard bb, MinotaurState incoming) {
        return false;
    }

    @Override
    public void exit(MinotaurEntity entity, Blackboard bb, boolean interrupted) {
        bb.put(MinotaurCtx.NEXT_MELEE_TIME,
                entity.level().getGameTime() + MinotaurCtx.CHARGE_HIT_RECOVERY);
    }
}
