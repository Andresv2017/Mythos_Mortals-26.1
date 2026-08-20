package net.darkblade.mythosmortals.content.amphora;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * El Ánfora Griega vacía: el punto de entrada de las dos cadenas de elaboración.
 *
 * <p>Su único trabajo es llenarse. Click derecho con <b>4 uvas</b> la convierte en
 * {@code greek_amphora_grapes}; con <b>4 aceitunas</b>, en {@code greek_amphora_olives}. De ahí en
 * adelante el bloque lleno es un ítem más que va al horno, y todo el paso siguiente son recetas —
 * ver el spec {@code docs/superpowers/specs/2026-08-17-anfora-griega-design.md}.
 *
 * <p><b>El recuento se hace sobre el stack de la mano, no sobre el inventario.</b> Es lo que hace
 * vanilla en todas sus interacciones de bloque (la tarta, el caldero, la maceta), y evita escribir
 * un barrido de inventario que además sorprendería al jugador recogiendo uvas de sitios que no está
 * mirando.
 *
 * <p>Con menos de 4 no pasa nada y no se gasta nada: llenar a medias no existe como estado, así que
 * cobrar por un intento fallido sería robar. Tampoco se avisa por chat — el mismo criterio que el
 * viñedo, que se explica solo probando.
 */
public class GreekAmphoraBlock extends Block {
    public static final MapCodec<GreekAmphoraBlock> CODEC = simpleCodec(GreekAmphoraBlock::new);

    /** Lo que cuesta llenar un ánfora, en cualquiera de las dos cadenas. Coincide a propósito con
     * las 4 raciones que rinde después: una fruta por botella. */
    public static final int FILL_COST = 4;

    public GreekAmphoraBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull MapCodec<? extends GreekAmphoraBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull InteractionResult useItemOn(@NotNull ItemStack itemStack, @NotNull BlockState state,
                                                   @NotNull Level level, @NotNull BlockPos pos,
                                                   @NotNull Player player, @NotNull InteractionHand hand,
                                                   @NotNull BlockHitResult hitResult) {
        if (itemStack.is(MythosMortalsItems.GRAPES.get())) {
            return tryFill(itemStack, level, pos, player, MythosMortalsRegistry.GREEK_AMPHORA_GRAPES);
        }
        if (itemStack.is(MythosMortalsItems.OLIVES.get())) {
            return tryFill(itemStack, level, pos, player, MythosMortalsRegistry.GREEK_AMPHORA_OLIVES);
        }
        // TRY_WITH_EMPTY_HAND y no PASS: deja que la cadena siga hasta useWithoutItem y, si nadie
        // consume, hasta el useOn del ítem en mano. Es el valor por defecto de BlockBehaviour.
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private InteractionResult tryFill(ItemStack itemStack, Level level, BlockPos pos, Player player,
                                      Supplier<? extends Block> filled) {
        if (itemStack.getCount() < FILL_COST) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        level.setBlock(pos, filled.get().defaultBlockState(), Block.UPDATE_ALL);
        itemStack.consume(FILL_COST, player);
        level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.SUCCESS;
    }
}
