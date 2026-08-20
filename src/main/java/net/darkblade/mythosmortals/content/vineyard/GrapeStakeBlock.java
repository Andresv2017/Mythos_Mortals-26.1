package net.darkblade.mythosmortals.content.vineyard;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import com.mojang.serialization.MapCodec;
import net.darkblade.mythosmortals.MythosMortals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GrapeStakeBlock extends Block {
    public static final MapCodec<GrapeStakeBlock> CODEC = simpleCodec(GrapeStakeBlock::new);

    public static final TagKey<Block> STAKE_PLACEABLE =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MythosMortals.MODID, "stake_placeable"));

    private static final VoxelShape SHAPE = Block.column(4.0, 0.0, 16.0);

    private static final int VINE_STAKES = 2;

    private static final int MAX_COLUMN_SCAN = VINE_STAKES + 1;

    public GrapeStakeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends GrapeStakeBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                           @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) || below.is(STAKE_PLACEABLE);
    }

    @Override
    protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull LevelReader level,
                                              @NotNull ScheduledTickAccess ticks, @NotNull BlockPos pos,
                                              @NotNull Direction directionToNeighbour, @NotNull BlockPos neighbourPos,
                                              @NotNull BlockState neighbourState, @NotNull RandomSource random) {
        return !state.canSurvive(level, pos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos,
                                                   @NotNull BlockState state, boolean includeData) {
        return new ItemStack(Items.STICK);
    }

    @Override
    protected @NotNull InteractionResult useItemOn(@NotNull ItemStack itemStack, @NotNull BlockState state,
                                                   @NotNull Level level, @NotNull BlockPos pos,
                                                   @NotNull Player player, @NotNull InteractionHand hand,
                                                   @NotNull BlockHitResult hitResult) {
        if (itemStack.is(Items.STICK)) {
            return tryStackStake(itemStack, level, pos, player);
        }
        if (itemStack.is(MythosMortalsItems.GRAPES.get())) {
            return tryPlantVine(itemStack, level, pos, player);
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private InteractionResult tryPlantVine(ItemStack itemStack, Level level, BlockPos pos, Player player) {
        BlockPos bottom = findColumnBottom(level, pos);
        if (bottom == null || columnHeight(level, bottom) != VINE_STAKES) {
            return InteractionResult.PASS;
        }
        if (!level.getBlockState(bottom.below()).is(Blocks.FARMLAND)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockState vine = MythosMortalsRegistry.GRAPE_VINE.get().defaultBlockState();

        level.setBlock(bottom.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        DoublePlantBlock.placeAt(level, vine, bottom, Block.UPDATE_ALL);

        itemStack.consume(1, player);
        announcePlacement(level, bottom, level.getBlockState(bottom), player);
        return InteractionResult.SUCCESS;
    }

    private @Nullable BlockPos findColumnBottom(Level level, BlockPos pos) {
        BlockPos bottom = pos;
        for (int step = 0; step < MAX_COLUMN_SCAN; step++) {
            if (!level.getBlockState(bottom.below()).is(this)) {
                return bottom;
            }
            bottom = bottom.below();
        }
        return null;
    }

    private int columnHeight(Level level, BlockPos bottom) {
        int height = 0;
        BlockPos cursor = bottom;
        while (height <= VINE_STAKES && level.getBlockState(cursor).is(this)) {
            height++;
            cursor = cursor.above();
        }
        return height;
    }

    private InteractionResult tryStackStake(ItemStack itemStack, Level level, BlockPos pos, Player player) {
        BlockPos above = pos.above();
        if (!level.isInsideBuildHeight(above) || !level.getBlockState(above).canBeReplaced()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockState placed = this.defaultBlockState();
        level.setBlock(above, placed, Block.UPDATE_ALL);
        itemStack.consume(1, player);
        announcePlacement(level, above, placed, player);
        return InteractionResult.SUCCESS;
    }


    static void announcePlacement(Level level, BlockPos pos, BlockState placed, @Nullable Player player) {
        SoundType sound = placed.getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
            (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, placed));
    }
}
