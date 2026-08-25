package net.darkblade.mythosmortals.entity.owl.client.input;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.deluxelib.client.PossessionClient;
import net.darkblade.mythosmortals.entity.owl.client.OwlAim;
import net.darkblade.mythosmortals.entity.owl.network.OwlOrderAttackServerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class OwlOrderInput {

    private static final double ORDER_REACH = 96.0;

    @SubscribeEvent
    public static void onSpyglassOrder(InputEvent.MouseButton.@NotNull Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        if (PossessionClient.possessed() != null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            return;
        }
        if (!player.isUsingItem() || !player.getUseItem().is(Items.SPYGLASS)) {
            return;
        }
        Entity target = OwlAim.findAimedLiving(mc, player, ORDER_REACH);
        if (target == null) {
            return;
        }
        event.setCanceled(true);
        MythosMortals.NETWORK.sendToServer(new OwlOrderAttackServerPacket(target.getId()));
    }

    private OwlOrderInput() {}
}
