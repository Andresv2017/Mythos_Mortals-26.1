package net.darkblade.mythosmortals.content.owl.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.deluxelib.client.PossessionClient;
import net.darkblade.deluxelib.client.PossessionInputHandler;
import net.darkblade.mythosmortals.content.owl.OwlAim;
import net.darkblade.mythosmortals.content.owl.OwlEntity;
import net.darkblade.mythosmortals.content.owl.network.OwlAttackServerPacket;
import net.darkblade.mythosmortals.content.owl.network.OwlMarkServerPacket;
import net.darkblade.mythosmortals.content.owl.network.OwlSonicAttackServerPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;


@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class OwlPossessionInput {


    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.mythosmortals.athena_sight",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H,
            KeyMapping.Category.GAMEPLAY);

    private static final double MARK_REACH = 40.0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        while (TOGGLE_KEY.consumeClick()) {
            if (PossessionClient.possessed() == null) {
                PossessionClient.requestActivate();
            } else {
                PossessionClient.requestCancel();
            }
        }
    }

    private static final class OwlActions implements PossessionInputHandler {

        @Override
        public void onAttack(LivingEntity possessed) {
            if (possessed instanceof OwlEntity owl) {
                MythosMortals.NETWORK.sendToServer(new OwlAttackServerPacket(owl.getId()));
            }
        }

        @Override
        public void onUse(LivingEntity possessed) {
            if (!(possessed instanceof OwlEntity owl)) {
                return;
            }
            Entity target = OwlAim.findAimedLiving(Minecraft.getInstance(), owl, MARK_REACH);
            if (target != null) {
                MythosMortals.NETWORK.sendToServer(new OwlMarkServerPacket(owl.getId(), target.getId()));
            }
        }

        @Override
        public void onMouseButton(LivingEntity possessed, int button) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && possessed instanceof OwlEntity owl) {
                MythosMortals.NETWORK.sendToServer(new OwlSonicAttackServerPacket(owl.getId()));
            }
        }
    }

    private OwlPossessionInput() {}

    @EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_KEY);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            PossessionClient.registerInputHandler(new OwlActions());
        }

        private ModEvents() {}
    }
}
