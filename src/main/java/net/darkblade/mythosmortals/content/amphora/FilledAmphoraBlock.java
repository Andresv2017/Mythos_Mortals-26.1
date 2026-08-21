package net.darkblade.mythosmortals.content.amphora;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class FilledAmphoraBlock extends Block {

    public static final MapCodec<FilledAmphoraBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("product").forGetter(block -> block.product.get()),
        propertiesCodec()
    ).apply(i, (product, properties) -> new FilledAmphoraBlock(() -> product, properties)));

    public static final IntegerProperty SERVINGS = IntegerProperty.create("servings", 1, 4);

    public static final int MAX_SERVINGS = 4;

    private final Supplier<Item> product;

    public FilledAmphoraBlock(Supplier<Item> product, BlockBehaviour.Properties properties) {
        super(properties);
        this.product = product;
        registerDefaultState(getStateDefinition().any().setValue(SERVINGS, MAX_SERVINGS));
    }

    @Override
    public @NotNull MapCodec<? extends FilledAmphoraBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(SERVINGS);
    }

    @Override
    protected @NotNull InteractionResult useItemOn(@NotNull ItemStack itemStack, @NotNull BlockState state,
                                                   @NotNull Level level, @NotNull BlockPos pos,
                                                   @NotNull Player player, @NotNull InteractionHand hand,
                                                   @NotNull BlockHitResult hitResult) {
        if (!itemStack.is(Items.GLASS_BOTTLE)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        player.setItemInHand(hand,
            ItemUtils.createFilledResult(itemStack, player, new ItemStack(product.get())));

        takeServing(state, level, pos);
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
        return InteractionResult.SUCCESS;
    }

    private static void takeServing(BlockState state, Level level, BlockPos pos) {
        int left = state.getValue(SERVINGS);
        if (left > 1) {
            level.setBlock(pos, state.setValue(SERVINGS, left - 1), Block.UPDATE_ALL);
        } else {
            level.setBlock(pos, MythosMortalsRegistry.GREEK_AMPHORA.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
