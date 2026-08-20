package net.darkblade.mythosmortals.content.amphora;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.mythosmortals.content.effect.BorealCourageEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Beberse el vino griego: por fin alguien llama a {@link BorealCourageEffect#apply}, que llevaba
 * implementado desde el spec de las armas sin gatillo.
 *
 * <p><b>La ventana del castigo es el propio buff.</b> Si bebes con Valentía Boreal todavía activa,
 * el buff se renueva <i>y además</i> te mareas; si esperas a que caduque, el trago sale limpio. No
 * hay temporizador aparte y por tanto tampoco estado que guardar: el icono del HUD <i>es</i> el
 * temporizador, y el jugador lo tiene delante. Ésa era la pega de la versión anterior —dos minutos
 * invisibles que castigaban sin avisar.
 *
 * <p>Y el atajo se cobra solo: un cubo de leche limpia la náusea, sí, pero también se lleva por
 * delante la Valentía Boreal. Quien lo use paga con el buff que estaba intentando mantener.
 *
 * <p>Va en {@link LivingEntityUseItemEvent.Finish} porque la decisión depende de qué efectos tiene
 * ya el jugador, y eso no cabe en un {@code ConsumeEffect} de datos. El evento recibe una copia del
 * stack previa al consumo (ver {@code LivingEntity#completeUsingItem}), así que el
 * {@code is(WINE_BOTTLE)} todavía ve la botella llena.
 */
@EventBusSubscriber(modid = MythosMortals.MODID)
public final class WineEvents {

    /** Reincidir una vez: incómodo, corto, y te deja terminar la pelea. */
    private static final int NAUSEA_TICKS = 10 * 20;

    /** Reincidir estando ya mareado: el doble de tiempo y Náusea II. */
    private static final int HEAVY_NAUSEA_TICKS = 20 * 20;

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.@NotNull Finish event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getItem().is(MythosMortalsItems.WINE_BOTTLE.get())) {
            drink(player);
            return;
        }
        if (event.getItem().is(MythosMortalsRegistry.GREEK_AMPHORA_WINE_ITEM.get())) {
            drink(player);
            returnAmphora(event, player);
        }
    }

    /**
     * El ánfora que acabas de beberte vuelve a tu mano con una ración menos, o vacía si era la
     * última. Vanilla ya le ha restado 1 al stack, así que aquí sólo hay que decidir qué entra en su
     * lugar.
     *
     * <p>No sirve un {@code USE_REMAINDER} en las propiedades del ítem: ése devuelve siempre el
     * mismo ítem fijo, y lo que toca aquí depende de cuántas raciones quedaban.
     *
     * <p>El caso del stack de dos ánforas está contemplado: si tras el trago todavía queda alguna en
     * la mano, la que acabas de usar va al inventario en vez de sustituir a las otras, que si no se
     * perderían.
     */
    private static void returnAmphora(LivingEntityUseItemEvent.Finish event, Player player) {
        ItemStack used = AmphoraServings.spend(event.getItem());
        ItemStack leftover = event.getResultStack();
        if (leftover.isEmpty()) {
            event.setResultStack(used);
        } else if (!player.getInventory().add(used)) {
            player.drop(used, false);
        }
    }

    /**
     * Un trago de vino: el buff siempre, y el mareo sólo si llegabas ya servido. Público porque el
     * ánfora se puede beber a morro sin botella de por medio — ver
     * {@link FilledAmphoraBlock#useWithoutItem}.
     *
     * <p>Los dos {@code hasEffect} se leen <b>antes</b> de aplicar nada: {@code apply} renueva la
     * Valentía Boreal, así que consultarla después diría siempre que sí.
     */
    public static void drink(@NotNull Player player) {
        boolean stillBuffed = player.hasEffect(MythosMortalsRegistry.BOREAL_COURAGE);
        boolean alreadyDizzy = player.hasEffect(MobEffects.NAUSEA);

        BorealCourageEffect.apply(player);

        if (!stillBuffed) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA,
            alreadyDizzy ? HEAVY_NAUSEA_TICKS : NAUSEA_TICKS,
            alreadyDizzy ? 1 : 0));
    }

    private WineEvents() {}
}
