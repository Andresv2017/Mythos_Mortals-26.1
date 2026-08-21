package net.darkblade.mythosmortals.content.olive;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;

public class OliveLeavesBlock extends LeavesBlock {
    public static final MapCodec<OliveLeavesBlock> CODEC = simpleCodec(OliveLeavesBlock::new);

    public OliveLeavesBlock(BlockBehaviour.Properties properties) {
        super(0.0F, properties);
    }


    @Override
    public @NotNull MapCodec<? extends OliveLeavesBlock> codec() {
        return CODEC;
    }

    @Override
    protected void spawnFallingLeavesParticle(@NotNull Level level, @NotNull BlockPos pos,
                                              @NotNull RandomSource random) {
    }
}
