package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosMortalsDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MythosMortals.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> MARINATED =
        DATA_COMPONENTS.registerComponentType("marinated", builder -> builder
            .persistent(Unit.CODEC)
            .networkSynchronized(Unit.STREAM_CODEC));

    private MythosMortalsDataComponents() {
    }
}
