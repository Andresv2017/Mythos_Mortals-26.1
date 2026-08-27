package net.darkblade.mythosmortals.entity;

import net.darkblade.deluxelib.anim.AnimSound;
import net.darkblade.deluxelib.anim.BaseAnimation;
import net.darkblade.mythosmortals.registry.MythosMortalsSounds;

/**
 * Footstep wiring shared by the Athenian and the Spartan. The two mobs keep their own
 * registerAnimations() because their combat timings diverge, but they run the same rig and the
 * same footstep samples, so the contact frames and this helper are common ground.
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

    private SoldierSounds() {
    }
}
