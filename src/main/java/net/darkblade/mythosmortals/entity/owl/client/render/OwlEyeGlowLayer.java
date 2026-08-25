package net.darkblade.mythosmortals.entity.owl.client.render;
import net.darkblade.mythosmortals.core.MythosMortals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class OwlEyeGlowLayer extends RenderLayer<DeluxeEntityRenderState, CopperOwlModel> {

    private static final Identifier GLOW_TEXTURE =
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/copper_owl_glow.png");

    private static final float HALF_SIZE = 3.0F;

    private static final float VERTICAL_NUDGE = 0.5F;

    private static final float BLINK_EPSILON = 0.01F;

    private static final float ALPHA_BASE = 160.0F;
    private static final float ALPHA_SWING = 95.0F;

    private static final float PULSE_PERIOD_TICKS = 52.0F;
    private static final float PULSE_FREQ = (float) (2.0 * Math.PI / PULSE_PERIOD_TICKS);


    public OwlEyeGlowLayer(RenderLayerParent<DeluxeEntityRenderState, CopperOwlModel> parent) {
        super(parent);
    }

    @Override
    public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector collector,
                       int lightCoords, @NotNull DeluxeEntityRenderState state,
                       float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }

        float pulse = ALPHA_BASE + ALPHA_SWING * Mth.sin(state.ageInTicks * PULSE_FREQ);
        RenderType renderType = OwlEyeGlowRenderType.get(GLOW_TEXTURE);
        CopperOwlModel model = this.getParentModel();

        for (CopperOwlModel.Eye eye : CopperOwlModel.Eye.values()) {
            float blink = model.eyeScale(eye);
            if (blink <= BLINK_EPSILON) {
                continue;
            }

            poseStack.pushPose();
            model.applyEyeTransform(poseStack, eye);

            poseStack.translate(0.0F, VERTICAL_NUDGE / ModelPart.Vertex.SCALE_FACTOR, 0.0F);

            float half = HALF_SIZE * blink / ModelPart.Vertex.SCALE_FACTOR;
            int alpha = Mth.clamp(Math.round(pulse * blink), 0, 255);
            int color = (alpha << 24) | 0xFFFFFF;

            collector.order(1).submitCustomGeometry(poseStack, renderType,
                    (pose, buffer) -> quad(pose, buffer, half, color, lightCoords));

            poseStack.popPose();
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer,
                             float half, int color, int light) {
        Vector3f scratch = new Vector3f();
        vertex(pose, buffer, scratch, -half, -half, 0.0F, 1.0F, color, light);
        vertex(pose, buffer, scratch, -half, half, 0.0F, 0.0F, color, light);
        vertex(pose, buffer, scratch, half, half, 1.0F, 0.0F, color, light);
        vertex(pose, buffer, scratch, half, -half, 1.0F, 1.0F, color, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vector3f scratch,
                               float x, float y, float u, float v, int color, int light) {
        Vector3f p = pose.pose().transformPosition(x, y, 0.0F, scratch);
        buffer.addVertex(p.x(), p.y(), p.z(), color, u, v, OverlayTexture.NO_OVERLAY, light, 0.0F, 0.0F, -1.0F);
    }
}
