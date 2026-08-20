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

/**
 * El lado ofensivo de {@link BorealCourageEffect}: +15% de daño en los golpes cuerpo a cuerpo hechos
 * con la Espada Xifos o la Lanza Dori mientras el atacante lleve el buff.
 *
 * <p>Va en {@code LivingIncomingDamageEvent} — pre-armadura, el mismo punto donde
 * {@code PoiseEvents} aplica el multiplicador de riposte. Ese es el sitio natural para un "+15% de
 * daño del arma": el bonus entra antes de que la armadura del objetivo lo reduzca, igual que el daño
 * base del arma. Ambos manejadores multiplican {@code getAmount()}, así que el orden entre ellos da
 * igual.
 *
 * <p>{@code getDirectEntity() == attacker} es lo que restringe esto al cuerpo a cuerpo: en una lanza
 * lanzada la entidad directa es el {@code ThrownDoriSpear}, no quien la tiró, así que el impacto del
 * proyectil no cobra el bonus (decisión tomada con la spec: sólo estocada/embestida en mano).
 */
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
