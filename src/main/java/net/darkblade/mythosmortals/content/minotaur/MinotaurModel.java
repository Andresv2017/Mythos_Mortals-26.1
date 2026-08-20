package net.darkblade.mythosmortals.content.minotaur;
import net.darkblade.mythosmortals.MythosMortals;

import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;
import net.darkblade.deluxelib.client.rig.AnimContext;
import net.darkblade.deluxelib.client.rig.Rig;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

// Mesh made with Blockbench 5.1.5; ported to the DeluxeLib rig pipeline (EntityModel<DeluxeEntityRenderState>).
public class MinotaurModel extends EntityModel<DeluxeEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(MythosMortals.MODID, "minotaur"), "main"
    );

    // reset → blend the animator's keyframes (idle/walk/run/combat_idle are wired; still-null
    // sources are skipped safely by AnimBlend) → head look.
    private static final Rig<MinotaurModel> RIG =
        Rig.<MinotaurModel>builder()
            .resetPoses()
            .keyframeBlend(400L, 0)
            .lookAt(m -> m.head, 40f, 30f)
            .build();

    // Public seat chain, walked by MinotaurRenderer#applyRiderTransform to place the rider.
    public final ModelPart minotaur;
    public final ModelPart body;
    public final ModelPart torso;
    public final ModelPart top;
    public final ModelPart head;

    public MinotaurModel(ModelPart root) {
        super(root);
        this.minotaur = root.getChild("minotaur");
        this.body = this.minotaur.getChild("body");
        this.torso = this.body.getChild("torso");
        this.top = this.torso.getChild("top");
        this.head = this.top.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition minotaur = partdefinition.addOrReplaceChild("minotaur", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = minotaur.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.125F, -16.0625F, -0.6875F));

        PartDefinition torso = body.addOrReplaceChild("torso", CubeListBuilder.create(), PartPose.offset(0.125F, 1.9375F, 0.8125F));

        PartDefinition top = torso.addOrReplaceChild("top", CubeListBuilder.create(), PartPose.offset(0.25F, -8.875F, -0.125F));

        PartDefinition head = top.addOrReplaceChild("head", CubeListBuilder.create().texOffs(62, 54).addBox(-4.6875F, -8.0448F, -2.959F, 9.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
        .texOffs(60, 242).addBox(-4.6875F, -6.0448F, -2.959F, 9.0F, 6.0F, 8.0F, new CubeDeformation(0.1F))
        .texOffs(64, 20).addBox(-4.6875F, -8.0448F, -3.959F, 9.0F, 2.0F, 9.0F, new CubeDeformation(0.3F))
        .texOffs(18, 217).addBox(-4.6875F, -8.0448F, -3.959F, 9.0F, 2.0F, 9.0F, new CubeDeformation(0.4F)), PartPose.offset(-0.3125F, -13.9552F, -4.041F));

        PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(45, 221).addBox(9.5F, -5.5F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.1F))
        .texOffs(52, 91).addBox(9.5F, -9.5F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(18, 89).addBox(4.5F, -1.5F, -1.5F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(94, 0).addBox(6.5F, -9.5F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1875F, -6.5448F, 0.541F, 0.4363F, 0.2618F, 0.0F));

        PartDefinition cube_r2 = head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 93).addBox(-9.5F, -9.5F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(45, 216).addBox(-11.5F, -5.5F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.1F))
        .texOffs(42, 87).addBox(-11.5F, -9.5F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(60, 85).addBox(-11.5F, -1.5F, -1.5F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1875F, -6.5448F, 0.541F, 0.4363F, -0.2618F, 0.0F));

        PartDefinition brow = head.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(68, 46).addBox(-4.5F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F, new CubeDeformation(0.1F))
        .texOffs(48, 238).addBox(-4.5F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F, new CubeDeformation(0.15F)), PartPose.offset(-0.1875F, -6.0448F, -1.959F));

        PartDefinition hair7 = head.addOrReplaceChild("hair7", CubeListBuilder.create(), PartPose.offset(3.3125F, -8.0448F, 5.041F));

        PartDefinition cube_r3 = hair7.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(62, 70).addBox(-3.5F, 0.0F, -2.0F, 7.0F, 13.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.4363F, 0.0F, 0.0F));

        PartDefinition hair = head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(92, 35).addBox(-2.0F, -1.5F, -1.45F, 2.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.6875F, -4.5448F, 3.491F));

        PartDefinition hair2 = hair.addOrReplaceChild("hair2", CubeListBuilder.create().texOffs(44, 25).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 3.5F, -0.45F));

        PartDefinition hair3 = hair2.addOrReplaceChild("hair3", CubeListBuilder.create().texOffs(94, 4).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -7.0F));

        PartDefinition hair4 = head.addOrReplaceChild("hair4", CubeListBuilder.create().texOffs(76, 92).addBox(0.0F, -1.5F, -1.45F, 2.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(4.3125F, -4.5448F, 3.491F));

        PartDefinition hair5 = hair4.addOrReplaceChild("hair5", CubeListBuilder.create().texOffs(0, 84).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, 3.5F, -0.45F));

        PartDefinition hair6 = hair5.addOrReplaceChild("hair6", CubeListBuilder.create().texOffs(94, 11).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -7.0F));

        PartDefinition hair8 = head.addOrReplaceChild("hair8", CubeListBuilder.create(), PartPose.offset(-3.6875F, -8.0448F, 5.041F));

        PartDefinition cube_r4 = hair8.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(24, 74).addBox(-3.5F, 0.0F, -2.0F, 7.0F, 13.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.4363F, 0.0F, 0.0F));

        PartDefinition ear = head.addOrReplaceChild("ear", CubeListBuilder.create().texOffs(32, 95).addBox(-2.8F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.8875F, -4.5448F, 1.041F));

        PartDefinition ear2 = head.addOrReplaceChild("ear2", CubeListBuilder.create().texOffs(22, 95).addBox(-0.2F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(4.5125F, -4.5448F, 1.041F));

        PartDefinition snout = head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(80, 70).addBox(-3.0F, -1.5F, -4.125F, 6.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
        .texOffs(48, 229).addBox(-3.0F, -1.5F, -4.125F, 6.0F, 4.0F, 5.0F, new CubeDeformation(0.1F))
        .texOffs(68, 50).addBox(-3.0F, 2.5F, -2.125F, 6.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.1875F, -2.5448F, -2.834F));

        PartDefinition nose = snout.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(90, 46).addBox(-3.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(92, 31).addBox(-3.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 1.5F, -4.125F));

        PartDefinition ring = nose.addOrReplaceChild("ring", CubeListBuilder.create().texOffs(17, 224).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.1F, -1.0F, 0.6545F, 0.0F, 0.0F));

        PartDefinition jaw = snout.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(86, 50).addBox(-3.0F, -0.5F, -3.5F, 6.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 3.0F, -1.625F));

        PartDefinition chest = top.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 0).addBox(-10.5F, -6.75F, -3.0F, 21.0F, 14.0F, 11.0F, new CubeDeformation(0.0F))
        .texOffs(0, 25).addBox(-10.5F, -4.75F, -4.0F, 21.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(0, 108).addBox(-10.5F, -4.75F, -4.0F, 21.0F, 9.0F, 1.0F, new CubeDeformation(0.1F))
        .texOffs(0, 200).addBox(-10.5F, -6.75F, -3.0F, 21.0F, 3.0F, 11.0F, new CubeDeformation(0.2F))
        .texOffs(44, 103).addBox(-10.5F, -6.75F, -3.0F, 21.0F, 14.0F, 11.0F, new CubeDeformation(0.1F)), PartPose.offset(-0.5F, -7.25F, -4.0F));

        PartDefinition arm = top.addOrReplaceChild("arm", CubeListBuilder.create().texOffs(97, 100).addBox(-4.0F, -1.05F, -3.5F, 8.0F, 6.0F, 7.0F, new CubeDeformation(0.1F))
        .texOffs(30, 243).addBox(-4.0F, -1.05F, -3.5F, 8.0F, 6.0F, 7.0F, new CubeDeformation(0.2F))
        .texOffs(32, 54).addBox(-4.0F, -1.05F, -3.5F, 8.0F, 13.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(-15.0F, -10.95F, -1.5F));

        PartDefinition forearm = arm.addOrReplaceChild("forearm", CubeListBuilder.create().texOffs(36, 35).addBox(-4.5F, 3.5F, -3.5F, 9.0F, 12.0F, 7.0F, new CubeDeformation(0.0F))
        .texOffs(94, 237).addBox(-4.5F, 3.5F, -3.5F, 9.0F, 12.0F, 7.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 8.45F, 0.0F));

        PartDefinition axe = forearm.addOrReplaceChild("axe", CubeListBuilder.create().texOffs(163, 213).addBox(-0.9143F, -1.0F, -23.5F, 2.0F, 2.0F, 41.0F, new CubeDeformation(0.0F))
        .texOffs(244, 228).addBox(-1.4143F, -1.5F, 15.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(180, 242).addBox(-1.9143F, -2.0F, -24.5F, 4.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
        .texOffs(221, 238).addBox(-0.5143F, -5.0F, -21.5F, 1.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(208, 236).addBox(-0.5143F, -12.0F, -25.5F, 1.0F, 7.0F, 11.0F, new CubeDeformation(0.0F))
        .texOffs(233, 238).addBox(-0.5143F, 2.0F, -21.5F, 1.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(211, 235).addBox(-0.5143F, -11.0F, -28.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(250, 235).addBox(-0.5143F, -8.0F, -28.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(250, 235).addBox(-0.5143F, -8.0F, -12.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(250, 235).addBox(-0.5143F, 7.0F, -12.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(245, 234).addBox(-0.5143F, -11.0F, -14.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(211, 241).addBox(-0.5143F, 8.0F, -14.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(250, 235).addBox(-0.5143F, 7.0F, -28.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(232, 236).addBox(-0.5143F, 5.0F, -25.5F, 1.0F, 7.0F, 11.0F, new CubeDeformation(0.0F))
        .texOffs(245, 241).addBox(-0.5143F, 8.0F, -28.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.4143F, 12.5F, 0.0F));

        PartDefinition arm2 = top.addOrReplaceChild("arm2", CubeListBuilder.create().texOffs(64, 0).addBox(-4.0F, -1.05F, -3.5F, 8.0F, 13.0F, 7.0F, new CubeDeformation(0.0F))
        .texOffs(97, 100).addBox(-4.0F, -1.05F, -3.5F, 8.0F, 6.0F, 7.0F, new CubeDeformation(0.1F))
        .texOffs(0, 243).addBox(-4.0F, -1.05F, -3.5F, 8.0F, 6.0F, 7.0F, new CubeDeformation(0.2F)), PartPose.offset(14.0F, -10.95F, -1.5F));

        PartDefinition forearm2 = arm2.addOrReplaceChild("forearm2", CubeListBuilder.create().texOffs(0, 50).addBox(-4.5F, 3.5F, -3.5F, 9.0F, 12.0F, 7.0F, new CubeDeformation(0.0F))
        .texOffs(126, 237).addBox(-4.5F, 3.5F, -3.5F, 9.0F, 12.0F, 7.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 8.45F, 0.0F));

        PartDefinition lower_body = torso.addOrReplaceChild("lower_body", CubeListBuilder.create().texOffs(0, 35).addBox(-5.5F, -8.75F, -5.25F, 11.0F, 8.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.25F, -0.125F, 0.125F));

        PartDefinition skirt = lower_body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 120).addBox(-7.25F, -1.3F, -4.725F, 11.0F, 1.0F, 7.0F, new CubeDeformation(0.1F)), PartPose.offset(1.75F, -0.45F, -0.525F));

        PartDefinition skirt2 = skirt.addOrReplaceChild("skirt2", CubeListBuilder.create().texOffs(29, 118).addBox(-3.5F, 0.0F, 0.0F, 7.0F, 9.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.75F, -0.2F, -4.775F));

        PartDefinition tail = lower_body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(86, 92).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.25F, 1.75F));

        PartDefinition tail2 = tail.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(96, 54).addBox(-0.25F, -6.5F, -0.55F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.25F, 0.0F, 4.55F));

        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(62, 91).addBox(-1.5F, -1.5F, -0.6F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.1F))
        .texOffs(12, 95).addBox(-1.5F, -1.5F, 3.6F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.1F)), PartPose.offset(0.25F, -5.0F, 0.05F));

        PartDefinition leftLeg = minotaur.addOrReplaceChild("leftLeg", CubeListBuilder.create().texOffs(0, 69).addBox(-3.0F, 0.0F, -3.5F, 6.0F, 9.0F, 6.0F, new CubeDeformation(0.0F))
        .texOffs(24, 228).addBox(-3.0F, 0.0F, -3.5F, 6.0F, 9.0F, 6.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(4.0F, -15.0F, -0.5F, 0.0117F, -0.2615F, -0.0452F));

        PartDefinition leftKnee = leftLeg.addOrReplaceChild("leftKnee", CubeListBuilder.create().texOffs(80, 79).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 9.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(0, 221).addBox(-2.5F, 5.5F, -2.0F, 5.0F, 3.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-0.2F, 6.5F, 1.5F, 0.0F, 0.0F, 0.0436F));

        PartDefinition rightLeg = minotaur.addOrReplaceChild("rightLeg", CubeListBuilder.create().texOffs(68, 31).addBox(-3.0F, 0.0F, -3.5F, 6.0F, 9.0F, 6.0F, new CubeDeformation(0.0F))
        .texOffs(0, 228).addBox(-3.0F, 0.0F, -3.5F, 6.0F, 9.0F, 6.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(-4.0F, -15.0F, -0.5F, 0.0117F, 0.2615F, 0.0452F));

        PartDefinition rightKnee = rightLeg.addOrReplaceChild("rightKnee", CubeListBuilder.create().texOffs(42, 74).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 9.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(0, 214).addBox(-2.5F, 5.5F, -2.0F, 5.0F, 3.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.2F, 6.5F, 1.5F, 0.0F, 0.0F, -0.0436F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    @Override
    public void setupAnim(DeluxeEntityRenderState state) {
        RIG.apply(state, this, AnimContext.from(state));
    }
}
