package net.darkblade.mythosmortals.content.olive;

import net.darkblade.mythosmortals.MythosMortals;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Optional;

/**
 * El enganche entre el brote de olivo y el árbol que genera el mundo.
 *
 * <p>Las dos constantes apuntan al mismo {@code configured_feature}, así que el olivo que crece de
 * un brote y el que el generador siembra en llanuras y sabana son <b>literalmente el mismo árbol</b>.
 * Afinar la forma (el {@code blob_foliage_placer} de
 * {@code data/deluxelib/worldgen/configured_feature/olive.json}) se hace en un único sitio y afecta
 * a los dos caminos.
 */
public final class OliveTree {

    /** El árbol, definido en {@code data/deluxelib/worldgen/configured_feature/olive.json}.
     * Un {@link ResourceKey} es sólo un nombre: no exige que el feature esté cargado todavía, así que
     * es seguro construirlo durante el registro de bloques. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED =
        ResourceKey.create(Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "olive"));

    /**
     * El grower que usa el {@code SaplingBlock} de vanilla. Sin árbol gigante y sin variante
     * florida: el olivo tiene una sola forma.
     *
     * <p>El nombre lleva namespace <b>a propósito</b>. El constructor de {@link TreeGrower} se
     * registra en un {@code Map<String, TreeGrower>} estático y global, compartido por todos los mods
     * cargados, y ese mapa es lo que resuelve el códec de {@code SaplingBlock} al leer un blockstate
     * guardado. Un {@code "olive"} a secas sería una colisión esperando a otro mod mediterráneo, y la
     * víctima sería el que cargara segundo — con brotes que crecerían el árbol equivocado.
     */
    public static final TreeGrower GROWER =
        new TreeGrower(MythosMortals.MODID + ":olive", Optional.empty(), Optional.of(CONFIGURED), Optional.empty());

    private OliveTree() {}
}
