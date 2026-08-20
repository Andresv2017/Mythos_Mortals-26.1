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

/**
 * Decorative "Owl Statue" block. Right-clicking it while holding the Minotaur's
 * {@link MythosMortalsItems#GREEK_BRONZE_CORE} removes the block, spawns the living
 * {@link OwlEntity} companion in its place, and bonds it to the interacting player as its owner
 * (see {@link OwlEntity#bondTo}) — the statue block → companion entity transformation.
 *
 * <p>Extends {@link StatueBlock}, so it only contributes this wake-up behaviour — the base class
 * already covers the state definition, placement facing, block entity, and invisible render
 * shape. A dirt-textured placeholder cube was tried instead, but it just hid/enclosed the owl
 * model rather than reading as ground the owl stands on, so this went back to pure invisibility.
 */
public class OwlStatueBlock extends StatueBlock {
    /** Clave server-safe de esta estatua. Vive aquí (no en {@code StatueConfig}) porque el block
     * entity la necesita en el servidor dedicado. */
    public static final StatueType OWL_TYPE =
            new StatueType(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "owl"));

    public OwlStatueBlock(BlockBehaviour.Properties properties) {
        // El DeferredHolder ya es un Supplier diferido, así que se pasa tal cual: nada se resuelve
        // hasta que newBlockEntity lo invoca, mucho después de este constructor (que corre durante el
        // register event de bloques, cuando el block entity type todavía no está bound).
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

        // Carry the statue's own facing into the spawned owl's initial rotation, so waking it doesn't
        // snap its facing to the default (south) — it keeps looking the way the statue already did.
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        level.removeBlock(pos, false);

        OwlEntity owl = new OwlEntity(MythosMortalsRegistry.OWL.get(), level);
        owl.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        // setYRot alone isn't enough — rendering uses yBodyRot/yHeadRot (separately tracked/
        // interpolated), which a fresh entity doesn't derive from yRot on its own. Same three-field
        // sync OwlEntity's own PossessionGoal/PerchGoal already use for exactly this reason.
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

    /**
     * The copper burst that sells the statue cracking open into a living bird.
     *
     * <p>Copper block shards rather than a coloured dust: they are literally the texture the statue is
     * made of, so the colour can never drift from whatever the model ends up using, and they read as
     * material breaking apart instead of as a generic magic puff. Electric sparks on top because in
     * vanilla that particle already belongs to copper (it is what lightning rods throw), so it reads
     * as "the metal woke up" without inventing a new visual language.
     *
     * <p>Both use the default 32-block particle range — you are standing at arm's length from the
     * block you just used, so the longer-distance flag would only be spending packets.
     */
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
