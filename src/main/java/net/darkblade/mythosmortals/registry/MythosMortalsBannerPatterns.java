package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.MythosMortals;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BannerPattern;

public final class MythosMortalsBannerPatterns {

    public static final TagKey<BannerPattern> PATTERN_ITEM_ATHENA = create("athena");
    public static final TagKey<BannerPattern> PATTERN_ITEM_CENTAUR = create("centaur");
    public static final TagKey<BannerPattern> PATTERN_ITEM_SPARTA = create("sparta");

    private static TagKey<BannerPattern> create(String name) {
        return TagKey.create(Registries.BANNER_PATTERN,
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "pattern_item/" + name));
    }

    private MythosMortalsBannerPatterns() {}
}
