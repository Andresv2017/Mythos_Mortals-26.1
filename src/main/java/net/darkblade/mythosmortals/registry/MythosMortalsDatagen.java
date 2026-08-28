package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.deluxelib.datagen.DeluxeEntityLootProvider;
import net.darkblade.deluxelib.datagen.DeluxeEntityLootSubProvider;
import net.darkblade.deluxelib.datagen.DeluxeLangProvider;
import net.darkblade.deluxelib.spawn.DeluxeBiomeSpawnProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

public final class MythosMortalsDatagen {

    private MythosMortalsDatagen() {
        ;;
    }

    public static void gatherClientData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        generator.addProvider(true, new Lang(output));
    }

    public static void gatherServerData(GatherDataEvent.Server event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> registries = event.getLookupProvider();
        generator.addProvider(true, new BiomeSpawns(output));
        generator.addProvider(true, new EntityLoot(output, registries));
    }

    private static final class Lang extends DeluxeLangProvider {
        Lang(PackOutput output) {
            super(output, MythosMortals.MODID, "en_us");
        }

        @Override
        protected void addTranslations() {
            add(MythosMortalsEntities.ATHENIAN.get(), "Athenian Warrior");

            add("itemGroup.mythosmortals.deluxe_armory", "Deluxe Armory");
            add(MythosMortalsItems.ATHENA_OCULAR_UPGRADE.get(), "Athena's Ocular");
            add(MythosMortalsItems.SONIC_SCREECH_UPGRADE.get(), "Sonic Screech");
            add(MythosMortalsItems.GREEK_BRONZE_CORE.get(), "Bronze Core");
            add(MythosMortalsItems.WINE_BOTTLE.get(), "Greek Wine");
            add(MythosMortalsItems.OLIVE_OIL_BOTTLE.get(), "Olive Oil");

            add(MythosMortalsItems.ATHENA_BANNER_PATTERN.get(), "Owl of Athena Banner Pattern");
            add(MythosMortalsItems.CENTAUR_BANNER_PATTERN.get(), "Centaur Banner Pattern");
            add(MythosMortalsItems.SPARTA_BANNER_PATTERN.get(), "Lambda Banner Pattern");

            add(MythosMortalsBlocks.GREEK_AMPHORA.get(), "Greek Amphora");
            add(MythosMortalsBlocks.GREEK_AMPHORA_GRAPES.get(), "Greek Amphora of Grapes");
            add(MythosMortalsBlocks.GREEK_AMPHORA_OLIVES.get(), "Greek Amphora of Olives");
            add(MythosMortalsBlocks.GREEK_AMPHORA_WINE.get(), "Greek Amphora of Wine");
            add(MythosMortalsBlocks.GREEK_AMPHORA_OLIVE_OIL.get(), "Greek Amphora of Olive Oil");

            add(MythosMortalsBlocks.OLIVE_CHEST_BOAT_ITEM.get(), "Olive Boat with Chest");

            autoItemNames(MythosMortalsItems.ITEMS);

            autoBlockNames(MythosMortalsBlocks.BLOCKS);

            add(MythosMortalsEntities.OLIVE_CHEST_BOAT.get(), "Olive Boat with Chest");
            autoEntityNames(MythosMortalsEntities.ENTITY_TYPES);

            add("effect.mythosmortals.boreal_courage", "Boreal Courage");

            add("tooltip.mythosmortals.marinated", "Marinated in olive oil");
            add("tooltip.mythosmortals.marinated.effect", "+2 hunger, +50% saturation");

            add("key.mythosmortals.athena_sight", "Toggle Athena's Sight");

            add("advancement.mythosmortals.athena_sight.title", "Athena's Sight");
            add("advancement.mythosmortals.athena_sight.description", "Grant the Bronze Owl the sight of Athena");

            add("mythosmortals.configuration.title", "Mythos & Mortals Configs");
            add("mythosmortals.configuration.section.mythosmortals.common.toml", "Mythos & Mortals Configs");
            add("mythosmortals.configuration.section.mythosmortals.common.toml.title", "Mythos & Mortals Configs");
            add("mythosmortals.configuration.items", "Item List");
            add("mythosmortals.configuration.logDirtBlock", "Log Dirt Block");
            add("mythosmortals.configuration.magicNumberIntroduction", "Magic Number Text");
            add("mythosmortals.configuration.magicNumber", "Magic Number");

            add("key.mythosmortals.pegasus_dash", "Wind Surge (Pegasus)");
            add("pegasus.mythosmortals.not_your_mount", "This pegasus answers to someone else.");

            bannerPatternNames("athena", "Owl of Athena");
            bannerPatternNames("centaur", "Centaur");
            bannerPatternNames("sparta", "Lambda");

            add("subtitles.mythosmortals.entity.arpy.attack", "Harpy screeches");
            add("subtitles.mythosmortals.entity.arpy.death", "Harpy dies");
            add("subtitles.mythosmortals.entity.arpy.fly", "Harpy flaps");
            add("subtitles.mythosmortals.entity.arpy.landing", "Harpy lands");
            add("subtitles.mythosmortals.entity.soldier.attack", "Soldier attacks");
            add("subtitles.mythosmortals.entity.soldier.block", "Soldier raises shield");
            add("subtitles.mythosmortals.entity.soldier.death", "Soldier dies");
            add("subtitles.mythosmortals.entity.soldier.poise_break", "Soldier's guard breaks");
            add("subtitles.mythosmortals.entity.soldier.step", "Footsteps");
            add("subtitles.mythosmortals.entity.arpy.ambient", "Harpy calls");
            add("subtitles.mythosmortals.entity.arpy.hurt", "Harpy hurts");
            add("subtitles.mythosmortals.entity.arpy.step", "Talons scrape");
            add("subtitles.mythosmortals.entity.arpy.dive_return", "Harpy pulls up");
            add("subtitles.mythosmortals.entity.soldier.ambient", "Soldier shifts");
            add("subtitles.mythosmortals.entity.soldier.hurt", "Soldier hurts");
            add("subtitles.mythosmortals.entity.soldier.shield_up", "Soldier raises shield");
            add("subtitles.mythosmortals.entity.pegasus.ambient", "Pegasus whinnies");
            add("subtitles.mythosmortals.entity.pegasus.wing_flap", "Pegasus flaps");
            add("subtitles.mythosmortals.entity.pegasus.take_off", "Pegasus takes off");
            add("subtitles.mythosmortals.entity.pegasus.landing", "Pegasus lands");
            add("subtitles.mythosmortals.entity.pegasus.dash", "Pegasus surges");
        }

        private void bannerPatternNames(String id, String name) {
            for (DyeColor color : DyeColor.values()) {
                add("block." + MythosMortals.MODID + ".banner." + id + "." + color.getName(),
                    humanize(color.getName()) + " " + name);
            }
        }

        private static String humanize(String path) {
            StringBuilder result = new StringBuilder(path.length());
            for (String word : path.split("_")) {
                if (result.length() > 0) {
                    result.append(' ');
                }
                result.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
            }
            return result.toString();
        }
    }

    private static final class BiomeSpawns extends DeluxeBiomeSpawnProvider {
        BiomeSpawns(PackOutput output) {
            super(output, MythosMortals.MODID);
        }
    }

    private static final class EntityLoot extends DeluxeEntityLootProvider {
        EntityLoot(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, Loot::new);
        }
    }

    private static final class Loot extends DeluxeEntityLootSubProvider {
        Loot(HolderLookup.Provider registries) {
            super(registries, MythosMortalsEntities.ENTITY_TYPES);
        }

        @Override
        protected void addLootTables() {
            add(MythosMortalsEntities.ATHENIAN.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(Items.BONE)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                    .when(LootItemRandomChanceCondition.randomChance(0.5f)))
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(MythosMortalsItems.DORI_SPEAR.get())
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.15f, 0.5f))))
                    .add(LootItem.lootTableItem(MythosMortalsItems.ATHENIAN_HELMET.get())
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.15f, 0.5f))))
                    .when(LootItemRandomChanceCondition.randomChance(0.25f))));

            add(MythosMortalsEntities.SPARTAN.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(MythosMortalsItems.XIFOS_SWORD.get())
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.15f, 0.5f))))
                    .add(LootItem.lootTableItem(MythosMortalsItems.SPARTAN_HELMET.get())
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.15f, 0.5f))))
                    .when(LootItemRandomChanceCondition.randomChance(0.25f))));

            add(MythosMortalsEntities.ARPY.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(MythosMortalsItems.ARPY_FEATHER.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                    .when(LootItemRandomChanceCondition.randomChance(0.6f))));

            add(MythosMortalsEntities.MINOTAUR.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(MythosMortalsItems.GREEK_BRONZE_CORE.get()))
                    .add(LootItem.lootTableItem(MythosMortalsItems.ATHENA_BRIDLE.get()))));
        }
    }

}
