package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.mythosmortals.entity.pegasus.menu.PegasusInventoryMenu;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosMortalsMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(Registries.MENU, MythosMortals.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<PegasusInventoryMenu>> PEGASUS_MENU =
        MENU_TYPES.register("pegasus_inventory",
            () -> IMenuTypeExtension.create(PegasusInventoryMenu::new));

    private MythosMortalsMenus() {
    }
}
