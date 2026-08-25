package net.darkblade.mythosmortals.content.pegasus.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.mythosmortals.content.pegasus.PegasusEntity;
import net.darkblade.mythosmortals.content.pegasus.network.PegasusDashServerPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class PegasusDashInput {

    public static final KeyMapping DASH_KEY = new KeyMapping(
            "key.mythosmortals.pegasus_dash",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R,
            KeyMapping.Category.GAMEPLAY);

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        while (DASH_KEY.consumeClick()) {
            if (mc.player.getVehicle() instanceof PegasusEntity pegasus
                    && pegasus.getControllingPassenger() == mc.player) {
                MythosMortals.NETWORK.sendToServer(new PegasusDashServerPacket(pegasus.getId()));
            }
        }
    }

    private PegasusDashInput() {}

    @EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(DASH_KEY);
        }

        private ModEvents() {}
    }
}
