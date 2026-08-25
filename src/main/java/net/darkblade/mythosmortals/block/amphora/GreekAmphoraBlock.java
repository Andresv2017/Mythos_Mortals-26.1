package net.darkblade.mythosmortals.block.amphora;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;

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
import net.darkblade.mythosmortals.registry.MythosMortalsBlocks;


public class GreekAmphoraBlock extends Block {
    public static final MapCodec<GreekAmphoraBlock> CODEC = simpleCodec(GreekAmphoraBlock::new);

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
            return tryFill(itemStack, level, pos, player, MythosMortalsBlocks.GREEK_AMPHORA_GRAPES);
        }
        if (itemStack.is(MythosMortalsItems.OLIVES.get())) {
            return tryFill(itemStack, level, pos, player, MythosMortalsBlocks.GREEK_AMPHORA_OLIVES);
        }
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
