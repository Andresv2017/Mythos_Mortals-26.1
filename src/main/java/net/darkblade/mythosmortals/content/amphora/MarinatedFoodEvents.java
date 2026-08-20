package net.darkblade.mythosmortals.content.amphora;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import net.darkblade.mythosmortals.MythosMortals;
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

/**
 * El efecto de comerse algo marinado en aceite de oliva: <b>+2 de hambre y un 50% más de
 * saturación</b>.
 *
 * <p>Va después de comer y no en las propiedades del ítem porque NeoForge 26.1 <b>ya no tiene</b>
 * {@code getFoodProperties} — se leyó {@code IItemExtension} entero. No se puede devolver un
 * {@code FoodProperties} modificado antes de que vanilla lo aplique, así que se suma el extra justo
 * después, y el evento recibe una copia del stack previa al consumo (ver
 * {@code LivingEntity#completeUsingItem}) que todavía lleva el componente.
 *
 * <p><b>Por qué no es "el doble de saturación", que era el diseño original.</b> La saturación se
 * recorta al nivel de hambre (máximo 20), así que duplicarla desperdiciaba el bono justo en las
 * comidas fuertes: el filete marinado pedía 25.6 y cobraba 20. El marinado acababa siendo mejor
 * cuanto peor era la comida base — al revés de lo que cualquiera espera del condimento caro.
 *
 * <p>Con +50% ninguna comida de vanilla llega al techo (el filete, la más alta, se queda en 19.2),
 * así que el bono se cobra entero siempre. Y la nutrición, que es donde de verdad se nota que has
 * comido, entra como un <b>+2 plano</b>: plano y no proporcional para que tampoco escale con lo
 * fuerte que ya era el plato.
 */
@EventBusSubscriber(modid = MythosMortals.MODID)
public final class MarinatedFoodEvents {

    /** Muslo entero de hambre extra, igual para todas las comidas. */
    private static final int BONUS_NUTRITION = 2;

    /** Fracción de la saturación base que se suma encima. */
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

        // El hambre primero, y la saturación después: así el recorte de la segunda se mide contra el
        // nivel ya subido. Al revés, el +2 de hambre no serviría de nada para la saturación.
        data.setFoodLevel(Math.min(data.getFoodLevel() + BONUS_NUTRITION, FoodConstants.MAX_FOOD));

        // Los dos Math.min replican a mano los recortes de FoodData#add, que hace
        // Mth.clamp(food + foodLevel, 0, 20) y Mth.clamp(saturation + saturationLevel, 0, foodLevel).
        // Sin ellos, el marinado dejaría al jugador con más saturación que hambre — un estado que
        // vanilla no produce nunca.
        float bonus = food.saturation() * BONUS_SATURATION_FRACTION;
        data.setSaturation(Math.min(data.getSaturationLevel() + bonus, data.getFoodLevel()));
    }

    /** La marca visible mientras el glint de color siga en su propio spec. El nombre del ítem no se
     * toca: renombrar por componente obliga a {@code CUSTOM_NAME}, que sale en cursiva y le pisaría
     * el nombre a quien lo hubiera renombrado en un yunque. */
    @SubscribeEvent
    public static void onTooltip(@NotNull ItemTooltipEvent event) {
        if (!event.getItemStack().has(MythosMortalsRegistry.MARINATED.get())) {
            return;
        }
        event.getToolTip().add(Component.translatable("tooltip.deluxelib.marinated")
            .withStyle(ChatFormatting.GRAY));
        // Los números van escritos porque no se deducen de ningún sitio: el bono no vive en el
        // componente FOOD del ítem, así que la barra de muslos del tooltip sigue mostrando la comida
        // sin marinar.
        event.getToolTip().add(Component.translatable("tooltip.deluxelib.marinated.effect")
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    private MarinatedFoodEvents() {}
}
