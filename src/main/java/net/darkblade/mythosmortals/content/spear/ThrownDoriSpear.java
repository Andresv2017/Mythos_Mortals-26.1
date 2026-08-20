package net.darkblade.mythosmortals.content.spear;
import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;

import net.darkblade.deluxelib.entity.projectile.ThrownWeapon;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A thrown Dori spear — the demo's worked example of {@link ThrownWeapon}. Everything that makes a
 * thrown weapon behave like one (not discarding on hit, so it bleeds speed and drops nearby to be
 * picked up) lives in the base class; what's left here is the three things that make it <em>this</em>
 * spear.
 */
public class ThrownDoriSpear extends ThrownWeapon {
    private static final float THROWN_DAMAGE = 6.0F;

    /** Used by {@code EntityType.Builder.of(ThrownDoriSpear::new, ...)} and on the client for incoming packets. */
    public ThrownDoriSpear(EntityType<? extends ThrownDoriSpear> type, Level level) {
        super(type, level);
    }

    /** Thrown by a player/mob via {@code DoriSpearItem}. */
    public ThrownDoriSpear(Level level, LivingEntity owner, ItemStack pickupItemStack) {
        super(MythosMortalsRegistry.THROWN_DORI_SPEAR.get(), level, owner, pickupItemStack);
    }

    /** Spawned at a fixed position with no owner (e.g. from a dispenser via {@code ProjectileItem}). */
    public ThrownDoriSpear(Level level, double x, double y, double z, ItemStack pickupItemStack) {
        super(MythosMortalsRegistry.THROWN_DORI_SPEAR.get(), level, x, y, z, pickupItemStack);
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
