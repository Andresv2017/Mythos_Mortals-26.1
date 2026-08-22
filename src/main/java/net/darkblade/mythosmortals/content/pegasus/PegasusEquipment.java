package net.darkblade.mythosmortals.content.pegasus;

import net.darkblade.mythosmortals.registry.MythosMortalsItems;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

/**
 * The pegasus' bridle slot.
 *
 * <p>The saddle and the body armour live in the vanilla {@code SADDLE} and {@code BODY} equipment
 * slots — those already give syncing, dropping on death and NBT for free. The bridle has no vanilla
 * slot to borrow, so it gets this one-slot container, which the entity persists and drops itself.
 */
public final class PegasusEquipment extends SimpleContainer {

    public static final int BRIDLE_SLOT = 0;

    /** Armour tiers the renderer has a texture layer for. */
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

    /**
     * Maps a body-armour stack onto the texture layer to draw. Armour the pegasus can wear but has
     * no layer for (leather, copper) still protects it — it just renders bare.
     */
    public static int armorTier(ItemStack stack) {
        if (stack.is(Items.IRON_HORSE_ARMOR)) return TIER_IRON;
        if (stack.is(Items.GOLDEN_HORSE_ARMOR)) return TIER_GOLD;
        if (stack.is(Items.DIAMOND_HORSE_ARMOR)) return TIER_DIAMOND;
        if (stack.is(Items.NETHERITE_HORSE_ARMOR)) return TIER_NETHERITE;
        return TIER_NONE;
    }
}
