package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosMortalsParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, MythosMortals.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> OWL_BOOM =
        PARTICLE_TYPES.register("owl_boom", () -> new SimpleParticleType(false));

    private MythosMortalsParticles() {
    }
}
