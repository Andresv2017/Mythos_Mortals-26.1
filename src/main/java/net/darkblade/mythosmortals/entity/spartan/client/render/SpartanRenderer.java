package net.darkblade.mythosmortals.entity.spartan.client.render;
import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.mythosmortals.entity.spartan.SpartanEntity;

import net.darkblade.deluxelib.client.render.CustomMobRenderer;
import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class SpartanRenderer extends CustomMobRenderer<SpartanEntity, SpartanModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/spartan.png");

    public SpartanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SpartanModel(ctx.bakeLayer(SpartanModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull DeluxeEntityRenderState state) {
        return TEXTURE;
    }
}
