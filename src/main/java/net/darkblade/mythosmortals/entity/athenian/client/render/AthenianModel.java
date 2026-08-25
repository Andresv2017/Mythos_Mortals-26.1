package net.darkblade.mythosmortals.entity.athenian.client.render;
import net.darkblade.mythosmortals.core.MythosMortals;

import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;
import net.darkblade.deluxelib.client.rig.AnimContext;
import net.darkblade.deluxelib.client.rig.Rig;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

public class AthenianModel extends EntityModel<DeluxeEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(MythosMortals.MODID, "athenian"), "main"
    );

    private static final Rig<AthenianModel> RIG =
        Rig.<AthenianModel>builder()
            .resetPoses()
            .keyframeBlend(400L, 0)
            .lookAt(m -> m.head, 40f, 30f)
            .build();

    public final ModelPart head;

    public AthenianModel(ModelPart root) {
        super(root);
        this.head = root.getChild("base").getChild("spartan").getChild("waist").getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Named "base", NOT "root": HierarchicalModel.getAnyDescendantWithName special-cases the
        // name "root" to the invisible baked layer root (pivot Y=0, 1.5 blocks up), so animation
        // channels on a bone called "root" would rotate around the wrong pivot. Keep the
        // Blockbench bone renamed to match.
        PartDefinition root = partdefinition.addOrReplaceChild("base", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition spartan = root.addOrReplaceChild("spartan", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition waist = spartan.addOrReplaceChild("waist", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition head = waist.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(64, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.1F))
            .texOffs(52, 17).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition helmet = head.addOrReplaceChild("helmet", CubeListBuilder.create()
            .texOffs(96, 0).addBox(-4.0F, 5.0F, -5.2F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.5F))
            .texOffs(56, 6).addBox(0.0F, -10.0F, -5.2F, 0.0F, 15.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(86, 12).addBox(-1.0F, -6.0F, -5.2F, 2.0F, 8.0F, 10.0F, new CubeDeformation(0.0F))
            .texOffs(98, 18).addBox(-0.5F, -8.0F, -5.2F, 1.0F, 11.0F, 12.0F, new CubeDeformation(0.0F))
            .texOffs(32, 0).addBox(-4.0F, -4.0F, -5.2F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)),
            PartPose.offset(0.0F, -4.0F, 1.2F));

        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
            PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create(), PartPose.offset(0.0F, 12.225F, 0.0F));
        skirt.addOrReplaceChild("skirt_front", CubeListBuilder.create()
            .texOffs(56, 35).addBox(-4.0F, 0.275F, -0.5F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.25F)),
            PartPose.offset(0.0F, 0.0F, -1.5F));
        skirt.addOrReplaceChild("skirt_back", CubeListBuilder.create()
            .texOffs(56, 35).addBox(-4.0F, 0.275F, -0.5F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.25F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.5F, 0.0F, 3.1416F, 0.0F));

        PartDefinition right_arm = waist.addOrReplaceChild("right_arm", CubeListBuilder.create()
            .texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.2F)),
            PartPose.offset(-5.0F, -10.0F, 0.0F));

        PartDefinition spear = right_arm.addOrReplaceChild("spear", CubeListBuilder.create()
            .texOffs(50, 62).addBox(-0.6F, -0.5F, -22.0F, 1.0F, 1.0F, 38.0F, new CubeDeformation(0.0F))
            .texOffs(120, 93).addBox(-1.6F, -1.5F, -23.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(90, 91).addBox(-0.6F, -1.5F, -29.0F, 1.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(104, 94).addBox(-0.2F, -1.5F, -32.0F, 0.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
            PartPose.offset(-1.4F, 8.5F, 0.0F));
        spear.addOrReplaceChild("spear_r1", CubeListBuilder.create()
            .texOffs(116, 91).addBox(-0.05F, -1.5F, 0.0F, 0.1F, 3.0F, 6.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(-0.1F, 0.0F, 16.0F, 0.0F, 0.0F, 0.7854F));
        spear.addOrReplaceChild("spear_r2", CubeListBuilder.create()
            .texOffs(116, 91).addBox(-0.05F, -1.5F, 0.0F, 0.1F, 3.0F, 6.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(-0.1F, 0.0F, 16.0F, 0.0F, 0.0F, -0.7854F));

        waist.addOrReplaceChild("left_arm", CubeListBuilder.create()
            .texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.2F)),
            PartPose.offset(5.0F, -10.0F, 0.0F))
            .addOrReplaceChild("shield", CubeListBuilder.create()
                .texOffs(0, 96).addBox(-0.5F, -8.0F, -8.0F, 1.0F, 16.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(34, 102).addBox(-0.5F, 8.0F, -6.0F, 1.0F, 1.0F, 12.0F, new CubeDeformation(0.0F))
                .texOffs(18, 103).addBox(-0.5F, 9.0F, -4.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(12, 99).addBox(-0.5F, -6.0F, 8.0F, 1.0F, 12.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(4, 103).addBox(-0.5F, -4.0F, 9.0F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(8, 99).addBox(-0.5F, -6.0F, -9.0F, 1.0F, 12.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 103).addBox(-0.5F, -4.0F, -10.0F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(34, 115).addBox(-0.5F, -9.0F, -6.0F, 1.0F, 1.0F, 12.0F, new CubeDeformation(0.0F))
                .texOffs(18, 94).addBox(-0.5F, -10.0F, -4.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.5F, 7.0F, 0.0F, -0.7854F, 0.0F, 0.0F));

        spartan.addOrReplaceChild("right_leg", CubeListBuilder.create()
            .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.22F)),
            PartPose.offset(-1.9F, 0.0F, 0.0F));

        spartan.addOrReplaceChild("left_leg", CubeListBuilder.create()
            .texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.22F)),
            PartPose.offset(1.9F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(DeluxeEntityRenderState state) {
        RIG.apply(state, this, AnimContext.from(state));
    }
}
