package net.darkblade.mythosmortals.registry;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.mythosmortals.block.amphora.MarinatingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MythosMortalsRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, MythosMortals.MODID);


    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MarinatingRecipe>> MARINATING =
        RECIPE_SERIALIZERS.register("marinating",
            () -> new RecipeSerializer<>(MarinatingRecipe.MAP_CODEC, MarinatingRecipe.STREAM_CODEC));

    private MythosMortalsRecipes() {
    }
}
