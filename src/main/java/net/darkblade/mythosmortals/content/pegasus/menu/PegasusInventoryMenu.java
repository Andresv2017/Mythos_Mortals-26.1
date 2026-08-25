package net.darkblade.mythosmortals.content.pegasus.menu;

import net.darkblade.mythosmortals.content.pegasus.PegasusEntity;
import net.darkblade.mythosmortals.content.pegasus.PegasusEquipment;
import net.darkblade.mythosmortals.registry.MythosMortalsRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PegasusInventoryMenu extends AbstractContainerMenu {

    public static final int SLOT_SADDLE = 0;
    public static final int SLOT_ARMOR = 1;
    public static final int SLOT_BRIDLE = 2;
    private static final int TACK_SLOTS = 3;

    private final Container bridleContainer;
    private final @Nullable PegasusEntity pegasus;

    public PegasusInventoryMenu(int containerId, Inventory playerInventory,
                                Container bridleContainer, @Nullable PegasusEntity pegasus) {
        super(MythosMortalsRegistry.PEGASUS_MENU.get(), containerId);
        this.bridleContainer = bridleContainer;
        this.pegasus = pegasus;
        bridleContainer.startOpen(playerInventory.player);

        if (pegasus != null) {
            this.addSlot(new TackSlot(pegasus.createEquipmentSlotContainer(EquipmentSlot.SADDLE),
                    pegasus, EquipmentSlot.SADDLE, 8, 18));
            this.addSlot(new TackSlot(pegasus.createEquipmentSlotContainer(EquipmentSlot.BODY),
                    pegasus, EquipmentSlot.BODY, 8, 36));
        } else {
            // Client-side fallback if the entity has not arrived yet: harmless placeholders that
            // accept nothing, replaced as soon as the menu is reopened.
            Container empty = new SimpleContainer(2);
            this.addSlot(new InertSlot(empty, 0, 8, 18));
            this.addSlot(new InertSlot(empty, 1, 8, 36));
        }
        this.addSlot(new Slot(bridleContainer, PegasusEquipment.BRIDLE_SLOT, 8, 54) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return bridleContainer.canPlaceItem(PegasusEquipment.BRIDLE_SLOT, stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        this.addStandardInventorySlots(playerInventory, 8, 84);
    }

    public PegasusInventoryMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainer(1), resolve(playerInventory, buf.readVarInt()));
    }

    private static @Nullable PegasusEntity resolve(Inventory playerInventory, int entityId) {
        Entity entity = playerInventory.player.level().getEntity(entityId);
        return entity instanceof PegasusEntity found ? found : null;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.pegasus != null
                && this.pegasus.isAlive()
                && this.bridleContainer.stillValid(player)
                && player.isWithinEntityInteractionRange(this.pegasus, 4.0);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.bridleContainer.stopOpen(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (slotIndex < TACK_SLOTS) {
            if (!this.moveItemStackTo(stack, TACK_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveIntoFirstMatchingTackSlot(stack)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private boolean moveIntoFirstMatchingTackSlot(ItemStack stack) {
        for (int index = 0; index < TACK_SLOTS; index++) {
            Slot target = this.getSlot(index);
            if (!target.hasItem() && target.mayPlace(stack) && this.moveItemStackTo(stack, index, index + 1, false)) {
                return true;
            }
        }
        return false;
    }

    private static class TackSlot extends Slot {
        private final PegasusEntity owner;
        private final EquipmentSlot equipmentSlot;

        TackSlot(Container container, PegasusEntity owner, EquipmentSlot equipmentSlot, int x, int y) {
            super(container, 0, x, y);
            this.owner = owner;
            this.equipmentSlot = equipmentSlot;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.canEquip(this.equipmentSlot, this.owner);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class InertSlot extends Slot {
        InertSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }
    }
}
