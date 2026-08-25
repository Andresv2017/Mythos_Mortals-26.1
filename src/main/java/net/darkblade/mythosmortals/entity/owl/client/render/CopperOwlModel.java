package net.darkblade.mythosmortals.entity.owl.client.render;
import net.darkblade.mythosmortals.core.MythosMortals;

import com.mojang.blaze3d.vertex.PoseStack;
import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;
import net.darkblade.deluxelib.client.rig.AnimContext;
import net.darkblade.deluxelib.client.rig.Rig;
import net.darkblade.mythosmortals.entity.arpy.client.render.ArpyModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;


public class CopperOwlModel extends EntityModel<DeluxeEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "copper_owl"), "main");

    private static final Rig<CopperOwlModel> RIG =
            Rig.<CopperOwlModel>builder()
                    .resetPoses()
                    .keyframeBlend(200L, 0)
                    .lookAt(m -> m.head, 30f, 20f)
                    // Perched owls are otherwise completely static: every clip's play condition
                    // excludes isPerched(), and the look-at above contributes nothing because
                    // PerchGoal sets yHeadRot == yBodyRot, leaving its head-relative yaw at zero.
                    .idleHead(m -> m.head, state -> state.perched)
                    // Same gate: away from the perch the clips animate the eyes on SCALE themselves,
                    // and two writers on one bone would fight. The eye glow layer reads the same
                    // yScale, so the halo blinks along for free.
                    .idleBlink(state -> state.perched,
                            m -> m.eyePart(Eye.A), m -> m.eyePart(Eye.B))
                    .build();

    public enum Eye {
        A, B
    }

    private final ModelPart owl;
    private final ModelPart body;
    private final ModelPart top;
    public final ModelPart head;
    private final ModelPart eye;
    private final ModelPart eye2;

    public CopperOwlModel(ModelPart root) {
        super(root);
        this.owl = root.getChild("owl");
        this.body = this.owl.getChild("body");
        this.top = this.body.getChild("top");
        this.head = this.top.getChild("head");
        this.eye = this.head.getChild("eye");
        this.eye2 = this.head.getChild("eye2");
    }

    private static final float EYE_A_PUPIL_Y = -0.5F;
    private static final float EYE_B_PUPIL_Y = -0.25F;

    public void applyEyeTransform(PoseStack poseStack, Eye which) {
        this.root().translateAndRotate(poseStack);
        this.owl.translateAndRotate(poseStack);
        this.body.translateAndRotate(poseStack);
        this.top.translateAndRotate(poseStack);
        this.head.translateAndRotate(poseStack);
        eyePart(which).translateAndRotate(poseStack);

        float pupilY = which == Eye.A ? EYE_A_PUPIL_Y : EYE_B_PUPIL_Y;
        poseStack.translate(0.0F, pupilY / ModelPart.Vertex.SCALE_FACTOR, 0.0F);
    }

    public float eyeScale(Eye which) {
        return eyePart(which).yScale;
    }

    public ModelPart eyePart(Eye which) {
        return which == Eye.A ? this.eye : this.eye2;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition owl = partdefinition.addOrReplaceChild("owl", CubeListBuilder.create(), PartPose.offset(0.0F, 16.5F, 0.0F));

        PartDefinition body = owl.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 3.4889F, 0.0F));

        PartDefinition top = body.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 9).addBox(-3.0F, -6.9667F, -2.0F, 6.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.0222F, 0.0F, 0.0873F, 0.0F, 0.0F));

        top.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0333F, 2.0F, 1.1345F, 0.0F, 0.0F));

        top.addOrReplaceChild("wing", CubeListBuilder.create().texOffs(0, 20).addBox(1.0625F, 0.5F, -2.1F, 0.0F, 9.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(12, 24).addBox(0.0375F, -0.5F, -2.1F, 1.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.9625F, -5.4667F, 0.1F, 0.1309F, 0.0F, 0.0F));

        PartDefinition head = top.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.6F, -4.0F, -2.5F, 7.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(32, 15).addBox(-0.6F, -4.0F, -3.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1F, -6.9667F, 0.0F, -0.0873F, 0.0F, 0.0F));

        PartDefinition eye = head.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(32, 21).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.1F, -2.0F, -2.51F));

        eye.addOrReplaceChild("pupil", CubeListBuilder.create().texOffs(24, 8).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, -0.01F));

        head.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(24, 5).addBox(-2.5F, -1.5F, 0.0F, 5.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(2.9F, -4.5F, -2.525F));

        head.addOrReplaceChild("brow2", CubeListBuilder.create().texOffs(32, 8).addBox(-2.5F, -1.5F, 0.0F, 5.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.1F, -4.5F, -2.525F));

        PartDefinition eye2 = head.addOrReplaceChild("eye2", CubeListBuilder.create().texOffs(32, 23).addBox(-1.5F, -0.75F, 0.005F, 3.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, -2.25F, -2.515F));

        eye2.addOrReplaceChild("pupil2", CubeListBuilder.create().texOffs(26, 8).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.25F, -0.005F));

        top.addOrReplaceChild("wing2", CubeListBuilder.create().texOffs(20, 9).addBox(-1.0625F, 0.5F, -2.1F, 0.0F, 9.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(22, 24).addBox(-1.0375F, -0.5F, -2.1F, 1.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.9625F, -5.4667F, 0.1F, 0.1309F, 0.0F, 0.0F));

        PartDefinition leg = body.addOrReplaceChild("leg", CubeListBuilder.create().texOffs(32, 25).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 0.0111F, 0.0F, 0.0F, -0.2618F, 0.0F));

        leg.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(12, 20).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(12, 22).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 3.0F, 0.0F));

        PartDefinition leg2 = body.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(32, 28).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 0.0111F, 0.0F, 0.0F, 0.2618F, 0.0F));

        leg2.addOrReplaceChild("foot2", CubeListBuilder.create().texOffs(32, 11).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(32, 13).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 3.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(@NotNull DeluxeEntityRenderState state) {
        RIG.apply(state, this, AnimContext.from(state));
    }
}
