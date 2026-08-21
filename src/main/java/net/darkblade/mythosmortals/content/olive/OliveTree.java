package net.darkblade.mythosmortals.content.olive;

import net.darkblade.mythosmortals.MythosMortals;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Optional;

public final class OliveTree {

    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED =
        ResourceKey.create(Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "olive"));

    public static final TreeGrower GROWER =
        new TreeGrower(MythosMortals.MODID + ":olive", Optional.empty(), Optional.of(CONFIGURED), Optional.empty());

    private OliveTree() {}
}
