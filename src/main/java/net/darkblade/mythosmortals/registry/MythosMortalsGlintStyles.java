package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.deluxelib.client.render.GlintStyle;
import net.darkblade.deluxelib.client.render.GlintStyles;
import net.minecraft.resources.Identifier;

/**
 * Los estilos de brillo del demo. Hoy sólo uno: la comida marinada en aceite de oliva brilla en
 * dorado en vez de en el morado de vanilla.
 *
 * <p><b>Vive en una clase propia por seguridad de dist.</b> Lo llama el constructor de
 * {@link MythosMortals} tras comprobar que estamos en cliente, así que en un servidor dedicado esta
 * clase no se carga nunca y sus referencias a {@code GlintStyles} —que arrastra clases de render—
 * no se resuelven. Es seguridad por alcance, no por anotación.
 *
 * <p><b>Y va en el constructor y no en {@code FMLClientSetupEvent}</b>, que es donde estaba y por lo
 * que el brillo no se veía: {@code Minecraft.<init>} construye sus {@code RenderBuffers} antes de la
 * primera recarga de recursos, de la que cuelga el setup. Registrado allí, el estilo llegaba tarde a
 * reservar su buffer fijo. Ver {@link GlintStyles} para el desarrollo completo.
 *
 * <p>El componente se pasa sin resolver ({@code MARINATED} es un {@code DeferredHolder}, o sea un
 * {@code Supplier}) porque en la construcción del mod todavía no ha corrido el {@code RegisterEvent}
 * que lo vincula.
 */
public final class MythosMortalsGlintStyles {

    public static void register() {
        GlintStyles.register(MythosMortalsRegistry.MARINATED, new GlintStyle(
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/misc/olive_oil_glint.png")));
    }

    private MythosMortalsGlintStyles() {}
}
