package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


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

    // --- Pegasus ---
    public static final DeferredHolder<SoundEvent, SoundEvent> PEGASUS_AMBIENT   = event("entity.pegasus.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> PEGASUS_WING_FLAP = event("entity.pegasus.wing_flap");
    public static final DeferredHolder<SoundEvent, SoundEvent> PEGASUS_TAKE_OFF  = event("entity.pegasus.take_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> PEGASUS_LANDING   = event("entity.pegasus.landing");
    public static final DeferredHolder<SoundEvent, SoundEvent> PEGASUS_DASH      = event("entity.pegasus.dash");

    // --- Minotaur ---
    // The physical half of the charge stays on vanilla, which happens to have exact matches:
    // goat.prepare_ram, goat.ram_impact, ravager.stunned and ravager.step. What is registered here
    // is the voice and the weapon — the parts no vanilla mob can stand in for. See MinotaurEntity.
    public static final DeferredHolder<SoundEvent, SoundEvent> MINOTAUR_AMBIENT     = event("entity.minotaur.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINOTAUR_ROAR        = event("entity.minotaur.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINOTAUR_DEATH       = event("entity.minotaur.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINOTAUR_HURT        = event("entity.minotaur.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINOTAUR_SWING       = event("entity.minotaur.swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINOTAUR_SLAM        = event("entity.minotaur.slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINOTAUR_PUSH        = event("entity.minotaur.push");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINOTAUR_CHARGE_LOOP = event("entity.minotaur.charge_loop");

    // --- Soldiers (Athenian + Spartan) ---
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_ATTACK      = event("entity.soldier.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_BLOCK       = event("entity.soldier.block");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_DEATH1      = event("entity.soldier.death1");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_DEATH2      = event("entity.soldier.death2");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_POISE_BREAK = event("entity.soldier.poise_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_AMBIENT     = event("entity.soldier.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_HURT        = event("entity.soldier.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_SHIELD_UP   = event("entity.soldier.shield_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLDIER_STEP        = event("entity.soldier.step");

    private static DeferredHolder<SoundEvent, SoundEvent> event(String name) {
        return SOUND_EVENTS.register(name, () ->
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MythosMortals.MODID, name)));
    }

    private MythosMortalsSounds() {
    }
}
