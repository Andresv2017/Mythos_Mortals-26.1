package net.darkblade.mythosmortals.content.amphora;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import org.jetbrains.annotations.NotNull;

/**
 * Las raciones de un ánfora llena <b>mientras es un ítem</b>, no un bloque.
 *
 * <p>Existe porque hay dos sitios que gastan raciones sin que el ánfora esté colocada —
 * {@link WineEvents} al beber de la mano y {@link MarinatingRecipe} al marinar con el aceite— y
 * repetir la lectura del componente en los dos era pedir que se desincronizaran.
 *
 * <p>Las raciones viven en el componente {@code block_state}, que es donde las deja el
 * {@code minecraft:copy_state} de la loot table al romper el bloque, y de donde las recoge el
 * {@code BlockItem} al recolocarlo. Un ánfora recién salida del horno <b>no lleva ese componente
 * todavía</b>, y por eso leer devuelve el máximo por defecto en vez de cero.
 */
public final class AmphoraServings {

    /** Cuántas raciones le quedan a este ítem-ánfora. */
    public static int of(@NotNull ItemStack amphora) {
        Integer stored = amphora
            .getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
            .get(FilledAmphoraBlock.SERVINGS);
        return stored == null ? FilledAmphoraBlock.MAX_SERVINGS : stored;
    }

    /**
     * Gasta una ración y devuelve lo que queda: la misma ánfora con una menos, o el <b>ánfora
     * vacía</b> si era la última. La vasija se conserva siempre — lo que se acaba es el contenido,
     * igual que al servirla a botellas.
     */
    public static @NotNull ItemStack spend(@NotNull ItemStack amphora) {
        int left = of(amphora);
        if (left <= 1) {
            return new ItemStack(MythosMortalsRegistry.GREEK_AMPHORA_ITEM.get());
        }
        ItemStack used = amphora.copyWithCount(1);
        used.set(DataComponents.BLOCK_STATE, used
            .getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
            .with(FilledAmphoraBlock.SERVINGS, left - 1));
        return used;
    }

    private AmphoraServings() {}
}
