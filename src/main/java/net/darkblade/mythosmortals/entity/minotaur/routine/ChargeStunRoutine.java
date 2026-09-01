package net.darkblade.mythosmortals.entity.minotaur.behavior;

import net.darkblade.deluxelib.entity.ai.cortex.behavior.BehaviorContext;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.impl.TimedAnimationBehavior;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.entity.minotaur.MinotaurState;
import net.minecraft.world.phys.Vec3;


public class ChargeStunBehavior extends TimedAnimationBehavior<MinotaurEntity, MinotaurState> {

    public ChargeStunBehavior() {
        super("charge_stun", MinotaurCtx.CHARGE_STUN_TICKS, MinotaurState.COMBAT_IDLE);
    }

    @Override
    public void onEnter(MinotaurEntity entity, BehaviorContext context) {
        super.onEnter(entity, context);

        final Vec3 direction = context.get(MinotaurCtx.CHARGE_DIRECTION);
        entity.setDeltaMovement(direction.scale(-0.5).add(0.0, 0.3, 0.0));
    }

    @Override
    public void onExit(MinotaurEntity entity, BehaviorContext context, boolean interrupted) {
        super.onExit(entity, context, interrupted);
        context.set(MinotaurCtx.NEXT_MELEE_TIME,
                entity.level().getGameTime() + MinotaurCtx.CHARGE_STUN_RECOVERY);
    }

}
