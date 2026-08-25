package net.darkblade.mythosmortals.block.amphora;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.NotNull;


@EventBusSubscriber(modid = MythosMortals.MODID)
public final class MarinatedFoodEvents {

    private static final int BONUS_NUTRITION = 2;

    private static final float BONUS_SATURATION_FRACTION = 0.5F;

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.@NotNull Finish event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItem();
        if (!stack.has(MythosMortalsRegistry.MARINATED.get())) {
            return;
        }
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) {
            return;
        }

        FoodData data = player.getFoodData();

        data.setFoodLevel(Math.min(data.getFoodLevel() + BONUS_NUTRITION, FoodConstants.MAX_FOOD));

        float bonus = food.saturation() * BONUS_SATURATION_FRACTION;
        data.setSaturation(Math.min(data.getSaturationLevel() + bonus, data.getFoodLevel()));
    }

    @SubscribeEvent
    public static void onTooltip(@NotNull ItemTooltipEvent event) {
        if (!event.getItemStack().has(MythosMortalsRegistry.MARINATED.get())) {
            return;
        }
        event.getToolTip().add(Component.translatable("tooltip.mythosmortals.marinated")
            .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable("tooltip.mythosmortals.marinated.effect")
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    private MarinatedFoodEvents() {}
}
