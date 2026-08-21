package net.darkblade.mythosmortals.content.owl;
import net.darkblade.mythosmortals.MythosMortals;

import com.mojang.blaze3d.vertex.PoseStack;
import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;
import net.darkblade.deluxelib.client.render.FlyingMobRenderer;
import net.darkblade.mythosmortals.content.owl.perch.OwlPerchPlacement;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class OwlRenderer extends FlyingMobRenderer<OwlEntity, CopperOwlModel> {

    public static final float OWL_MODEL_SCALE = OwlPerchPlacement.MODEL_SCALE;

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/copper_owl.png");

    public OwlRenderer(EntityRendererProvider.Context context) {
        super(context, new CopperOwlModel(context.bakeLayer(CopperOwlModel.LAYER_LOCATION)), 0.25f);
        this.addLayer(new OwlEyeGlowLayer(this));
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull DeluxeEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public void extractRenderState(@NotNull OwlEntity entity, @NotNull DeluxeEntityRenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.perched = entity.isPerched();
    }

    @Override
    protected void setupRotations(@NotNull DeluxeEntityRenderState state, @NotNull PoseStack poseStack,
                                  float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        poseStack.scale(OWL_MODEL_SCALE, OWL_MODEL_SCALE, OWL_MODEL_SCALE);
    }
}
