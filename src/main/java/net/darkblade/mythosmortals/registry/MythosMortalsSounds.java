package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Entity sound events. Every one of these is fired from an animation frame through DeluxeLib's
 * {@code BaseAnimation.sound(...)} / {@code AnimSound} — see the entities' registerAnimations().
 * The library ticks sounds server-side only and broadcasts via Level#playSound, so there is no
 * client/server guard to write at the call sites.
 *
 * <p>The soldier family is shared by the Athenian and the Spartan: same samples, same events.
 */
public final class MythosMortalsSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, MythosMortals.MODID);

    // --- Arpy ---
    public static final DeferredHolder<SoundEvent, SoundEvent> ARPY_ATTACK  = event("entity.arpy.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARPY_DEATH   = event("entity.arpy.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARPY_FLY     = event("entity.arpy.fly");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARPY_LANDING = event("entity.arpy.landing");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARPY_AMBIENT = event("entity.arpy.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARPY_HURT    = event("entity.arpy.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARPY_STEP    = event("entity.arpy.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARPY_DIVE_RETURN = event("entity.arpy.dive_return");

    // --- Soldiers (Athenian + Spartan) ---
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_ATTACK      = event("entity.soldier.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_BLOCK       = event("entity.soldier.block");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_DEATH1      = event("entity.soldier.death1");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_DEATH2      = event("entity.soldier.death2");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_POISE_BREAK = event("entity.soldier.poise_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_AMBIENT     = event("entity.soldier.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_HURT        = event("entity.soldier.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_SHIELD_UP   = event("entity.soldier.shield_up");
    /** Three footstep variants behind one event — sounds.json picks one at random per step. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_STEP        = event("entity.soldier.step");

    private static DeferredHolder<SoundEvent, SoundEvent> event(String name) {
        return SOUND_EVENTS.register(name, () ->
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MythosMortals.MODID, name)));
    }

    private MythosMortalsSounds() {
    }
}
