package net.darkblade.mythosmortals.content.pegasus;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Lets the rider fit the bridle mid-buck without having to aim at the animal underneath them.
 *
 * <p>Right-clicking the pegasus itself already works through {@code mobInteract}, but a rider is
 * sitting on the very hitbox they would have to hit, while it throws itself around the sky. Handling
 * the plain "used an item" cases as well means the bridle goes on wherever the player happens to be
 * looking — which is the only fair way to run a fifteen-second window.
 */
@EventBusSubscriber(modid = MythosMortals.MODID)
public final class PegasusBridleEvents {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (fitBridle(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (fitBridle(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** @return whether this right-click was the bridle going onto a bucking pegasus */
    private static boolean fitBridle(Player player, ItemStack held) {
        if (!held.is(MythosMortalsItems.ATHENA_BRIDLE.get())
                || !(player.getVehicle() instanceof PegasusEntity pegasus)
                || pegasus.tameState() != PegasusTameState.BUCKING) {
            return false;
        }
        if (!player.level().isClientSide()) {
            pegasus.fitBridle(player, held);
        }
        return true;
    }

    private PegasusBridleEvents() {}
}
