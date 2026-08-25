package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.deluxelib.block.StatueBlockEntity;
import net.darkblade.mythosmortals.entity.owl.statue.OwlStatueBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosMortalsBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MythosMortals.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StatueBlockEntity>> OWL_STATUE_BLOCK_ENTITY =
        StatueBlockEntity.registerType(BLOCK_ENTITY_TYPES, "owl_statue", MythosMortalsBlocks.OWL_STATUE::get, OwlStatueBlock.OWL_TYPE);

    private MythosMortalsBlockEntities() {
    }
}
