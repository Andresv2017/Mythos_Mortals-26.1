package net.darkblade.mythosmortals.block.vineyard;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.NotNull;


public class GrapeVineBlock extends DoublePlantBlock implements BonemealableBlock {
    public static final MapCodec<GrapeVineBlock> CODEC = simpleCodec(GrapeVineBlock::new);

    public static final IntegerProperty AGE = BlockStateProperties.AGE_2;

    public static final int MAX_AGE = 2;

    private static final int MIN_GROW_LIGHT = 9;

    private static final int MIN_YIELD = 2;
    private static final int MAX_YIELD = 3;

    public GrapeVineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HALF, DoubleBlockHalf.LOWER)
            .setValue(AGE, 0));
    }

    @Override
    public @NotNull MapCodec<? extends GrapeVineBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AGE);
    }

    @Override
    protected boolean mayPlaceOn(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return state.is(GrapeStakeBlock.STAKE_PLACEABLE);
    }

    @Override
    protected @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos,
                                                   @NotNull BlockState state, boolean includeData) {
        return new ItemStack(MythosMortalsItems.GRAPES.get());
    }

    public int getAge(BlockState state) {
        return state.getValue(AGE);
    }

    @Override
    protected boolean isRandomlyTicking(@NotNull BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && getAge(state) < MAX_AGE;
    }

    @Override
    protected void randomTick(@NotNull BlockState state, @NotNull ServerLevel level,
                              @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        if (level.getRawBrightness(pos, 0) < MIN_GROW_LIGHT) {
            return;
        }
        int age = getAge(state);
        if (age >= MAX_AGE) {
            return;
        }
        float speed = growthSpeed(level, pos);
        boolean rolled = random.nextInt((int) (25.0F / speed) + 1) == 0;
        if (CommonHooks.canCropGrow(level, pos, state, rolled)) {
            setAge(level, pos, state, age + 1);
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    private float growthSpeed(BlockGetter level, BlockPos pos) {
        float speed = 1.0F;
        BlockPos below = pos.below();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos soilPos = below.offset(dx, 0, dz);
                BlockState soil = level.getBlockState(soilPos);
                float soilSpeed = 0.0F;

                TriState decision = soil.canSustainPlant(level, soilPos, Direction.UP, soil);
                if (decision.isDefault() ? soil.is(BlockTags.GROWS_CROPS) : decision.isTrue()) {
                    soilSpeed = soil.isFertile(level, soilPos) ? 3.0F : 1.0F;
                }
                if (dx != 0 || dz != 0) {
                    soilSpeed /= 4.0F;
                }
                speed += soilSpeed;
            }
        }

        boolean alongX = isSameVine(level, pos.west()) || isSameVine(level, pos.east());
        boolean alongZ = isSameVine(level, pos.north()) || isSameVine(level, pos.south());
        if (alongX && alongZ) {
            return speed / 2.0F;
        }
        boolean diagonal = isSameVine(level, pos.west().north())
            || isSameVine(level, pos.east().north())
            || isSameVine(level, pos.east().south())
            || isSameVine(level, pos.west().south());
        return diagonal ? speed / 2.0F : speed;
    }

    private boolean isSameVine(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).is(this);
    }

    @Override
    public boolean isValidBonemealTarget(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return getAge(state) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(@NotNull Level level, @NotNull RandomSource random,
                                     @NotNull BlockPos pos, @NotNull BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(@NotNull ServerLevel level, @NotNull RandomSource random,
                                @NotNull BlockPos pos, @NotNull BlockState state) {
        setAge(level, pos, state, Math.min(MAX_AGE, getAge(state) + 1));
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level,
                                                        @NotNull BlockPos pos, @NotNull Player player,
                                                        @NotNull BlockHitResult hitResult) {
        if (getAge(state) < MAX_AGE) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (level instanceof ServerLevel serverLevel) {
            BlockPos lowerPos = lowerPosOf(pos, state);
            int yield = MIN_YIELD + serverLevel.getRandom().nextInt(MAX_YIELD - MIN_YIELD + 1);
            Block.popResource(serverLevel, lowerPos, new ItemStack(MythosMortalsItems.GRAPES.get(), yield));
            serverLevel.playSound(null, lowerPos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                SoundSource.BLOCKS, 1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
            setAge(serverLevel, pos, state, 0);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, lowerPos,
                GameEvent.Context.of(player, serverLevel.getBlockState(lowerPos)));
        }
        return InteractionResult.SUCCESS;
    }

    public void setAge(Level level, BlockPos pos, BlockState state, int age) {
        BlockPos lowerPos = lowerPosOf(pos, state);
        BlockState lower = level.getBlockState(lowerPos);
        BlockState upper = level.getBlockState(lowerPos.above());
        if (!lower.is(this) || !upper.is(this)) {
            return;
        }
        level.setBlock(lowerPos, lower.setValue(AGE, age), Block.UPDATE_CLIENTS);
        level.setBlock(lowerPos.above(), upper.setValue(AGE, age), Block.UPDATE_CLIENTS);
    }

    public static BlockPos lowerPosOf(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }
}
