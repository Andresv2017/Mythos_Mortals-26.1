package net.darkblade.mythosmortals.content.pegasus;

import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;
import net.darkblade.deluxelib.client.rig.AnimContext;
import net.darkblade.deluxelib.client.rig.Rig;
import net.darkblade.mythosmortals.MythosMortals;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * Blockbench-exported pegasus rig. Bone names are the ones {@link PegasusAnimation} animates, so
 * renaming a bone here means re-exporting the animations too.
 *
 * <p>{@code reins} is part of the base mesh but only belongs on a bridled pegasus, so the renderer
 * toggles its visibility from {@code DeluxeEntityRenderState}.
 */
public class PegasusModel extends EntityModel<DeluxeEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "pegasus"), "main");

    private static final Rig<PegasusModel> RIG =
            Rig.<PegasusModel>builder()
                    .resetPoses()
                    .keyframeBlend(200L, 0)
                    .lookAt(m -> m.head, 30f, 20f)
                    .build();

    public final ModelPart pegasus;
    public final ModelPart body;
    public final ModelPart top;
    public final ModelPart neck;
    public final ModelPart head;
    public final ModelPart reins;

    public PegasusModel(ModelPart root) {
        super(root);
        this.pegasus = root.getChild("pegasus");
        this.body = this.pegasus.getChild("body");
        this.top = this.body.getChild("top");
        this.neck = this.top.getChild("neck");
        this.head = this.neck.getChild("head");
        this.reins = this.head.getChild("reins");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition pegasus = partdefinition.addOrReplaceChild("pegasus", CubeListBuilder.create(), PartPose.offset(0.0F, 13.03F, 0.69F));

        PartDefinition body = pegasus.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition top = body.addOrReplaceChild("top", CubeListBuilder.create(), PartPose.offset(0.0F, 0.12F, -1.24F));

        PartDefinition neck = top.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(64, 71).addBox(-2.0F, -15.0F, -3.0F, 4.0F, 16.0F, 9.0F, new CubeDeformation(0.0F))
        .texOffs(0, 205).addBox(-2.0F, -15.0F, -3.0F, 4.0F, 16.0F, 9.0F, new CubeDeformation(0.1F))
        .texOffs(90, 64).addBox(-1.0F, -16.0F, 4.0F, 2.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(0, 73).addBox(1.0F, -13.0F, 1.0F, 4.0F, 13.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.15F, -7.45F, 0.5236F, 0.0F, 0.0F));

        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(80, 0).addBox(-3.0F, -3.0F, -6.4F, 6.0F, 6.0F, 8.0F, new CubeDeformation(0.0F))
        .texOffs(0, 242).addBox(-3.0F, -3.0F, -6.4F, 6.0F, 6.0F, 8.0F, new CubeDeformation(0.1F))
        .texOffs(172, 8).addBox(-3.0F, -3.0F, -6.4F, 6.0F, 6.0F, 8.0F, new CubeDeformation(0.25F))
        .texOffs(238, 27).addBox(-3.0F, -3.0F, 2.1F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.25F))
        .texOffs(80, 99).addBox(-1.0F, -5.0F, -2.4F, 2.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
        .texOffs(60, 32).addBox(1.0F, -5.0F, -4.4F, 4.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
        .texOffs(38, 91).addBox(-3.0F, -6.0F, 0.6F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(16, 93).addBox(1.0F, -6.0F, 0.6F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(88, 52).addBox(-2.5F, -3.0F, -12.4F, 5.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
        .texOffs(0, 230).addBox(-2.5F, -3.0F, -12.4F, 5.0F, 6.0F, 6.0F, new CubeDeformation(0.1F))
        .texOffs(240, 18).addBox(-2.5F, -3.0F, -9.4F, 5.0F, 6.0F, 3.0F, new CubeDeformation(0.25F))
        .texOffs(247, 5).addBox(-3.5F, 1.0F, -10.4F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(100, 24).addBox(-2.5F, 3.0F, -12.4F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(247, 5).mirror().addBox(2.5F, 1.0F, -10.4F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, -13.0F, 2.4F));

        PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(226, 33).addBox(-2.5F, -2.0F, -1.0F, 5.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, -8.4F, -0.5236F, 0.0F, 0.0F));

        PartDefinition reins = head.addOrReplaceChild("reins", CubeListBuilder.create().texOffs(179, 1).addBox(-4.5F, -1.0F, 0.0F, 9.0F, 2.0F, 21.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 2.0F, -9.4F, -0.3927F, 0.0F, 0.0F));

        PartDefinition tail = top.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(88, 32).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 15.0F, 5.0F, new CubeDeformation(0.0F))
        .texOffs(38, 103).addBox(-1.5F, 10.0F, -1.0F, 3.0F, 0.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.15F, 12.55F, 0.6981F, 0.0F, 0.0F));

        PartDefinition belly = top.addOrReplaceChild("belly", CubeListBuilder.create().texOffs(44, 49).addBox(-5.0F, -4.75F, -0.25F, 10.0F, 10.0F, 12.0F, new CubeDeformation(0.0F))
        .texOffs(26, 219).addBox(-5.0F, -4.75F, 8.75F, 10.0F, 10.0F, 3.0F, new CubeDeformation(0.1F))
        .texOffs(218, 0).addBox(-5.0F, -4.75F, -0.25F, 10.0F, 10.0F, 9.0F, new CubeDeformation(0.25F))
        .texOffs(0, 49).addBox(-5.5F, -6.75F, -11.25F, 11.0F, 13.0F, 11.0F, new CubeDeformation(0.0F))
        .texOffs(28, 232).addBox(-5.5F, -6.75F, -11.25F, 11.0F, 13.0F, 11.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, -5.4F, 0.8F));

        PartDefinition Body_r1 = belly.addOrReplaceChild("Body_r1", CubeListBuilder.create().texOffs(218, 29).addBox(-5.0F, -3.5F, 0.0F, 10.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.35F, 8.25F, -0.5236F, 0.0F, 0.0F));

        PartDefinition Body_r2 = belly.addOrReplaceChild("Body_r2", CubeListBuilder.create().texOffs(218, 24).addBox(-5.0F, -4.5F, 0.0F, 10.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.25F, 8.75F, -0.5236F, 0.0F, 0.0F));

        PartDefinition wing3 = top.addOrReplaceChild("wing3", CubeListBuilder.create().texOffs(80, 14).addBox(-0.5F, -12.0F, -7.5F, 1.0F, 9.0F, 9.0F, new CubeDeformation(0.0F))
        .texOffs(104, 29).addBox(-0.5F, -3.0F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, -12.15F, -2.95F));

        PartDefinition wing4 = wing3.addOrReplaceChild("wing4", CubeListBuilder.create().texOffs(118, 0).addBox(0.0F, -23.75F, -8.0F, 1.0F, 24.0F, 13.0F, new CubeDeformation(0.0F))
        .texOffs(0, -8).addBox(0.9F, -37.75F, -8.0F, 0.0F, 38.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, -11.25F, -5.5F));

        PartDefinition wing2 = top.addOrReplaceChild("wing2", CubeListBuilder.create().texOffs(80, 14).mirror().addBox(-0.5F, -12.0F, -7.5F, 1.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false)
        .texOffs(104, 29).mirror().addBox(-0.5F, -3.0F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(5.0F, -12.15F, -2.95F));

        PartDefinition wing5 = wing2.addOrReplaceChild("wing5", CubeListBuilder.create().texOffs(0, -8).mirror().addBox(-0.9F, -37.75F, -8.0F, 0.0F, 38.0F, 19.0F, new CubeDeformation(0.0F)).mirror(false)
        .texOffs(118, 0).mirror().addBox(-1.0F, -23.75F, -8.0F, 1.0F, 24.0F, 13.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-0.5F, -11.25F, -5.5F));

        PartDefinition Leg = body.addOrReplaceChild("Leg", CubeListBuilder.create().texOffs(90, 84).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 11.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(100, 14).addBox(-2.0F, 5.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.15F))
        .texOffs(0, 197).mirror().addBox(-2.0F, 0.0F, -1.975F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offset(3.0F, -0.03F, -9.59F));

        PartDefinition Leg2 = body.addOrReplaceChild("Leg2", CubeListBuilder.create().texOffs(22, 91).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 11.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(0, 197).addBox(-2.0F, 0.0F, -1.975F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.1F))
        .texOffs(100, 99).addBox(-2.0F, 5.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.15F)), PartPose.offset(-3.0F, -0.03F, -9.59F));

        PartDefinition Leg3 = body.addOrReplaceChild("Leg3", CubeListBuilder.create().texOffs(90, 84).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 11.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(100, 14).addBox(-2.0F, 5.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.15F))
        .texOffs(0, 197).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offset(3.0F, -0.03F, 10.31F));

        PartDefinition Leg4 = body.addOrReplaceChild("Leg4", CubeListBuilder.create().texOffs(22, 91).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 11.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(0, 197).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.1F))
        .texOffs(100, 99).addBox(-2.0F, 5.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.15F)), PartPose.offset(-3.0F, -0.03F, 10.31F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    @Override
    public void setupAnim(@NotNull DeluxeEntityRenderState state) {
        // The reins belong to the bridle, not to the horse: hide the bone unless one is fitted, so
        // the equipment layer has something to paint on only when it should.
        this.reins.visible = state instanceof PegasusRenderState pegasus && pegasus.hasBridle;
        RIG.apply(state, this, AnimContext.from(state));
    }
}
