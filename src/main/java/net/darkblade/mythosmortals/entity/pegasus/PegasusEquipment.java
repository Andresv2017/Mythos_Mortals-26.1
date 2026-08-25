package net.darkblade.mythosmortals.entity.pegasus;

import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public final class PegasusEquipment extends SimpleContainer {

    public static final int BRIDLE_SLOT = 0;

    public static final int TIER_NONE = 0;
    public static final int TIER_IRON = 1;
    public static final int TIER_GOLD = 2;
    public static final int TIER_DIAMOND = 3;
    public static final int TIER_NETHERITE = 4;

    private final PegasusEntity pegasus;

    public PegasusEquipment(PegasusEntity pegasus) {
        super(1);
        this.pegasus = pegasus;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return slot == BRIDLE_SLOT
                && (stack.isEmpty() || stack.is(MythosMortalsItems.ATHENA_BRIDLE.get()));
    }

    @Override
    public void setChanged() {
        super.setChanged();
        this.pegasus.onEquipmentChanged();
    }

    public ItemStack getBridle() {
        return this.getItem(BRIDLE_SLOT);
    }

    public boolean hasBridle() {
        return !this.getBridle().isEmpty();
    }

    public static int armorTier(ItemStack stack) {
        if (stack.is(Items.IRON_HORSE_ARMOR)) return TIER_IRON;
        if (stack.is(Items.GOLDEN_HORSE_ARMOR)) return TIER_GOLD;
        if (stack.is(Items.DIAMOND_HORSE_ARMOR)) return TIER_DIAMOND;
        if (stack.is(Items.NETHERITE_HORSE_ARMOR)) return TIER_NETHERITE;
        return TIER_NONE;
    }
}
