package net.darkblade.mythosmortals.content.pegasus;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The two things a rider needs that the entity cannot do on its own: keep them in the saddle for the
 * length of the ritual, and let them fit the bridle without aiming at the animal beneath them.
 */
@EventBusSubscriber(modid = MythosMortals.MODID)
public final class PegasusRiderEvents {

    /**
     * Keeps the rider in the saddle in the two places where sneaking has to mean something else.
     *
     * <p>Shift is the vanilla dismount key, and this mob needs it twice over: it is how you approach
     * a pegasus without spooking it, so the ritual's auto-mount used to throw the player straight
     * back off, and it is how a rider descends in flight, which would otherwise mean stepping off at
     * altitude. So: during the ritual the only ways out are the bridle or the fall, and in flight
     * you dismount by landing first.
     */
    @SubscribeEvent
    public static void onDismount(EntityMountEvent event) {
        if (!event.isDismounting()
                || !(event.getEntityBeingMounted() instanceof PegasusEntity pegasus)
                || !(event.getEntityMounting() instanceof Player rider)) {
            return;
        }
        // Server only, and this is not a detail. The client calls ejectPassengers() on every
        // passenger sync (ClientPacketListener#handleSetEntityPassengersPacket) and re-seats whoever
        // the packet lists — so refusing a dismount here would cancel the client's half of a
        // dismount the server has already decided, leaving the rider stuck on screen aboard a
        // pegasus that has, as far as the server is concerned, already thrown them.
        if (pegasus.level().isClientSide()) {
            return;
        }
        // Only a voluntary dismount is refused. A dead or departing player must always come off,
        // or logging out mid-ritual would leave the pegasus holding a passenger that no longer exists.
        boolean alive = pegasus.isAlive() && !pegasus.isRemoved() && rider.isAlive() && !rider.isRemoved();
        boolean held = pegasus.tameState().isTaming() || (pegasus.isTamed() && pegasus.isFlying());
        if (alive && held) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (fitTack(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (fitTack(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * Tack a rider fits from the saddle rather than from the ground.
     *
     * <p>Right-clicking the pegasus itself works through {@code mobInteract}, but a rider is sitting
     * on the very hitbox they would have to hit — and during the buck it is throwing itself around
     * the sky. So both of the moments that are on a clock are handled here instead, and the item
     * goes on wherever the player happens to be looking:
     *
     * <ul>
     *   <li>the <b>bridle</b>, during the fifteen seconds of bucking;
     *   <li>the <b>saddle</b>, any time the rider is aboard an unsaddled tame one — which is what
     *       lets them cut short the descent after the taming and fly it themselves.
     * </ul>
     *
     * @return whether this right-click was tack going onto the pegasus the player is riding
     */
    private static boolean fitTack(Player player, ItemStack held) {
        if (!(player.getVehicle() instanceof PegasusEntity pegasus)) {
            return false;
        }

        if (held.is(MythosMortalsItems.ATHENA_BRIDLE.get())
                && pegasus.tameState() == PegasusTameState.BUCKING) {
            if (!player.level().isClientSide()) {
                pegasus.fitBridle(player, held);
            }
            return true;
        }

        if (pegasus.isTamed()
                && !pegasus.isSaddled()
                && pegasus.isEquippableInSlot(held, EquipmentSlot.SADDLE)) {
            if (!player.level().isClientSide()) {
                pegasus.fitSaddle(player, held);
            }
            return true;
        }
        return false;
    }

    private PegasusRiderEvents() {}
}
