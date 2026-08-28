package net.darkblade.mythosmortals.entity.minotaur.client;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the Minotaur's boss bar in place of vanilla's.
 *
 * <p>Vanilla's {@code BossHealthOverlay} fires a cancellable hook per bar, and cancelling it skips
 * both the bar and the name text — which is exactly the "bar only, no name" the art is designed
 * for, since the medallion already says whose bar it is.
 */
@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class MinotaurBossBarRenderer {

    private MinotaurBossBarRenderer() {
    }

    private static final Identifier BASE = tex("mino_bar_base");
    private static final Identifier FULL = tex("mino_bar_full");
    private static final Identifier OVERLAY = tex("mino_bar_overlay");

    /**
     * The bar's name is never rendered; it is purely how the client recognises its own bar among
     * everyone else's. Matching the translation key rather than the resolved text keeps it working
     * whatever language the player runs.
     */
    private static final String MARKER_KEY = "entity.mythosmortals.minotaur";

    // Vanilla's bar is 182px wide and 5px tall; the art wraps that band in a medallion, so each
    // sheet is 182 wide with the band itself sitting at rows 10-14.
    private static final int WIDTH = 182;
    private static final int BAND_Y = 10;
    private static final int BAND_HEIGHT = 5;

    private static final int BASE_HEIGHT = 17;
    private static final int FULL_HEIGHT = 18;
    private static final int OVERLAY_HEIGHT = 3;

    // The medallion overhangs the band by BAND_Y pixels. Drawing the sheet that far above the
    // anchor puts the band exactly where vanilla would have drawn its own bar, so the bar keeps its
    // usual screen position and only the medallion sticks out.
    private static final int ART_OFFSET_Y = -BAND_Y;

    // Centres the 3px notch strip on the 5px band.
    private static final int OVERLAY_OFFSET_Y = BAND_Y + (BAND_HEIGHT - OVERLAY_HEIGHT) / 2;

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.@NotNull BossEventProgress event) {
        if (!isMinotaurBar(event.getBossEvent().getName())) {
            return;
        }
        // Take the bar over entirely: vanilla draws neither the bar nor the name from here on.
        event.setCanceled(true);

        final GuiGraphicsExtractor gui = event.getGuiGraphics();
        final int x = event.getX();
        final int y = event.getY() + ART_OFFSET_Y;

        gui.blit(RenderPipelines.GUI_TEXTURED, BASE, x, y, 0.0F, 0.0F, WIDTH, BASE_HEIGHT, WIDTH, BASE_HEIGHT);

        // base and full share a pixel-identical layout, so clipping the fill from the left also
        // clips the medallion: it lights up as health crosses the halfway mark.
        final int filled = Math.round(WIDTH * clamp01(event.getBossEvent().getProgress()));
        if (filled > 0) {
            gui.blit(RenderPipelines.GUI_TEXTURED, FULL, x, y, 0.0F, 0.0F, filled, FULL_HEIGHT, WIDTH, FULL_HEIGHT);
        }

        gui.blit(RenderPipelines.GUI_TEXTURED, OVERLAY, x, y + OVERLAY_OFFSET_Y,
                0.0F, 0.0F, WIDTH, OVERLAY_HEIGHT, WIDTH, OVERLAY_HEIGHT);

        // Give the next bar down the medallion's headroom back, or it would overlap the horns.
        event.setIncrement(event.getIncrement() + BAND_Y);
    }

    private static boolean isMinotaurBar(@NotNull Component name) {
        return name.getContents() instanceof TranslatableContents contents
                && MARKER_KEY.equals(contents.getKey());
    }

    private static float clamp01(float value) {
        return value < 0.0F ? 0.0F : Math.min(value, 1.0F);
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/gui/" + name + ".png");
    }
}
