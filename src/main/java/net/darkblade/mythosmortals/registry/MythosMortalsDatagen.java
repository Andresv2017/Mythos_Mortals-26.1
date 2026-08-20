package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.MythosMortals;
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
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Datagen providers for Mythos & Mortals's content — lang auto-naming, biome spawns and per-entity
 * loot tables — built on DeluxeLib's datagen helpers, wired to the mobs in {@link MythosMortalsRegistry}.
 */
public final class MythosMortalsDatagen {

    private MythosMortalsDatagen() {
        ;;
    }

    // 26.1: GatherDataEvent is abstract — you must register listeners for its concrete Client/Server
    // subclasses (each fires only for that data-gen run), not the base event.

    /** Wire to the mod bus: {@code modEventBus.addListener(MythosMortalsDatagen::gatherClientData);} */
    public static void gatherClientData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        generator.addProvider(true, new Lang(output));
    }

    /** Wire to the mod bus: {@code modEventBus.addListener(MythosMortalsDatagen::gatherServerData);} */
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
            // Manual override example: everything else falls back to auto-naming below.
            add(MythosMortalsRegistry.ATHENIAN.get(), "Athenian Warrior");

            // Creative tab title + item display names (athenian_helmet -> "Athenian Helmet").
            add("itemGroup.mythosmortals.deluxe_armory", "Deluxe Armory");
            // The owl's upgrades read as a progression tier, so they are named for what they are
            // rather than auto-derived from their id. Named before autoItemNames, which skips
            // anything already given a name.
            add(MythosMortalsItems.ATHENA_OCULAR_UPGRADE.get(), "Athena's Ocular");
            add(MythosMortalsItems.SONIC_SCREECH_UPGRADE.get(), "Sonic Screech");
            add(MythosMortalsItems.GREEK_BRONZE_CORE.get(), "Bronze Core");
            // Los dos productos del ánfora: auto-nombrar daría "Wine Bottle" y "Olive Oil Bottle",
            // y lo que se bebe es el vino, no la botella.
            add(MythosMortalsItems.WINE_BOTTLE.get(), "Greek Wine");
            add(MythosMortalsItems.OLIVE_OIL_BOTTLE.get(), "Olive Oil");

            // Los tres ítems de telar. Vanilla parte el nombre en dos claves porque sus patrones son
            // BannerPatternItem: el ítem se llama "Banner Pattern" a secas y appendHoverText agrega
            // "<id>.desc" ("Creeper Charge") como segunda línea gris. Estos son Item pelado + el
            // componente PROVIDES_BANNER_PATTERNS, así que no hay tooltip que rellenar y el nombre
            // completo va en la única clave que se usa.
            //
            // A mano y no por autoItemNames para que el ítem y la capa sobre el estandarte digan lo
            // mismo: el patrón de sparta se llama "Lambda" abajo, no "Sparta".
            add(MythosMortalsItems.ATHENA_BANNER_PATTERN.get(), "Owl of Athena Banner Pattern");
            add(MythosMortalsItems.CENTAUR_BANNER_PATTERN.get(), "Centaur Banner Pattern");
            add(MythosMortalsItems.SPARTA_BANNER_PATTERN.get(), "Lambda Banner Pattern");

            // El ánfora y sus contenidos. A mano porque el auto-nombrado daría "Greek Amphora Wine"
            // en lugar de "Greek Amphora of Wine", y estos cinco nombres se leen juntos en el
            // creativo.
            //
            // Van ANTES de autoItemNames, no entre las dos llamadas automáticas: un BlockItem
            // devuelve la clave de SU BLOQUE en getDescriptionId(), así que autoItemNames y
            // autoBlockNames compiten por la misma entrada y gana quien llegue primero. Puestos
            // después, "Greek Amphora Wine" ya estaría escrito y este add no haría nada.
            add(MythosMortalsRegistry.GREEK_AMPHORA.get(), "Greek Amphora");
            add(MythosMortalsRegistry.GREEK_AMPHORA_GRAPES.get(), "Greek Amphora of Grapes");
            add(MythosMortalsRegistry.GREEK_AMPHORA_OLIVES.get(), "Greek Amphora of Olives");
            add(MythosMortalsRegistry.GREEK_AMPHORA_WINE.get(), "Greek Amphora of Wine");
            add(MythosMortalsRegistry.GREEK_AMPHORA_OLIVE_OIL.get(), "Greek Amphora of Olive Oil");

            // La barca con cofre: auto-nombrar daría "Olive Chest Boat", y vanilla la lee al revés
            // ("Oak Boat with Chest"). El nombre va tanto al ítem como a la entidad porque son dos
            // claves distintas y las dos salen en pantalla — la de la entidad, al apuntarla.
            add(MythosMortalsRegistry.OLIVE_CHEST_BOAT_ITEM.get(), "Olive Boat with Chest");

            autoItemNames(MythosMortalsItems.ITEMS);

            // Los bloques del viñedo no tienen BlockItem, así que autoItemNames no los alcanza y se
            // quedarían sin nombre en pantalla. autoBlockNames cubre todo el registro de bloques;
            // para los que sí tienen ítem simplemente genera la clave block.* además de la item.*,
            // que es lo correcto de todas formas.
            autoBlockNames(MythosMortalsRegistry.BLOCKS);

            add(MythosMortalsRegistry.OLIVE_CHEST_BOAT.get(), "Olive Boat with Chest");
            autoEntityNames(MythosMortalsRegistry.ENTITY_TYPES);

            // Efectos: la clave se escribe a mano porque DeluxeLangProvider sólo auto-nombra
            // items/bloques/entidades.
            add("effect.mythosmortals.boreal_courage", "Boreal Courage");

            // Marca de la comida marinada — ver MarinatedFoodEvents#onTooltip.
            add("tooltip.mythosmortals.marinated", "Marinated in olive oil");
            add("tooltip.mythosmortals.marinated.effect", "+2 hunger, +50% saturation");

            // Keybinds (Athena's Sight owl possession).
            add("key.mythosmortals.athena_sight", "Toggle Athena's Sight");

            // Advancements.
            add("advancement.mythosmortals.athena_sight.title", "Athena's Sight");
            add("advancement.mythosmortals.athena_sight.description", "Grant the Bronze Owl the sight of Athena");

            // Config screen (from the MDK scaffold's Config.java) — hand-authored, not derivable
            // from a registry, so it lives here rather than in src/main/resources to avoid a
            // duplicate assets/mythosmortals/lang/en_us.json between generated and hand-authored.
            add("mythosmortals.configuration.title", "Mythos & Mortals Configs");
            add("mythosmortals.configuration.section.mythosmortals.common.toml", "Mythos & Mortals Configs");
            add("mythosmortals.configuration.section.mythosmortals.common.toml.title", "Mythos & Mortals Configs");
            add("mythosmortals.configuration.items", "Item List");
            add("mythosmortals.configuration.logDirtBlock", "Log Dirt Block");
            add("mythosmortals.configuration.magicNumberIntroduction", "Magic Number Text");
            add("mythosmortals.configuration.magicNumber", "Magic Number");

            // Patrones de estandarte. No hay ítem ni bloque que auto-nombrar: BannerPatternLayers
            // arma la clave del tooltip como "<translation_key>.<color>", así que cada patrón
            // necesita una entrada por cada tinte (3 x 16 = 48). Vanilla las escribe todas a mano
            // ("block.minecraft.banner.mojang.red" = "Red Thing"); acá salen del bucle.
            //
            // Los nombres de los ítems (athena_banner_pattern -> "Athena Banner Pattern") sí los
            // cubre autoItemNames más arriba: esto es sólo la línea del patrón sobre el estandarte.
            bannerPatternNames("athena", "Owl of Athena");
            bannerPatternNames("centaur", "Centaur");
            bannerPatternNames("sparta", "Lambda");
        }

        /** Una entrada por tinte para {@code block.mythosmortals.banner.<id>.<color>}. */
        private void bannerPatternNames(String id, String name) {
            for (DyeColor color : DyeColor.values()) {
                add("block." + MythosMortals.MODID + ".banner." + id + "." + color.getName(),
                    humanize(color.getName()) + " " + name);
            }
        }

        /** {@code light_blue -> "Light Blue"}. Local a propósito: el humanize de DeluxeLangProvider
         * es privado y sólo alcanza registros, no claves sueltas. */
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
            super(registries, MythosMortalsRegistry.ENTITY_TYPES);
        }

        @Override
        protected void addLootTables() {
            // Manual override example: Athenian drops 1-2 bones at 50% chance.
            add(MythosMortalsRegistry.ATHENIAN.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(Items.BONE)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                    .when(LootItemRandomChanceCondition.randomChance(0.5f))));

            // Guaranteed drop that wakes the dormant Bronze Owl companion (see OwlEntity#mobInteract).
            add(MythosMortalsRegistry.MINOTAUR.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(MythosMortalsItems.GREEK_BRONZE_CORE.get()))));

            // Spartan and Arpy fall back to an empty table automatically.
        }
    }

}
