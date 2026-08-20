package net.darkblade.mythosmortals.content.arpy;
import net.darkblade.mythosmortals.MythosMortals;

import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;
import net.darkblade.deluxelib.client.render.FlyingMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class ArpyRenderer extends FlyingMobRenderer<ArpyEntity, ArpyModel> {

    private static final Identifier TEXTURE_NORMAL = Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/arpy.png");
    private static final Identifier TEXTURE_ARMORED = Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/arpy_armored.png");

    public ArpyRenderer(EntityRendererProvider.Context context) {
        super(context, new ArpyModel(context.bakeLayer(ArpyModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public void extractRenderState(@NotNull ArpyEntity entity, @NotNull DeluxeEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.armored = entity.isArmored();
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull DeluxeEntityRenderState state) {
        return state.armored ? TEXTURE_ARMORED : TEXTURE_NORMAL;
    }
}
