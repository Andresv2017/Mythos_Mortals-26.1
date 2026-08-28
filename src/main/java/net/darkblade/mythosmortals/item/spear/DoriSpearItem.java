package net.darkblade.mythosmortals.item.spear;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.deluxelib.entity.projectile.ThrownWeapon;
import net.darkblade.deluxelib.item.ThrownWeaponItem;
import net.minecraft.core.Position;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

public class DoriSpearItem extends ThrownWeaponItem {

    public DoriSpearItem(Item.Properties properties) {
        super(properties);
    }

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
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (level.isClientSide()) {
            return super.releaseUsing(stack, level, entity, remainingTime);
        }
        boolean willThrow = this.getUseDuration(stack, entity) - remainingTime >= this.throwThresholdTicks();
        if (willThrow && stack.nextDamageWillBreak()) {
            return false;
        }
        if (willThrow && entity instanceof Player player) {
            stack.hurtWithoutBreaking(1, player);
        }
        return super.releaseUsing(stack, level, entity, remainingTime);
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
