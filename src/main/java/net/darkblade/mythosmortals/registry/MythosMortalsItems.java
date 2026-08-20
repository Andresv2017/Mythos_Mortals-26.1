package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.mythosmortals.content.spear.DoriSpearItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;

/**
 * Mythos & Mortals's items and its creative-mode tab, built on DeluxeLib's item-side library
 * features (see {@link MythosMortalsRegistry} for the mobs using its entity-side ones).
 *
 * <p>Registry ids live under this mod's own namespace ({@code mythosmortals:athenian_helmet},
 * {@code mythosmortals:deluxe_armory}, …) rather than DeluxeLib's — this class registers under
 * {@link MythosMortals#MODID}, not {@code DeluxeLib.ID}.
 *
 * <p>The two helmets are plain {@link Item}s whose in-hand / inventory appearance is driven by the
 * hand-authored 3D models under {@code assets/mythosmortals/models/item/}. They are grouped in a
 * dedicated creative tab ("Deluxe Armory") that uses the Athenian helmet as its icon.</p>
 */
public final class MythosMortalsItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MythosMortals.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MythosMortals.MODID);

    // Equippable to the HEAD slot with NO equipment asset: the humanoid armor layer skips it, so
    // CustomHeadLayer renders the item's own 3D model on the head (like a pumpkin/skull). Armor
    // points are granted through attribute modifiers instead of an ArmorMaterial, so the 3D model
    // is kept while the helmet still protects.
    public static final DeferredItem<Item> ATHENIAN_HELMET =
        ITEMS.registerSimpleItem("athenian_helmet", () -> helmetProperties("athenian_helmet", 3.0, 1.0));
    public static final DeferredItem<Item> SPARTAN_HELMET =
        ITEMS.registerSimpleItem("spartan_helmet", () -> helmetProperties("spartan_helmet", 3.0, 2.0));

    // Shields: registered as vanilla's own ShieldItem (its only job is dye-color naming — no blocking
    // logic there). This matters for first-person rendering: ItemInHandRenderer applies an extra
    // hardcoded "raise defensively" transform to BLOCKS_ATTACKS items EXCEPT ShieldItem instances,
    // which looked wrong on our shield's own geometry (rotated the handle into view). Using ShieldItem
    // skips that override and just uses our own tuned firstperson_righthand/lefthand pose. Blocking
    // itself and the third-person ArmPose.BLOCK are both driven generically by the BLOCKS_ATTACKS
    // component regardless of item class.
    public static final DeferredItem<ShieldItem> ATHENIAN_SHIELD =
        ITEMS.registerItem("athenian_shield", ShieldItem::new, MythosMortalsItems::shieldProperties);
    public static final DeferredItem<ShieldItem> SPARTAN_SHIELD =
        ITEMS.registerItem("spartan_shield", ShieldItem::new, MythosMortalsItems::shieldProperties);

    private static Item.Properties shieldProperties() {
        return new Item.Properties()
            .stacksTo(1)
            .durability(336)
            .enchantable(9)
            .repairable(Items.OAK_PLANKS)
            .component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
                0.0F,
                1.0F,
                List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
                BlocksAttacks.ItemDamageFunction.DEFAULT,
                Optional.empty(),
                Optional.of(SoundEvents.SHIELD_BLOCK),
                Optional.of(SoundEvents.SHIELD_BREAK)));
    }

    // Spear: throwable via DoriSpearItem (charge-and-release like a trident, no riptide/loyalty) —
    // see ThrownDoriSpear for the in-flight entity, and the library's ThrownWeapon/ThrownWeaponItem
    // for the mechanism both of them are worked examples of.
    public static final DeferredItem<DoriSpearItem> DORI_SPEAR =
        ITEMS.registerItem("dori_spear", DoriSpearItem::new,
            () -> new Item.Properties().stacksTo(1).attributes(DoriSpearItem.createAttributes()));

    // Sword: full weapon behaviour via ToolMaterial (no dedicated SwordItem class in 26.1 — it's all
    // components). Iron-tier baseline damage/speed, same numbers as vanilla's iron sword.
    //
    // Properties MUST be built lazily (Supplier, not an eager Item.Properties instance): sword()
    // calls ToolMaterial.applySwordProperties(), which calls
    // BuiltInRegistries.acquireBootstrapRegistrationLookup(BLOCK) — only valid while RegisterEvent
    // is firing. Building it eagerly as a field initializer runs at class-load time (mod
    // construction), well before that window, and crashes with ExceptionInInitializerError.
    public static final DeferredItem<Item> XIFOS_SWORD =
        ITEMS.registerSimpleItem("xifos_sword", () -> new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.4F));

    // Boss drop that activates the Owl Statue block (see OwlStatueBlock#useItemOn), turning it into
    // the living Bronze Owl companion: a guaranteed Minotaur kill drop (DeluxeEntityLootProvider),
    // not craftable.
    public static final DeferredItem<Item> GREEK_BRONZE_CORE =
        ITEMS.registerSimpleItem("greek_bronze_core", Item.Properties::new);

    // The owl's two progression upgrades — applied by right-clicking your bonded owl while holding
    // one (see OwlEntity#mobInteract), consumed on use, permanent. Both stack to 1: they are
    // milestones, not supplies.
    //
    // Note the spyglass is NOT one of these. It used to stand in for the Athena Ocular, but it is now
    // the tool you aim to send the owl at a target (OwlOrderInput), and one item cannot be both the
    // permanent unlock and the everyday tool — so it becomes an ingredient of the Ocular instead.
    /** Unlocks possession: the owl can be flown as a remote scout with the Athena's Sight key.
     * Crafted from a spyglass + 2 copper + an amethyst shard — see
     * {@code data/deluxelib/recipe/athena_ocular_upgrade.json}. The copper and amethyst stand in for
     * bronze and a gem until Módulo 1's bronze chain exists, exactly as the spyglass itself used to
     * stand in for this whole item. */
    public static final DeferredItem<Item> ATHENA_OCULAR_UPGRADE =
        ITEMS.registerSimpleItem("athena_ocular_upgrade", () -> new Item.Properties().stacksTo(1));

    /** Unlocks the sonic screech, both while piloting and in the owl's own defence of its owner.
     * Crafted from an echo shard + 2 copper: the shard is the Warden's own material, and this attack
     * already borrows the Warden's sounds. */
    public static final DeferredItem<Item> SONIC_SCREECH_UPGRADE =
        ITEMS.registerSimpleItem("sonic_screech_upgrade", () -> new Item.Properties().stacksTo(1));

    // Plain material stubs — textures exist, nothing else does yet. Registered so the assets aren't
    // dead weight and the items are visible/testable, but none of them has a recipe, food component,
    // or any other behaviour. BRONZE_INGOT is the one exception: see its use in OwlEntity#mobInteract
    // (heals the bonded owl, no max-health change, blocked once it's at full health).
    public static final DeferredItem<Item> BRONZE_INGOT =
        ITEMS.registerSimpleItem("bronze_ingot", Item.Properties::new);
    public static final DeferredItem<Item> RAW_TIN =
        ITEMS.registerSimpleItem("raw_tin", Item.Properties::new);
    public static final DeferredItem<Item> TIN_INGOT =
        ITEMS.registerSimpleItem("tin_ingot", Item.Properties::new);
    // tin_ore / deepslate_tin_ore ya no son ítems sueltos: son bloques reales con su BlockItem, en
    // MythosMortalsRegistry (TIN_ORE / DEEPSLATE_TIN_ORE). Los ids de registro no cambiaron.
    public static final DeferredItem<Item> GRAPES =
        ITEMS.registerSimpleItem("grapes", Item.Properties::new);
    public static final DeferredItem<Item> OLIVES =
        ITEMS.registerSimpleItem("olives", Item.Properties::new);
    /**
     * Lo que alimenta un trago de vino, venga de la botella o del ánfora que se bebe en la mano
     * (ver {@code MythosMortalsRegistry.GREEK_AMPHORA_WINE_ITEM}). Compartido para que las dos vías no se
     * desincronicen.
     *
     * <p><b>{@code canAlwaysEat = true}</b>, como la miel o la manzana dorada. Sin eso,
     * {@code Consumable#canConsume} delega en {@code player.canEat(false)} y con la barra de hambre
     * llena el trago ni siquiera arranca — que es justo cuando más falta hace, porque el vino se bebe
     * por Valentía Boreal antes de una pelea, no por hambre.
     */
    public static final FoodProperties WINE_FOOD = new FoodProperties(3, 1.8F, true);

    /**
     * Aceite de oliva: <b>no se bebe</b>. Es el ingrediente del marinado (ver
     * {@link net.darkblade.mythosmortals.content.amphora.MarinatingRecipe}), así que no lleva ni comida ni
     * {@code Consumable} — beberse el aceite a morro no es una receta griega.
     *
     * <p>Una botella marina hasta 8 comidas <b>de una tacada</b>: rodéala de pescado en la mesa de
     * crafteo y salen todos marinados. Ocho porque son las casillas que rodean al centro en una
     * rejilla de 3×3, así que el límite se cuenta solo.
     */
    public static final DeferredItem<Item> OLIVE_OIL_BOTTLE =
        ITEMS.registerSimpleItem("olive_oil_bottle", () -> new Item.Properties().stacksTo(1));

    /**
     * Vino griego. Todo lo que es datos va en las propiedades, calcadas de la botella de miel de
     * vanilla; el efecto y su castigo viven en {@link net.darkblade.mythosmortals.content.amphora.WineEvents},
     * porque dependen de cuándo fue el trago anterior y eso no cabe en un componente.
     *
     * <p>{@code stacksTo(1)}: si las botellas se apilaran, el ánfora perdería su gracia — cuatro
     * raciones en un solo hueco de inventario es justo lo que la hace valer la pena.
     *
     * <p>3 de hambre (el "1.5 muslos" del brief) y saturación baja, a propósito: el vino es un buff
     * táctico, no una comida.
     */
    public static final DeferredItem<Item> WINE_BOTTLE =
        ITEMS.registerSimpleItem("wine_bottle", () -> new Item.Properties()
            .stacksTo(1)
            .food(WINE_FOOD, Consumables.DEFAULT_DRINK)
            .usingConvertsTo(Items.GLASS_BOTTLE));

    // Los tres patrones de estandarte griegos. El patrón vive en datapack
    // (data/deluxelib/banner_pattern/<id>.json) y estos ítems son sólo su llave en el telar: LoomMenu
    // acepta un ítem en la ranura de patrón si está en #minecraft:loom_patterns Y lleva el componente
    // PROVIDES_BANNER_PATTERNS, que es un HolderSet resuelto desde el tag correspondiente — ver
    // MythosMortalsBannerPatterns.
    //
    // delayedComponent, no component: getOrThrow(tag) necesita el lookup de registros, que sólo
    // existe mientras corre el bootstrap de componentes. Resolverlo aquí, al construir las
    // Properties, es demasiado pronto — mismo motivo por el que XIFOS_SWORD construye las suyas de
    // forma perezosa.
    //
    // stacksTo(1) como todos los de vanilla, pero sin rareza: RARE lo reservan mojang/skull/globe,
    // que no se craftean. Estos tres sí (papel + un ingrediente temático, ver
    // data/deluxelib/recipe/*_banner_pattern.json).
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
            .repairable(Items.IRON_INGOT)
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

    /** The "Deluxe Armory" creative tab, iconed with the Athenian helmet. */
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
                output.accept(ATHENA_BANNER_PATTERN.get());
                output.accept(CENTAUR_BANNER_PATTERN.get());
                output.accept(SPARTA_BANNER_PATTERN.get());
            })
            .build());

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        CREATIVE_TABS.register(bus);
    }

    private MythosMortalsItems() {}
}
