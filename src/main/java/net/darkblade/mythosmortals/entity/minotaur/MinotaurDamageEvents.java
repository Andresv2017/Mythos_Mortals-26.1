package net.darkblade.mythosmortals.entity.minotaur;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.mythosmortals.registry.MythosMortalsDamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = MythosMortals.MODID)
public final class MinotaurDamageEvents {

    @SubscribeEvent
    public static void onIncomingDamage(@NotNull LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!event.getSource().is(MythosMortalsDamageTypes.MINOTAUR_GORE)) {
            return;
        }

        final float kept = 1.0F - MinotaurCtx.GORE_ARMOR_PIERCE;
        event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * kept);
        event.addReductionModifier(DamageContainer.Reduction.ENCHANTMENTS, (container, reduction) -> reduction * kept);
    }

    private MinotaurDamageEvents() {
    }
}
