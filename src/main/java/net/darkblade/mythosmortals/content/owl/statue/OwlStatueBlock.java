package net.darkblade.mythosmortals.content.owl.statue;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.deluxelib.block.StatueBlock;
import net.darkblade.deluxelib.block.StatueType;
import net.darkblade.mythosmortals.content.owl.OwlEntity;
import net.darkblade.deluxelib.vfx.ParticleFx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;


public class OwlStatueBlock extends StatueBlock {

    public static final StatueType OWL_TYPE =
            new StatueType(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "owl"));

    public OwlStatueBlock(BlockBehaviour.Properties properties) {
        super(properties, MythosMortalsRegistry.OWL_STATUE_BLOCK_ENTITY);
    }

    @Override
    protected @NotNull InteractionResult useItemOn(@NotNull ItemStack itemStack, @NotNull BlockState state,
                                                    @NotNull Level level, @NotNull BlockPos pos,
                                                    @NotNull Player player, @NotNull InteractionHand hand,
                                                    @NotNull BlockHitResult hitResult) {
        if (!itemStack.is(MythosMortalsItems.GREEK_BRONZE_CORE.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        level.removeBlock(pos, false);

        OwlEntity owl = new OwlEntity(MythosMortalsRegistry.OWL.get(), level);
        owl.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        float yaw = facing.toYRot();
        owl.setYRot(yaw);
        owl.yBodyRot = yaw;
        owl.yHeadRot = yaw;
        owl.bondTo(serverPlayer);
        level.addFreshEntity(owl);
        spawnAwakeningParticles(level, pos);

        if (!player.hasInfiniteMaterials()) {
            itemStack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private static void spawnAwakeningParticles(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 centre = Vec3.atCenterOf(pos);
        ParticleFx.burst(server, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COPPER_BLOCK.defaultBlockState()),
                centre, SHARD_COUNT, SHARD_SPREAD, SHARD_SPEED);
        ParticleFx.burst(server, ParticleTypes.ELECTRIC_SPARK, centre, SPARK_COUNT, SPARK_SPREAD, SPARK_SPEED);
    }

    private static final int SHARD_COUNT = 40;
    private static final double SHARD_SPREAD = 0.4;
    private static final double SHARD_SPEED = 0.12;
    private static final int SPARK_COUNT = 18;
    private static final double SPARK_SPREAD = 0.35;
    private static final double SPARK_SPEED = 0.05;
}
