package net.darkblade.mythosmortals.block.amphora;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.mythosmortals.effect.BorealCourageEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import org.jetbrains.annotations.NotNull;


@EventBusSubscriber(modid = MythosMortals.MODID)
public final class WineEvents {

    private static final int NAUSEA_TICKS = 10 * 20;

    private static final int HEAVY_NAUSEA_TICKS = 20 * 20;

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.@NotNull Finish event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getItem().is(MythosMortalsItems.WINE_BOTTLE.get())) {
            drink(player);
            return;
        }
        if (event.getItem().is(MythosMortalsRegistry.GREEK_AMPHORA_WINE_ITEM.get())) {
            drink(player);
            returnAmphora(event, player);
        }
    }

    private static void returnAmphora(LivingEntityUseItemEvent.Finish event, Player player) {
        ItemStack used = AmphoraServings.spend(event.getItem());
        ItemStack leftover = event.getResultStack();
        if (leftover.isEmpty()) {
            event.setResultStack(used);
        } else if (!player.getInventory().add(used)) {
            player.drop(used, false);
        }
    }

    public static void drink(@NotNull Player player) {
        boolean stillBuffed = player.hasEffect(MythosMortalsRegistry.BOREAL_COURAGE);
        boolean alreadyDizzy = player.hasEffect(MobEffects.NAUSEA);

        BorealCourageEffect.apply(player);

        if (!stillBuffed) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA,
            alreadyDizzy ? HEAVY_NAUSEA_TICKS : NAUSEA_TICKS,
            alreadyDizzy ? 1 : 0));
    }

    private WineEvents() {}
}
