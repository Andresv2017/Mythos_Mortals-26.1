package net.darkblade.mythosmortals.content.pegasus.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Tack screen for the pegasus. Borrows the vanilla horse container art — same three-slot column on
 * the left, same player inventory below — and draws its own slot backdrops, since the pegasus has a
 * third slot where a horse has none.
 */
public class PegasusInventoryScreen extends AbstractContainerScreen<PegasusInventoryMenu> {

    private static final Identifier BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/horse.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    public PegasusInventoryScreen(PegasusInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, xo, yo, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);

        // Saddle, armour, bridle — the menu places the slots one row apart starting at y 18.
        for (int row = 0; row < 3; row++) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, xo + 7, yo + 17 + row * 18, 18, 18);
        }
    }
}
