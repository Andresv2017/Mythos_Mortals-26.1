package net.darkblade.mythosmortals.block.amphora;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import com.mojang.serialization.MapCodec;
import net.darkblade.mythosmortals.core.MythosMortals;
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
import net.darkblade.mythosmortals.registry.MythosMortalsBlocks;


public class MarinatingRecipe extends CustomRecipe {

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

        ItemStack marinated = food.copyWithCount(count);
        marinated.set(MythosMortalsRegistry.MARINATED.get(), Unit.INSTANCE);
        marinated.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return marinated;
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(MythosMortalsItems.OLIVE_OIL_BOTTLE.get())) {
                remaining.set(slot, new ItemStack(Items.GLASS_BOTTLE));
            } else if (stack.is(MythosMortalsBlocks.GREEK_AMPHORA_OLIVE_OIL_ITEM.get())) {
                remaining.set(slot, AmphoraServings.spend(stack));
            }
        }
        return remaining;
    }

    @Override
    public @NotNull RecipeSerializer<MarinatingRecipe> getSerializer() {
        return MythosMortalsRegistry.MARINATING.get();
    }

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
            || stack.is(MythosMortalsBlocks.GREEK_AMPHORA_OLIVE_OIL_ITEM.get());
    }

    private static int countFood(CraftingInput input) {
        ItemStack first = null;
        int count = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty() || isOil(stack)) {
                continue;
            }
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
