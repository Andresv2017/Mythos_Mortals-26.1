package net.darkblade.mythosmortals.entity.minotaur.client;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class MinotaurBossBarRenderer {

    private MinotaurBossBarRenderer() {
    }

    private static final Identifier BASE = tex("mino_bar_base");
    private static final Identifier FULL = tex("mino_bar_full");
    private static final Identifier OVERLAY = tex("mino_bar_overlay");

    private static final String MARKER_KEY = "entity.mythosmortals.minotaur";

    private static final int WIDTH = 182;
    private static final int BAND_Y = 10;
    private static final int BAND_HEIGHT = 5;

    private static final int BASE_HEIGHT = 17;
    private static final int FULL_HEIGHT = 18;
    private static final int OVERLAY_HEIGHT = 3;

    private static final int ART_OFFSET_Y = -BAND_Y;

    private static final int OVERLAY_OFFSET_Y = BAND_Y + (BAND_HEIGHT - OVERLAY_HEIGHT) / 2;

    private static final float FADE_MS = 450.0F;

    private static boolean drawnThisFrame;
    private static long lastDrawnMs;
    private static long appearedMs;
    private static int ghostX;
    private static int ghostY;
    private static float ghostProgress;

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.@NotNull BossEventProgress event) {
        if (!isMinotaurBar(event.getBossEvent().getName())) {
            return;
        }
        event.setCanceled(true);

        final long now = Util.getMillis();
        if (now - lastDrawnMs > FADE_MS) {
            appearedMs = now;
        }
        lastDrawnMs = now;
        drawnThisFrame = true;

        ghostX = event.getX();
        ghostY = event.getY();
        ghostProgress = event.getBossEvent().getProgress();

        draw(event.getGuiGraphics(), ghostX, ghostY, ghostProgress, clamp01((now - appearedMs) / FADE_MS));

        event.setIncrement(event.getIncrement() + BAND_Y);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.@NotNull Post event) {
        if (drawnThisFrame) {
            drawnThisFrame = false;
            return;
        }
        if (lastDrawnMs == 0L) {
            return;
        }
        final float elapsed = Util.getMillis() - lastDrawnMs;
        if (elapsed >= FADE_MS) {
            lastDrawnMs = 0L;
            return;
        }
        draw(event.getGuiGraphics(), ghostX, ghostY, ghostProgress, 1.0F - elapsed / FADE_MS);
    }

    private static void draw(@NotNull GuiGraphicsExtractor gui, int x, int anchorY, float progress, float alpha) {
        if (alpha <= 0.0F) {
            return;
        }
        final int tint = (Math.round(clamp01(alpha) * 255.0F) << 24) | 0x00FFFFFF;
        final int y = anchorY + ART_OFFSET_Y;

        gui.blit(RenderPipelines.GUI_TEXTURED, BASE, x, y, 0.0F, 0.0F, WIDTH, BASE_HEIGHT, WIDTH, BASE_HEIGHT, tint);

        final int filled = Math.round(WIDTH * clamp01(progress));
        if (filled > 0) {
            gui.blit(RenderPipelines.GUI_TEXTURED, FULL, x, y, 0.0F, 0.0F, filled, FULL_HEIGHT, WIDTH, FULL_HEIGHT, tint);
        }

        gui.blit(RenderPipelines.GUI_TEXTURED, OVERLAY, x, y + OVERLAY_OFFSET_Y,
                0.0F, 0.0F, WIDTH, OVERLAY_HEIGHT, WIDTH, OVERLAY_HEIGHT, tint);
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
