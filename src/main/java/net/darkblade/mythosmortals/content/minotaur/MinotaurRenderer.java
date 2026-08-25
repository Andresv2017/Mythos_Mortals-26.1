package net.darkblade.mythosmortals.content.minotaur;
import net.darkblade.mythosmortals.MythosMortals;

import com.mojang.blaze3d.vertex.PoseStack;
import net.darkblade.deluxelib.client.anim.HumanoidPoseApplier;
import net.darkblade.deluxelib.client.render.CustomMobRenderer;
import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;
import net.darkblade.deluxelib.client.render.RiderPoseHandler;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class MinotaurRenderer extends CustomMobRenderer<MinotaurEntity, MinotaurModel> implements RiderPoseHandler {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/minotaur.png");

    public MinotaurRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MinotaurModel(ctx.bakeLayer(MinotaurModel.LAYER_LOCATION)), 0.9F);
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
        HumanoidPoseApplier.applyStatic(MinotaurRiderPose.RIDER_POSE, model);
    }

    @Override
    public <S extends HumanoidRenderState> boolean canApplyTo(@NotNull LivingEntity vehicle, @NotNull S riderState) {
        return true; // pose every humanoid rider
    }

    @Override
    public void applyRiderTransform(@NotNull LivingEntityRenderState vehicleState, @NotNull PoseStack poseStack) {
        // Walk the vehicle's bone chain to the mid-back (torso), which lands the rider's origin near
        // the saddle area. Fine-tune the exact seat live in-game with /riderpose (numpad), then paste
        // the printed translate/rotation here. Seat frame: +Y is DOWN, +Z toward the tail.
        MinotaurModel model = this.getModel();
        model.minotaur.translateAndRotate(poseStack);
        model.body.translateAndRotate(poseStack);
        model.torso.translateAndRotate(poseStack);
        // Seat offset dialed in with /riderpose.
        poseStack.translate(0.000F, -2.100F, 0.550F);
    }
}
