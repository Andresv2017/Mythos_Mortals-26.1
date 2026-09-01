package net.darkblade.mythosmortals.entity.minotaur.routine;

import net.darkblade.deluxelib.entity.ai.cortex.Blackboard;
import net.darkblade.deluxelib.entity.ai.cortex.routine.impl.ScriptedRoutine;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;

public class ChargeRecoverRoutine extends ScriptedRoutine<MinotaurEntity, MinotaurState> {

    public ChargeRecoverRoutine() {
        super("charge_hit", MinotaurCtx.CHARGE_HIT_TICKS, MinotaurState.COMBAT_IDLE);
    }

    @Override
    public void exit(MinotaurEntity entity, Blackboard bb, boolean interrupted) {
        super.exit(entity, bb, interrupted);
        bb.put(MinotaurCtx.NEXT_MELEE_TIME,
                entity.level().getGameTime() + MinotaurCtx.CHARGE_WHIFF_RECOVERY);
    }
}
