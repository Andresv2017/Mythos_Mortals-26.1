package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;


public final class MythosMortalsDamageTypes {

    public static final ResourceKey<DamageType> MINOTAUR_GORE = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(MythosMortals.MODID, "minotaur_gore"));

    public static @NotNull DamageSource minotaurGore(@NotNull LivingEntity attacker) {
        return attacker.damageSources().source(MINOTAUR_GORE, attacker);
    }

    private MythosMortalsDamageTypes() {
    }
}
