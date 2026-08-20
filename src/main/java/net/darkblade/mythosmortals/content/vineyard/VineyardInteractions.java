package net.darkblade.mythosmortals.content.vineyard;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import net.darkblade.mythosmortals.MythosMortals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import org.jetbrains.annotations.NotNull;

/**
 * El único evento que necesita el viñedo: coloca el primer {@link GrapeStakeBlock} al hacer click
 * derecho con un palo de vanilla sobre tierra.
 *
 * <p>Existe <b>sólo</b> porque el palo y la tierra son bloques e ítems de vanilla y no hay dónde
 * meterles código. Todo lo demás (apilar el segundo poste, plantar la uva) vive en el
 * {@code useItemOn} de nuestros propios bloques, que es el sitio idiomático.
 *
 * <p>Escucha la fase {@link UseItemOnBlockEvent.UsePhase#ITEM_AFTER_BLOCK}, la última de las tres.
 * Es la correcta: la tierra de vanilla no consume la acción, así que la cadena de
 * {@code ServerPlayerGameMode#useItemOn} llega hasta aquí sin que nadie la corte. Y como es la
 * última fase, si algún día el jugador tiene en la mano un ítem que sí quiere hacer algo con la
 * tierra, ese ítem gana — no le robamos la interacción.
 *
 * <p>El evento dispara en cliente y servidor. La colocación se hace sólo en servidor; el cliente se
 * limita a devolver {@code SUCCESS} para que el brazo se mueva y espera el paquete de bloque.
 */
@EventBusSubscriber(modid = MythosMortals.MODID)
public final class VineyardInteractions {

    @SubscribeEvent
    public static void onUseItemOnBlock(@NotNull UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK) {
            return;
        }
        ItemStack itemStack = event.getItemStack();
        if (!itemStack.is(Items.STICK)) {
            return;
        }
        // Sólo la cara de arriba: clicar el lateral de un bloque de tierra no debe plantar un poste
        // flotando al lado.
        if (event.getFace() != Direction.UP) {
            return;
        }

        Level level = event.getLevel();
        BlockPos soilPos = event.getPos();
        if (!level.getBlockState(soilPos).is(GrapeStakeBlock.STAKE_PLACEABLE)) {
            return;
        }

        BlockPos stakePos = soilPos.above();
        if (!level.isInsideBuildHeight(stakePos) || !level.getBlockState(stakePos).canBeReplaced()) {
            return;
        }

        Player player = event.getPlayer();
        if (!level.isClientSide()) {
            BlockState placed = MythosMortalsRegistry.STICK_BLOCK.get().defaultBlockState();
            level.setBlock(stakePos, placed, Block.UPDATE_ALL);
            itemStack.consume(1, player);
            GrapeStakeBlock.announcePlacement(level, stakePos, placed, player);
        }

        event.cancelWithResult(InteractionResult.SUCCESS);
    }

    private VineyardInteractions() {}
}
