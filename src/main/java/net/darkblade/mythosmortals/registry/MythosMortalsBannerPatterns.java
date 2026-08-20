package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.MythosMortals;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BannerPattern;

/**
 * Los tres patrones de estandarte griegos del mod de ejemplo, y el único trozo de Java que
 * necesitan.
 *
 * <p>El patrón en sí <b>no se registra desde código</b>: {@code minecraft:banner_pattern} es un
 * registro de datapack, así que la entrada vive entera en
 * {@code data/deluxelib/banner_pattern/<id>.json} ({@code asset_id} + {@code translation_key}).
 * Tampoco hay nada que registrar del lado del cliente: el {@code DirectoryLister} del atlas de
 * vanilla lista {@code textures/entity/banner} y {@code textures/entity/shield} en <i>todos</i> los
 * namespaces, y {@code SpriteMapper} conserva el nuestro al derivar el sprite del {@code asset_id}.
 *
 * <p>Lo único que no puede vivir en JSON son estos {@link TagKey}: {@code LoomMenu} sólo ofrece un
 * patrón si el ítem que está en la ranura lleva el componente
 * {@code DataComponents.PROVIDES_BANNER_PATTERNS}, y ese componente es un {@code HolderSet} que se
 * resuelve desde un tag. De ahí el rodeo un-tag-por-ítem, calcado de
 * {@code BannerPatternTags.PATTERN_ITEM_*}: el ítem apunta al tag y el tag apunta al patrón.
 *
 * <p>El escudo no aparece por ningún lado a propósito. {@code ShieldDecorationRecipe} es genérica —
 * copia el componente {@code banner_patterns} de cualquier estandarte a cualquier escudo sin
 * patrones — y {@code ShieldSpecialRenderer} pinta cada capa con el sprite de
 * {@code entity/shield/}. Con que exista la entrada de registro y el PNG, el escudo funciona solo.
 */
public final class MythosMortalsBannerPatterns {

    /** La lechuza de Atenea. Ver {@code data/deluxelib/tags/banner_pattern/pattern_item/athena.json}. */
    public static final TagKey<BannerPattern> PATTERN_ITEM_ATHENA = create("athena");
    /** El centauro. */
    public static final TagKey<BannerPattern> PATTERN_ITEM_CENTAUR = create("centaur");
    /** La lambda espartana. */
    public static final TagKey<BannerPattern> PATTERN_ITEM_SPARTA = create("sparta");

    private static TagKey<BannerPattern> create(String name) {
        return TagKey.create(Registries.BANNER_PATTERN,
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "pattern_item/" + name));
    }

    private MythosMortalsBannerPatterns() {}
}
