package net.darkblade.mythosmortals.content.owl.perch;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.deluxelib.client.PerchClient;
import net.darkblade.mythosmortals.content.owl.OwlEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class OwlPerchHand {

    private static final Identifier ICON =
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/item/copper_owl_item.png");

    private static final int ICON_SIZE = 16;
    private static final int HOTBAR_LEFT_OFFSET = 90;
    private static final int SLOT_WIDTH = 20;
    private static final int SLOT_X_PADDING = 2;
    private static final int SLOT_Y_FROM_BOTTOM = 16 + 3;

    private static final int ICON_OFFSET_X = 1;
    private static final int ICON_OFFSET_Y = 0;

    private static int lockedSlot = -1;
    private static boolean releaseRequested;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.@NotNull Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !owlPerchedOn(player)) {
            lockedSlot = -1;
            releaseRequested = false;
            return;
        }
        int selected = player.getInventory().getSelectedSlot();
        if (lockedSlot == -1) {
            lockedSlot = selected;
            return;
        }
        if (selected != lockedSlot && !releaseRequested) {
            releaseRequested = true;
            PerchClient.requestDismount();
        }
    }

    @SubscribeEvent
    public static void onRenderHotbar(RenderGuiLayerEvent.@NotNull Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR) || !shouldDraw()) {
            return;
        }
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int x = graphics.guiWidth() / 2 - HOTBAR_LEFT_OFFSET + lockedSlot * SLOT_WIDTH + SLOT_X_PADDING;
        int y = graphics.guiHeight() - SLOT_Y_FROM_BOTTOM;
        draw(graphics, x, y);
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.@NotNull Background event) {
        if (!shouldDraw() || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == inventory && slot.getContainerSlot() == lockedSlot) {
                // Slot coordinates are relative to the screen's own origin; this event fires outside
                // that translation, so add it back by hand.
                draw(event.getGuiGraphics(), screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y);
                return;
            }
        }
    }


    private static boolean owlPerchedOn(@NotNull LocalPlayer player) {
        int perchedId = PerchClient.perchedEntityIdFor(player.getId());
        return perchedId != -1 && player.level().getEntity(perchedId) instanceof OwlEntity;
    }


    private static boolean shouldDraw() {
        if (lockedSlot == -1) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.getInventory().getItem(lockedSlot).isEmpty();
    }

    private static void draw(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, x + ICON_OFFSET_X, y + ICON_OFFSET_Y,
                0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    private OwlPerchHand() {}
}
