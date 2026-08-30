package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.mythosmortals.item.spear.DoriSpearItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;
import java.util.Optional;

public final class MythosMortalsItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MythosMortals.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MythosMortals.MODID);

    private static final TagKey<Item> BRONZE_INGOTS =
        ItemTags.create(Identifier.fromNamespaceAndPath("c", "ingots/bronze"));

    public static final DeferredItem<Item> ATHENIAN_HELMET =
        ITEMS.registerSimpleItem("athenian_helmet", () -> helmetProperties("athenian_helmet", 3.0, 1.0));
    public static final DeferredItem<Item> SPARTAN_HELMET =
        ITEMS.registerSimpleItem("spartan_helmet", () -> helmetProperties("spartan_helmet", 3.0, 2.0));

    public static final DeferredItem<ShieldItem> ATHENIAN_SHIELD =
        ITEMS.registerItem("athenian_shield", ShieldItem::new, MythosMortalsItems::shieldProperties);
    public static final DeferredItem<ShieldItem> SPARTAN_SHIELD =
        ITEMS.registerItem("spartan_shield", ShieldItem::new, MythosMortalsItems::shieldProperties);

    private static Item.Properties shieldProperties() {
        return new Item.Properties()
            .stacksTo(1)
            .durability(336)
            .enchantable(9)
            .repairable(BRONZE_INGOTS)
            .component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
                0.0F,
                1.0F,
                List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
                BlocksAttacks.ItemDamageFunction.DEFAULT,
                Optional.empty(),
                Optional.of(SoundEvents.SHIELD_BLOCK),
                Optional.of(SoundEvents.SHIELD_BREAK)));
    }

    public static final DeferredItem<DoriSpearItem> DORI_SPEAR =
        ITEMS.registerItem("dori_spear", DoriSpearItem::new,
            () -> new Item.Properties()
                .stacksTo(1)
                .durability(250)
                .repairable(BRONZE_INGOTS)
                .component(DataComponents.WEAPON, new Weapon(1))
                .attributes(DoriSpearItem.createAttributes()));

    public static final DeferredItem<Item> XIFOS_SWORD =
        ITEMS.registerSimpleItem("xifos_sword", () -> new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.4F));

    public static final DeferredItem<Item> GREEK_BRONZE_CORE =
        ITEMS.registerSimpleItem("greek_bronze_core", Item.Properties::new);

    public static final DeferredItem<Item> ATHENA_OCULAR_UPGRADE =
        ITEMS.registerSimpleItem("athena_ocular_upgrade", () -> new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> SONIC_SCREECH_UPGRADE =
        ITEMS.registerSimpleItem("sonic_screech_upgrade", () -> new Item.Properties().stacksTo(1));


    public static final DeferredItem<Item> BRONZE_INGOT =
        ITEMS.registerSimpleItem("bronze_ingot", Item.Properties::new);
    public static final DeferredItem<Item> RAW_TIN =
        ITEMS.registerSimpleItem("raw_tin", Item.Properties::new);
    public static final DeferredItem<Item> TIN_INGOT =
        ITEMS.registerSimpleItem("tin_ingot", Item.Properties::new);

    public static final FoodProperties GRAPES_FOOD = new FoodProperties(2, 1.2F, false);

    public static final FoodProperties OLIVES_FOOD = new FoodProperties(2, 2.0F, false);

    public static final DeferredItem<Item> GRAPES =
        ITEMS.registerSimpleItem("grapes", () -> new Item.Properties().food(GRAPES_FOOD));
    public static final DeferredItem<Item> OLIVES =
        ITEMS.registerSimpleItem("olives", () -> new Item.Properties().food(OLIVES_FOOD));

    public static final FoodProperties WINE_FOOD = new FoodProperties(3, 1.8F, true);

    public static final DeferredItem<Item> OLIVE_OIL_BOTTLE =
        ITEMS.registerSimpleItem("olive_oil_bottle", () -> new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> WINE_BOTTLE =
        ITEMS.registerSimpleItem("wine_bottle", () -> new Item.Properties()
            .stacksTo(1)
            .food(WINE_FOOD, Consumables.DEFAULT_DRINK)
            .usingConvertsTo(Items.GLASS_BOTTLE));

    public static final DeferredItem<Item> ATHENA_BRIDLE =
        ITEMS.registerSimpleItem("athena_bridle", () -> new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.RARE));

    public static final DeferredItem<Item> PEGASUS_SADDLE =
        ITEMS.registerSimpleItem("pegasus_saddle", () -> new Item.Properties()
            .stacksTo(1)
            .component(DataComponents.EQUIPPABLE, pegasusSaddleEquippable()));

    private static net.minecraft.world.item.equipment.Equippable pegasusSaddleEquippable() {
        return net.minecraft.world.item.equipment.Equippable.builder(EquipmentSlot.SADDLE)
            .setEquipSound(SoundEvents.HORSE_SADDLE)
            .setDispensable(false)
            .setDamageOnHurt(false)
            .build();
    }

    public static final DeferredItem<Item> ARPY_FEATHER =
        ITEMS.registerSimpleItem("arpy_feather", Item.Properties::new);

    public static final DeferredItem<Item> ATHENA_BANNER_PATTERN =
        bannerPattern("athena_banner_pattern", MythosMortalsBannerPatterns.PATTERN_ITEM_ATHENA);
    public static final DeferredItem<Item> CENTAUR_BANNER_PATTERN =
        bannerPattern("centaur_banner_pattern", MythosMortalsBannerPatterns.PATTERN_ITEM_CENTAUR);
    public static final DeferredItem<Item> SPARTA_BANNER_PATTERN =
        bannerPattern("sparta_banner_pattern", MythosMortalsBannerPatterns.PATTERN_ITEM_SPARTA);

    private static DeferredItem<Item> bannerPattern(String id, TagKey<BannerPattern> patternTag) {
        return ITEMS.registerSimpleItem(id, () -> new Item.Properties()
            .stacksTo(1)
            .delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS,
                context -> context.getOrThrow(patternTag)));
    }

    private static Item.Properties helmetProperties(String id, double armor, double toughness) {
        return new Item.Properties()
            .equippable(EquipmentSlot.HEAD)
            .durability(200)
            .enchantable(10)
            .repairable(BRONZE_INGOTS)
            .attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR,
                    new AttributeModifier(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "armor." + id),
                        armor, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HEAD)
                .add(Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "armor_toughness." + id),
                        toughness, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HEAD)
                .build());
    }

    // --- Spawn eggs ----------------------------------------------------------------------
    public static final DeferredItem<SpawnEggItem> ARPY_SPAWN_EGG =
        spawnEgg("arpy", MythosMortalsEntities.ARPY);
    public static final DeferredItem<SpawnEggItem> ATHENIAN_SPAWN_EGG =
        spawnEgg("athenian", MythosMortalsEntities.ATHENIAN);
    public static final DeferredItem<SpawnEggItem> MINOTAUR_SPAWN_EGG =
        spawnEgg("minotaur", MythosMortalsEntities.MINOTAUR);
    public static final DeferredItem<SpawnEggItem> OWL_SPAWN_EGG =
        spawnEgg("owl", MythosMortalsEntities.OWL);
    public static final DeferredItem<SpawnEggItem> PEGASUS_SPAWN_EGG =
        spawnEgg("pegasus", MythosMortalsEntities.PEGASUS);
    public static final DeferredItem<SpawnEggItem> SPARTAN_SPAWN_EGG =
        spawnEgg("spartan", MythosMortalsEntities.SPARTAN);

    private static DeferredItem<SpawnEggItem> spawnEgg(String mob, Supplier<? extends EntityType<?>> type) {
        return ITEMS.registerItem(mob + "_spawn_egg", SpawnEggItem::new,
            () -> new Item.Properties().spawnEgg(type.get()));
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARMORY_TAB =
        CREATIVE_TABS.register("deluxe_armory", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mythosmortals.deluxe_armory"))
            .icon(() -> new ItemStack(ATHENIAN_HELMET.get()))
            .displayItems((params, output) -> {
                output.accept(ATHENIAN_HELMET.get());
                output.accept(SPARTAN_HELMET.get());
                output.accept(ATHENIAN_SHIELD.get());
                output.accept(SPARTAN_SHIELD.get());
                output.accept(DORI_SPEAR.get());
                output.accept(XIFOS_SWORD.get());
                output.accept(GREEK_BRONZE_CORE.get());
                output.accept(ATHENA_OCULAR_UPGRADE.get());
                output.accept(SONIC_SCREECH_UPGRADE.get());
                output.accept(BRONZE_INGOT.get());
                output.accept(RAW_TIN.get());
                output.accept(TIN_INGOT.get());
                output.accept(GRAPES.get());
                output.accept(OLIVES.get());
                output.accept(OLIVE_OIL_BOTTLE.get());
                output.accept(WINE_BOTTLE.get());
                output.accept(ATHENA_BRIDLE.get());
                output.accept(PEGASUS_SADDLE.get());
                output.accept(ARPY_FEATHER.get());
                output.accept(ATHENA_BANNER_PATTERN.get());
                output.accept(CENTAUR_BANNER_PATTERN.get());
                output.accept(SPARTA_BANNER_PATTERN.get());
                output.accept(ARPY_SPAWN_EGG.get());
                output.accept(ATHENIAN_SPAWN_EGG.get());
                output.accept(MINOTAUR_SPAWN_EGG.get());
                output.accept(OWL_SPAWN_EGG.get());
                output.accept(PEGASUS_SPAWN_EGG.get());
                output.accept(SPARTAN_SPAWN_EGG.get());
            })
            .build());

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        CREATIVE_TABS.register(bus);
    }

    private MythosMortalsItems() {}
}
