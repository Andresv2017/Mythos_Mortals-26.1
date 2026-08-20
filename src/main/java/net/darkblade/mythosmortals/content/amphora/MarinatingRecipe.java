package net.darkblade.mythosmortals.content.amphora;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import com.mojang.serialization.MapCodec;
import net.darkblade.mythosmortals.MythosMortals;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Marinar: una fuente de aceite de oliva y <b>hasta ocho comidas cocinadas alrededor</b>, y salen
 * todas marinadas de una tacada.
 *
 * <p>Ocho no es un número elegido: son las casillas que rodean al centro en una rejilla de 3×3, así
 * que el límite se cuenta solo y no hay que comprobarlo. Rodear el aceite de pescado y sacar los
 * ocho marinados es la forma natural de usar esto.
 *
 * <p>Es una {@link CustomRecipe} y no una receta sin forma normal porque <b>no hay una receta, hay
 * una familia entera</b>: una por cada comida de {@link #MARINATABLE}, y además el número de
 * resultados depende de cuántas metas. El precio es que {@code isSpecial()} es {@code true} y esto
 * no sale en el libro de recetas — el mismo trato que aceptan el teñido de armadura y el copiado de
 * mapas de vanilla.
 *
 * <p><b>Todas las comidas tienen que ser la misma.</b> No es una restricción caprichosa: una receta
 * de crafteo produce <i>un</i> {@link ItemStack}, y un stack no puede ser mitad bacalao y mitad
 * pollo. Mezclar simplemente no casa, y no pasa nada.
 *
 * <p>Dos fuentes de aceite: la <b>botella</b>, que se gasta entera en la tacada y devuelve el
 * cristal, y el <b>ánfora</b>, que gasta una de sus cuatro raciones — cuatro tacadas de ocho, 32
 * comidas por ánfora, lo mismo que si la hubieras embotellado.
 */
public class MarinatingRecipe extends CustomRecipe {

    /** Qué se puede marinar. Vive en {@code data/deluxelib/tags/item/marinatable.json}: una
     * etiqueta y no una comprobación en Java para que la lista sea editable sin recompilar. */
    public static final TagKey<Item> MARINATABLE =
        ItemTags.create(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "marinatable"));

    public static final MapCodec<MarinatingRecipe> MAP_CODEC = MapCodec.unit(MarinatingRecipe::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, MarinatingRecipe> STREAM_CODEC =
        StreamCodec.unit(new MarinatingRecipe());

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        return findOil(input) != null && countFood(input) > 0;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input) {
        ItemStack food = firstFood(input);
        int count = countFood(input);
        if (food == null || count <= 0) {
            return ItemStack.EMPTY;
        }

        // copyWithCount y no un stack nuevo del mismo Item: la comida puede llevar encantamientos,
        // nombre de yunque o cualquier otro componente, y marinarla no debe borrárselos.
        ItemStack marinated = food.copyWithCount(count);
        // MARINATED es la señal de negocio y la que elige el color: GlintStyles indexa por componente.
        marinated.set(MythosMortalsRegistry.MARINATED.get(), Unit.INSTANCE);
        // Y este otro es lo que hace que haya brillo <b>que</b> recolorear. No es una marca
        // provisional que sobrara al implementar el glint de color: ItemFeatureRenderer#renderItem
        // envuelve todo el pase del foil en un `if (foilType != NONE)`, y el foilType sólo se pone si
        // ItemStack#hasFoil() es cierto. Sin este componente el ítem no dibuja brillo ninguno y no
        // hay nada que teñir de dorado. Sólo hay un pase, así que no se superponen dos brillos:
        // ItemFeatureRendererMixin sustituye el RenderType de ese único pase.
        marinated.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return marinated;
    }

    /**
     * Lo que vuelve a la rejilla: la botella de cristal si el aceite venía en botella, o el ánfora
     * con una ración menos si venía en ánfora.
     *
     * <p>La botella se resolvería sola con su {@code craftRemainder}, pero el ánfora no —lo que
     * devuelve depende de cuántas raciones le quedaban—, así que las dos se escriben aquí y no hay
     * que ir a buscar la mitad de la respuesta a las propiedades del ítem.
     */
    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(MythosMortalsItems.OLIVE_OIL_BOTTLE.get())) {
                remaining.set(slot, new ItemStack(Items.GLASS_BOTTLE));
            } else if (stack.is(MythosMortalsRegistry.GREEK_AMPHORA_OLIVE_OIL_ITEM.get())) {
                remaining.set(slot, AmphoraServings.spend(stack));
            }
        }
        return remaining;
    }

    @Override
    public @NotNull RecipeSerializer<MarinatingRecipe> getSerializer() {
        return MythosMortalsRegistry.MARINATING.get();
    }

    /** Exactamente una fuente de aceite. Con dos devuelve {@code null}: dos aceites en la rejilla es
     * un error del jugador, no media receta. */
    private static @Nullable ItemStack findOil(CraftingInput input) {
        ItemStack found = null;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (isOil(stack)) {
                if (found != null) {
                    return null;
                }
                found = stack;
            }
        }
        return found;
    }

    private static boolean isOil(ItemStack stack) {
        return stack.is(MythosMortalsItems.OLIVE_OIL_BOTTLE.get())
            || stack.is(MythosMortalsRegistry.GREEK_AMPHORA_OLIVE_OIL_ITEM.get());
    }

    /**
     * Cuántas comidas marinables hay, o {@code -1} si la rejilla no vale: algo que no es ni aceite
     * ni comida marinable, una comida ya marinada, o dos comidas distintas entre sí.
     *
     * <p>Devolver {@code -1} y no lanzar mantiene {@link #matches} en una sola condición.
     */
    private static int countFood(CraftingInput input) {
        ItemStack first = null;
        int count = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty() || isOil(stack)) {
                continue;
            }
            // Ya marinada, o directamente no marinable: la rejilla entera no vale.
            if (!stack.is(MARINATABLE) || stack.has(MythosMortalsRegistry.MARINATED.get())) {
                return -1;
            }
            if (first == null) {
                first = stack;
            } else if (!ItemStack.isSameItemSameComponents(first, stack)) {
                return -1;
            }
            count++;
        }
        return count;
    }

    private static @Nullable ItemStack firstFood(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && !isOil(stack)) {
                return stack;
            }
        }
        return null;
    }
}
