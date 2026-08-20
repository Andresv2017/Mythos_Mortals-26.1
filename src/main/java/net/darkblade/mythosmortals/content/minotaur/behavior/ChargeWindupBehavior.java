package net.darkblade.mythosmortals.content.minotaur.behavior;

import net.darkblade.deluxelib.entity.ai.cortex.behavior.Behavior;
import net.darkblade.deluxelib.entity.ai.cortex.behavior.BehaviorContext;
import net.darkblade.mythosmortals.content.minotaur.MinotaurCtx;
import net.darkblade.mythosmortals.content.minotaur.MinotaurEntity;
import net.darkblade.mythosmortals.content.minotaur.MinotaurState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Embestida A) Wind-up: rasca el suelo, baja el torso y apunta los cuernos.
 * Al final fija la dirección de la carrera (recta, sin homing) en el contexto.
 */
public class ChargeWindupBehavior implements Behavior<MinotaurEntity, MinotaurState> {

    @Override
    public void onEnter(MinotaurEntity entity, BehaviorContext context) {
        entity.getNavigation().stop();
        entity.animator().play(entity.animator().getByName("charge_start"));
        // TODO: rugido sordo + partículas de tierra en la pezuña
    }

    @Override
    public @Nullable Integer tick(MinotaurEntity entity, BehaviorContext context) {
        final LivingEntity target = entity.getTarget();

        // Apunta al objetivo durante toda la anticipación (la carrera luego va recta)
        if (target != null) {
            entity.getLookControl().setLookAt(target);
        }

        if (context.ticksInState() < MinotaurCtx.CHARGE_WINDUP_TICKS) {
            return null;
        }

        if (target == null) {
            return MinotaurState.COMBAT_IDLE.id();
        }

        // Bloquea la dirección horizontal hacia donde está el objetivo AHORA
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
