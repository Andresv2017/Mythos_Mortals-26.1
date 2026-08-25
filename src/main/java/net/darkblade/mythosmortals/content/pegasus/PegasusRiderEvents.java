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

@EventBusSubscriber(modid = MythosMortals.MODID)
public final class PegasusRiderEvents {

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
