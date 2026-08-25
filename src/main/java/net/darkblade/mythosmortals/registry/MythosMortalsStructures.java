package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.mythosmortals.worldgen.structure.MarkedStructurePiece;
import net.darkblade.mythosmortals.worldgen.structure.MarkedTemplateStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosMortalsStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_TYPE, MythosMortals.MODID);


    public static final DeferredHolder<StructureType<?>, StructureType<MarkedTemplateStructure>> MARKED_TEMPLATE_STRUCTURE =
        STRUCTURE_TYPES.register("marked_template",
            () -> (StructureType<MarkedTemplateStructure>) () -> MarkedTemplateStructure.CODEC);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
        DeferredRegister.create(Registries.STRUCTURE_PIECE, MythosMortals.MODID);


    public static final DeferredHolder<StructurePieceType, StructurePieceType> MARKED_STRUCTURE_PIECE =
        STRUCTURE_PIECES.register("marked_structure",
            () -> (StructurePieceType.StructureTemplateType) MarkedStructurePiece::new);

    private MythosMortalsStructures() {
    }
}
