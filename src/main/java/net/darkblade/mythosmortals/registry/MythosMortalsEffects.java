package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.mythosmortals.effect.BorealCourageEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosMortalsEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, MythosMortals.MODID);

    public static final DeferredHolder<MobEffect, BorealCourageEffect> BOREAL_COURAGE =
        MOB_EFFECTS.register("boreal_courage", BorealCourageEffect::new);

    private MythosMortalsEffects() {
    }
}
