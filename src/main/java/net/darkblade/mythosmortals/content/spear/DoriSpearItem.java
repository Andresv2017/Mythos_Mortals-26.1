package net.darkblade.mythosmortals.content.spear;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.deluxelib.entity.projectile.ThrownWeapon;
import net.darkblade.deluxelib.item.ThrownWeaponItem;
import net.minecraft.core.Position;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/**
 * The Dori spear item — the demo's worked example of {@link ThrownWeaponItem}. Holding right-click
 * charges a throw and releasing launches a {@link ThrownDoriSpear}; all of that is the base class's.
 * What's left here is what kind of weapon this is, plus how to build its projectile.
 *
 * <p>Melee attacks (plain left-click) use {@link #createAttributes()} — flat attribute modifiers, the
 * same mechanism as {@code TridentItem.createAttributes()} — so they don't interfere with the
 * charge-and-throw flow.
 */
public class DoriSpearItem extends ThrownWeaponItem {

    public DoriSpearItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * Heavier and slower than a sword, matching a two-handed spear: +4 damage, -2.8 speed, and
     * +1.5 entity-interaction range (3.0 base -> 4.5) so it actually hits farther than a sword.
     */
    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.8, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "reach.dori_spear"), 1.5, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND)
            .build();
    }

    @Override
    protected ThrownWeapon createThrown(Level level, LivingEntity owner, ItemStack pickupItemStack) {
        return new ThrownDoriSpear(level, owner, pickupItemStack);
    }

    @Override
    protected ThrownWeapon createDispensed(Level level, Position position, ItemStack pickupItemStack) {
        return new ThrownDoriSpear(level, position.x(), position.y(), position.z(), pickupItemStack);
    }
}
