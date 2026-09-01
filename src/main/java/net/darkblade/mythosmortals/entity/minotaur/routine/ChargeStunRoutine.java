package net.darkblade.mythosmortals.entity.minotaur.routine;

import net.darkblade.deluxelib.entity.ai.cortex.Blackboard;
import net.darkblade.deluxelib.entity.ai.cortex.routine.impl.ScriptedRoutine;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;
import net.minecraft.world.phys.Vec3;


public class ChargeStunRoutine extends ScriptedRoutine<MinotaurEntity, MinotaurState> {

    public ChargeStunRoutine() {
        super("charge_stun", MinotaurCtx.CHARGE_STUN_TICKS, MinotaurState.COMBAT_IDLE);
    }

    @Override
    public void enter(MinotaurEntity entity, Blackboard bb) {
        super.enter(entity, bb);

        final Vec3 direction = bb.get(MinotaurCtx.CHARGE_DIRECTION);
        entity.setDeltaMovement(direction.scale(-0.5).add(0.0, 0.3, 0.0));
    }

    @Override
    public void exit(MinotaurEntity entity, Blackboard bb, boolean interrupted) {
        super.exit(entity, bb, interrupted);
        bb.put(MinotaurCtx.NEXT_MELEE_TIME,
                entity.level().getGameTime() + MinotaurCtx.CHARGE_STUN_RECOVERY);
    }

}
