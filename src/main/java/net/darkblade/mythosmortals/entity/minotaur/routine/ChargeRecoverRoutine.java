package net.darkblade.mythosmortals.entity.minotaur.behavior;

import net.darkblade.deluxelib.entity.ai.cortex.behavior.BehaviorContext;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.impl.TimedAnimationBehavior;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;

public class ChargeRecoverBehavior extends TimedAnimationBehavior<MinotaurEntity, MinotaurState> {

    public ChargeRecoverBehavior() {
        super("charge_hit", MinotaurCtx.CHARGE_HIT_TICKS, MinotaurState.COMBAT_IDLE);
    }

    @Override
    public void onExit(MinotaurEntity entity, BehaviorContext context, boolean interrupted) {
        super.onExit(entity, context, interrupted);
        context.set(MinotaurCtx.NEXT_MELEE_TIME,
                entity.level().getGameTime() + MinotaurCtx.CHARGE_WHIFF_RECOVERY);
    }
}
