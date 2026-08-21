package net.darkblade.mythosmortals.content.effect;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import net.darkblade.mythosmortals.MythosMortals;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = MythosMortals.MODID)
public final class BorealCourageEvents {

    @SubscribeEvent
    public static void onIncomingDamage(@NotNull LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (event.getSource().getDirectEntity() != attacker) {
            return;
        }
        if (!attacker.hasEffect(MythosMortalsRegistry.BOREAL_COURAGE)) {
            return;
        }
        if (!isBorealWeapon(attacker.getMainHandItem())) {
            return;
        }
        event.setAmount(event.getAmount() * (1.0F + BorealCourageEffect.MELEE_DAMAGE_BONUS));
    }

    private static boolean isBorealWeapon(@NotNull ItemStack stack) {
        return stack.is(MythosMortalsItems.XIFOS_SWORD.get()) || stack.is(MythosMortalsItems.DORI_SPEAR.get());
    }

    private BorealCourageEvents() {}
}
