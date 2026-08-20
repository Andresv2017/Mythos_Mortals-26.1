package net.darkblade.mythosmortals.content.athenian;
import net.darkblade.mythosmortals.MythosMortals;

import net.darkblade.deluxelib.client.render.CustomMobRenderer;
import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class AthenianRenderer extends CustomMobRenderer<AthenianEntity, AthenianModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/athenian.png");

    public AthenianRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new AthenianModel(ctx.bakeLayer(AthenianModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull DeluxeEntityRenderState state) {
        return TEXTURE;
    }
}
