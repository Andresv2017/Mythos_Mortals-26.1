package net.darkblade.mythosmortals.content.pegasus;

import com.mojang.blaze3d.vertex.PoseStack;
import net.darkblade.deluxelib.client.anim.HumanoidPoseApplier;
import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;
import net.darkblade.deluxelib.client.render.FlyingMobRenderer;
import net.darkblade.deluxelib.client.render.RiderPoseHandler;
import net.darkblade.mythosmortals.MythosMortals;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Renders the pegasus, its tack layers, and any rider seated on its back.
 *
 * <p>The reins bone belongs to the base mesh but only makes sense on a bridled pegasus, so it is
 * toggled per frame from the render state rather than baked into the model.
 */
public class PegasusRenderer extends FlyingMobRenderer<PegasusEntity, PegasusModel> implements RiderPoseHandler {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/pegasus/pegasus.png");

    public PegasusRenderer(EntityRendererProvider.Context context) {
        super(context, new PegasusModel(context.bakeLayer(PegasusModel.LAYER_LOCATION)), 0.8F);
        this.addLayer(new PegasusEquipmentLayer(this));
    }

    @Override
    public @NotNull DeluxeEntityRenderState createRenderState() {
        return new PegasusRenderState();
    }

    @Override
    public void extractRenderState(@NotNull PegasusEntity entity, @NotNull DeluxeEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (state instanceof PegasusRenderState pegasus) {
            pegasus.hasSaddle = entity.isSaddled();
            pegasus.hasBridle = entity.hasBridle();
            pegasus.armorTier = entity.armorTier();
        }
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull DeluxeEntityRenderState state) {
        return TEXTURE;
    }

    // ------------------------------------------------------------------
    // RiderPoseHandler
    // ------------------------------------------------------------------

    @Override
    public <S extends HumanoidRenderState> void applyRiderPose(@NotNull LivingEntity vehicle,
                                                               @NotNull HumanoidModel<S> model,
                                                               @NotNull S riderState) {
        HumanoidPoseApplier.applyStatic(PegasusRiderPose.RIDER_POSE, model);
    }

    @Override
    public <S extends HumanoidRenderState> boolean canApplyTo(@NotNull LivingEntity vehicle, @NotNull S riderState) {
        return true;
    }

    @Override
    public void applyRiderTransform(@NotNull LivingEntityRenderState vehicleState, @NotNull PoseStack poseStack) {
        // Walk down the rig to the barrel, which puts the rider's origin over the saddle. Fine-tune
        // live with /riderpose (numpad) and paste the printed offset here. +Y is DOWN, +Z is tailward.
        PegasusModel model = this.getModel();
        model.pegasus.translateAndRotate(poseStack);
        model.body.translateAndRotate(poseStack);
        model.top.translateAndRotate(poseStack);
        poseStack.translate(0.0F, -0.400F, 0.200F);
    }
}
