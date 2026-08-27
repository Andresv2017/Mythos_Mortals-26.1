package net.darkblade.mythosmortals.entity;

import net.darkblade.deluxelib.anim.AnimSound;
import net.darkblade.deluxelib.anim.BaseAnimation;
import net.darkblade.mythosmortals.registry.MythosMortalsSounds;
import net.minecraft.world.entity.LivingEntity;

/**
 * Sound wiring shared by the Athenian and the Spartan. The two mobs keep their own
 * registerAnimations() because their combat timings diverge, but they run the same rig and the
 * same samples, so the contact frames and these helpers are common ground.
 */
public final class SoldierSounds {

    /**
     * Attaches one footstep to {@code anim} at each of the given tick frames.
     *
     * <p>Frames are in ticks: DeluxeLib derives {@code durationTicks = (int)(withLength * 20)} and
     * {@link AnimSound#at(float, net.minecraft.sounds.SoundEvent)} indexes that same scale. On a
     * REPEATING animation each step fires once per cycle.
     *
     * <p>The pitch jitter matters more than it looks: a patrol of several soldiers walking in
     * step would otherwise phase-cancel into one flat clap instead of a crowd.
     */
    public static void steps(BaseAnimation anim, float volume, int... frames) {
        for (int frame : frames) {
            anim.sound(AnimSound.at(frame, MythosMortalsSounds.SOLDIER_STEP.get())
                .volume(volume)
                .pitchJitter(0.1F));
        }
    }

    /**
     * The shield taking a hit. This is deliberately NOT wired to the guard animation: the guard
     * cycle restarts every time it wins the animator back — after each swing, and after a stagger
     * chains into it — so a sound on its first frame trails the hit it was supposed to mark.
     * The block is an event, not a pose, so it is fired from the entity's hurtServer instead.
     *
     * <p>Volume and pitch spread match what GuardingMeleeEntity used for SoundEvents.SHIELD_BLOCK,
     * so the impact keeps the same weight it had with the vanilla clang.
     */
    public static void blocked(LivingEntity soldier) {
        soldier.level().playSound(null, soldier.blockPosition(),
            MythosMortalsSounds.SOLDIER_BLOCK.get(), soldier.getSoundSource(),
            1.0F, 0.9F + soldier.getRandom().nextFloat() * 0.2F);
    }

    /**
     * The shield coming up into guard. Like {@link #blocked}, this is an event rather than a pose,
     * and it is edge-triggered by the caller: GuardedMeleeAttackGoal#isRaisingGuard() stays true
     * for the first 6 ticks of the guard phase, so polling it would fire six times per raise.
     */
    public static void shieldUp(LivingEntity soldier) {
        soldier.level().playSound(null, soldier.blockPosition(),
            MythosMortalsSounds.SOLDIER_SHIELD_UP.get(), soldier.getSoundSource(),
            0.8F, 0.95F + soldier.getRandom().nextFloat() * 0.1F);
    }

    private SoldierSounds() {
    }
}
