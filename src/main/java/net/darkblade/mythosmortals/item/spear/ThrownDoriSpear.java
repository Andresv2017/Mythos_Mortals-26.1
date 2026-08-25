package net.darkblade.mythosmortals.item.spear;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;

import net.darkblade.deluxelib.entity.projectile.ThrownWeapon;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.darkblade.mythosmortals.registry.MythosMortalsEntities;

public class ThrownDoriSpear extends ThrownWeapon {
    private static final float THROWN_DAMAGE = 6.0F;

    public ThrownDoriSpear(EntityType<? extends ThrownDoriSpear> type, Level level) {
        super(type, level);
    }

    public ThrownDoriSpear(Level level, LivingEntity owner, ItemStack pickupItemStack) {
        super(MythosMortalsEntities.THROWN_DORI_SPEAR.get(), level, owner, pickupItemStack);
    }

    public ThrownDoriSpear(Level level, double x, double y, double z, ItemStack pickupItemStack) {
        super(MythosMortalsEntities.THROWN_DORI_SPEAR.get(), level, x, y, z, pickupItemStack);
    }

    @Override
    protected float thrownDamage() {
        return THROWN_DAMAGE;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(MythosMortalsItems.DORI_SPEAR.get());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }
}
