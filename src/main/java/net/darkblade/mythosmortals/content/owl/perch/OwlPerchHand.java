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

/**
 * "The owl is in your hand": while it is perched, its hotbar slot shows an owl icon, and reaching for
 * anything else lets the bird go.
 *
 * <p>Perching already requires an empty hand. This makes that legible — the game has no other way to
 * tell you your hand is occupied by a bird — and keeps it honest afterwards, rather than being a check
 * that only held at the instant you perched.
 *
 * <p><b>Two rules, and they only work together.</b> Switching slot releases the owl, so the hand is
 * never showing one thing while holding another. And an occupied slot is skipped by item pickups
 * ({@code InventoryFreeSlotMixin}), so the <em>only</em> way the hand can change is you deciding to
 * change it. Drop either one and the other stops meaning anything: without the release you get an owl
 * welded to a slot you have moved on from, and without the pickup guard a stray arrow silently takes
 * the hand out from under it.
 *
 * <p><b>There is no item.</b> Nothing is registered, no {@code ItemStack} exists, nothing enters the
 * inventory: the texture is drawn straight over the slot, in the HUD and in the inventory screen
 * alike. That is what makes "you can't drop it with Q, move it, or have it stolen" true by
 * construction rather than by a list of prohibitions someone has to remember to keep closed — a real
 * stack in a real slot would be a duplicable owl.
 *
 * <p>Purely client-side. The perch itself is server-authoritative; releasing goes through the same
 * dismount request the normal gesture uses, so a modified client can at worst let go of its own bird.
 *
 * <p><b>Owls only</b>, and that has to be checked explicitly — see {@link #owlPerchedOn}. This handler
 * is registered for the whole mod, but perching is a library-wide mechanic that any {@code Perchable}
 * can use, so gating on "is something perched" instead of "is an <em>owl</em> perched" leaks both
 * halves of this feature onto every other perchable creature: the sprite over their slot, and the
 * scroll-wheel release that drops them.
 */
@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class OwlPerchHand {

    private static final Identifier ICON =
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/item/copper_owl_item.png");

    /** Icon size, and the offsets vanilla itself draws hotbar contents at, so the owl lands exactly
     * where an item would ({@code Gui.renderHotbar}). */
    private static final int ICON_SIZE = 16;
    private static final int HOTBAR_LEFT_OFFSET = 90;
    private static final int SLOT_WIDTH = 20;
    private static final int SLOT_X_PADDING = 2;
    private static final int SLOT_Y_FROM_BOTTOM = 16 + 3;

    /** Nudge applied to both draw sites, on top of the exact slot position. The owl sprite doesn't
     * fill its 16×16 the way a normal item icon does, so sitting it dead-centre in the slot reads as
     * low; lifting it a couple of pixels makes it look seated rather than dropped in. Negative Y is
     * up. Purely cosmetic — change the numbers, both the HUD and the inventory follow. */
    private static final int ICON_OFFSET_X = 1;
    private static final int ICON_OFFSET_Y = 0;

    /** Slot the owl is occupying, or {@code -1} when it isn't perched. Captured the first tick the
     * perch is observed, because the client learns about perching from the entity's synced state
     * rather than from the interaction that caused it. */
    private static int lockedSlot = -1;
    /** Set once a release has been asked for, so the request isn't repeated every tick during the
     * round trip — the perch only actually ends when the server says so. */
    private static boolean releaseRequested;

    /**
     * Lets the owl go the moment the player reaches for something else.
     *
     * <p>Watching the value rather than intercepting input catches the scroll wheel, the number keys
     * and anything else that can move the selection, in one place, without depending on which of them
     * has a cancellable event on this version.
     */
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

    /** Draws the owl over its slot in the HUD hotbar. */
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

    /**
     * Draws the owl over its slot inside any container screen — the inventory, a chest, anything
     * showing the player's hotbar row.
     *
     * <p>Needed as its own handler because the in-game HUD isn't drawn at all while a screen is open,
     * so {@link #onRenderHotbar} simply never fires there and the icon would vanish the moment you
     * pressed E. The row you see in the inventory is a completely separate render path: menu slots.
     *
     * <p>{@code Render.Background} rather than {@code Post} is the documented slot-overlay phase — it
     * runs after the screen's background and before its contents, so the icon sits under tooltips and
     * under a stack being dragged, exactly where a slot decoration belongs.
     */
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

    /**
     * Whether the thing perched on this player is actually an owl.
     *
     * <p>Perching is a library-wide mechanic — anything implementing {@code Perchable} uses it, in this
     * mod or any dependent one. This class is not: it is the owl's own "the bird is in your hand"
     * affordance, tied to the owl's sprite and to the owl's rule that its slot may not change. Asking
     * {@link PerchClient#perchedEntityIdFor} alone answers "is <em>something</em> perched", which is
     * not the same question, and answering it that way made every other perchable creature paint an
     * owl over the hotbar and get dropped by the scroll wheel.
     */
    private static boolean owlPerchedOn(@NotNull LocalPlayer player) {
        int perchedId = PerchClient.perchedEntityIdFor(player.getId());
        return perchedId != -1 && player.level().getEntity(perchedId) instanceof OwlEntity;
    }

    /** Whether there is a perched owl whose slot is genuinely empty. The emptiness check is a
     * belt-and-braces against ever painting over a real item: pickups already skip the slot, so it
     * should never be false while perched. */
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
