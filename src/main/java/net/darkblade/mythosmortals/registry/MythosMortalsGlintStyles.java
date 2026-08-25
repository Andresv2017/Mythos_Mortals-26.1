package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.deluxelib.client.render.GlintStyle;
import net.darkblade.deluxelib.client.render.GlintStyles;
import net.minecraft.resources.Identifier;

public final class MythosMortalsGlintStyles {

    public static void register() {
        GlintStyles.register(MythosMortalsDataComponents.MARINATED, new GlintStyle(
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/misc/olive_oil_glint.png")));
    }

    private MythosMortalsGlintStyles() {}
}
