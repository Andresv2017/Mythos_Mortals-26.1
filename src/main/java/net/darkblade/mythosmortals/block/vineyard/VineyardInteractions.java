package net.darkblade.mythosmortals.block.vineyard;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import net.darkblade.mythosmortals.core.MythosMortals;
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
