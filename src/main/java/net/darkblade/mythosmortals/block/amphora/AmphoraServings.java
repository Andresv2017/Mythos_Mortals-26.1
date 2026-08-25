package net.darkblade.mythosmortals.block.amphora;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import org.jetbrains.annotations.NotNull;
import net.darkblade.mythosmortals.registry.MythosMortalsBlocks;

public final class AmphoraServings {

    public static int of(@NotNull ItemStack amphora) {
        Integer stored = amphora
            .getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
            .get(FilledAmphoraBlock.SERVINGS);
        return stored == null ? FilledAmphoraBlock.MAX_SERVINGS : stored;
    }

    public static @NotNull ItemStack spend(@NotNull ItemStack amphora) {
        int left = of(amphora);
        if (left <= 1) {
            return new ItemStack(MythosMortalsBlocks.GREEK_AMPHORA_ITEM.get());
        }
        ItemStack used = amphora.copyWithCount(1);
        used.set(DataComponents.BLOCK_STATE, used
            .getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
            .with(FilledAmphoraBlock.SERVINGS, left - 1));
        return used;
    }

    private AmphoraServings() {}
}
