package net.darkblade.mythosmortals.entity.arpy.client.render;
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
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class ArpyModel extends EntityModel<DeluxeEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "arpy"), "main");

    private static final Rig<ArpyModel> RIG =
            Rig.<ArpyModel>builder()
                    .resetPoses()
                    .keyframeBlend(200L, 0)
                    .lookAt(m -> m.head, 30f, 20f)
                    .build();

    private final ModelPart arpy;
    public final ModelPart head;

    public ArpyModel(ModelPart root) {
        super(root);
        this.arpy = root.getChild("arpy");
        this.head = this.arpy.getChild("body").getChild("top").getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition arpy = partdefinition.addOrReplaceChild("arpy", CubeListBuilder.create(), PartPose.offset(0.0F, 12.9167F, -0.3333F));
        PartDefinition body = arpy.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition top = body.addOrReplaceChild("top", CubeListBuilder.create(), PartPose.offset(0.0F, -0.4167F, 1.3333F));

        PartDefinition head = top.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-4.0F, -8.0F, -6.75F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(96, 112).addBox(-4.0F, -8.0F, -6.75F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.1F))
                .texOffs(0, 81).addBox(-5.0F, -6.0F, -6.75F, 10.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(64, 11).addBox(-4.0F, -8.0F, 1.25F, 8.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -11.5F, -1.25F));

        head.addOrReplaceChild("eyes", CubeListBuilder.create()
                .texOffs(15, 83).addBox(-4.0F, -1.5F, 0.0F, 8.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -4.5F, -6.65F));

        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 83).addBox(-3.0F, -0.5F, -1.5F, 6.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -4.5F, -5.15F));

        jaw.addOrReplaceChild("teeth", CubeListBuilder.create()
                .texOffs(-2, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.5F, -1.5F, 1.5708F, 0.0F, 0.0F));
        jaw.addOrReplaceChild("teeth2", CubeListBuilder.create()
                .texOffs(-2, 50).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 3.5F, -1.5F, 1.5708F, 0.0F, 0.0F));

        top.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-3.0F, -6.0F, -2.0F, 6.0F, 6.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(106, 101).addBox(-3.0F, -6.0F, -2.0F, 6.0F, 6.0F, 5.0F, new CubeDeformation(0.1F))
                .texOffs(24, 44).addBox(0.0F, -14.0F, 3.0F, 0.0F, 7.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(32, 48).addBox(-4.0F, -13.0F, -3.0F, 8.0F, 7.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(66, 114).addBox(-4.0F, -13.0F, -3.0F, 8.0F, 7.0F, 7.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 1.5F, -1.0F));

        PartDefinition arm = top.addOrReplaceChild("arm", CubeListBuilder.create()
                .texOffs(62, 48).addBox(3.0F, -7.0F, -1.0F, 10.0F, 9.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(48, 73).addBox(0.0F, -2.0F, -1.0F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(24, 69).addBox(3.0F, 2.0F, 0.0F, 10.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(4.0F, -9.5F, 1.0F));
        arm.addOrReplaceChild("wing", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, -3.0F, 0.0F, 32.0F, 17.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(0, 34).addBox(0.0F, -3.0F, -1.0F, 17.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(13.0F, -5.0F, 1.0F));

        PartDefinition arm2 = top.addOrReplaceChild("arm2", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-13.0F, -7.0F, -1.0F, 10.0F, 9.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 75).addBox(-3.0F, -2.0F, -1.0F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(44, 69).addBox(-13.0F, 2.0F, 0.0F, 10.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-4.0F, -9.5F, 1.0F));
        arm2.addOrReplaceChild("wing2", CubeListBuilder.create()
                .texOffs(0, 17).addBox(-32.0F, -3.0F, 0.0F, 32.0F, 17.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(38, 34).addBox(-17.0F, -3.0F, -1.0F, 17.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-13.0F, -5.0F, 1.0F));

        PartDefinition leg2 = body.addOrReplaceChild("leg2", CubeListBuilder.create()
                .texOffs(66, 59).addBox(-2.125F, -1.125F, -2.0F, 3.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.375F, 0.2083F, -0.6667F, 0.0F, 0.5236F, 0.0436F));
        PartDefinition knee2 = leg2.addOrReplaceChild("knee2", CubeListBuilder.create()
                .texOffs(72, 69).addBox(-2.25F, -1.25F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(90, 103).addBox(-2.25F, -1.25F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.1F)),
                PartPose.offsetAndRotation(0.625F, 4.125F, 2.0F, 0.0F, 0.0F, -0.0436F));
        PartDefinition foot2 = knee2.addOrReplaceChild("foot2", CubeListBuilder.create()
                .texOffs(36, 73).addBox(-1.5543F, -0.0278F, -2.3237F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.25F, 5.75F, 0.0F));

        foot2.addOrReplaceChild("bone6", CubeListBuilder.create()
                .texOffs(28, 64).addBox(-0.5F, -1.5F, 0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(24, 67).addBox(-0.5F, -1.5F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(62, 123).addBox(-0.5F, -1.5F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.02F))
                .texOffs(58, 73).addBox(-0.5F, -1.5F, 2.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(58, 125).addBox(-0.5F, -1.5F, 2.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.1F)),
                PartPose.offset(-0.0543F, 0.4722F, 0.1763F));

        PartDefinition bone7 = foot2.addOrReplaceChild("bone7", CubeListBuilder.create(), PartPose.offset(-1.0543F, 0.4722F, -1.8237F));
        bone7.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                .texOffs(62, 125).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.1F))
                .texOffs(76, 44).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(62, 123).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.02F))
                .texOffs(34, 77).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(76, 41).addBox(-0.5F, -1.5F, -1.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F));

        PartDefinition bone8 = foot2.addOrReplaceChild("bone8", CubeListBuilder.create(), PartPose.offset(0.9457F, 0.4722F, -1.8237F));
        bone8.addOrReplaceChild("cube_r2", CubeListBuilder.create()
                .texOffs(62, 123).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.02F))
                .texOffs(38, 77).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(62, 125).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.1F))
                .texOffs(22, 77).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(58, 76).addBox(-0.5F, -1.5F, -1.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

        foot2.addOrReplaceChild("bone9", CubeListBuilder.create()
                .texOffs(26, 77).addBox(-0.5F, -1.5F, -1.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(30, 77).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(62, 125).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.1F))
                .texOffs(42, 77).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(62, 123).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.02F)),
                PartPose.offset(-0.0543F, 0.4722F, -1.8237F));

        PartDefinition leg = body.addOrReplaceChild("leg", CubeListBuilder.create()
                .texOffs(64, 22).addBox(-0.875F, -1.125F, -2.0F, 3.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.375F, 0.2083F, -0.6667F, 0.0F, -0.5236F, -0.0436F));
        PartDefinition knee = leg.addOrReplaceChild("knee", CubeListBuilder.create()
                .texOffs(64, 69).addBox(0.25F, -1.25F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(98, 103).addBox(0.25F, -1.25F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.1F)),
                PartPose.offsetAndRotation(-0.625F, 4.125F, 2.0F, 0.0F, 0.0F, 0.0436F));
        PartDefinition foot = knee.addOrReplaceChild("foot", CubeListBuilder.create()
                .texOffs(24, 73).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.25F, 5.75F, 0.0F));

        PartDefinition bone2 = foot.addOrReplaceChild("bone2", CubeListBuilder.create(), PartPose.offset(1.0F, 0.5F, -1.5F));
        bone2.addOrReplaceChild("cube_r3", CubeListBuilder.create()
                .texOffs(62, 125).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.1F))
                .texOffs(76, 32).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(62, 123).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.02F))
                .texOffs(28, 67).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(10, 75).addBox(-0.5F, -1.5F, -1.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

        PartDefinition bone4 = foot.addOrReplaceChild("bone4", CubeListBuilder.create(), PartPose.offset(-1.0F, 0.5F, -1.5F));
        bone4.addOrReplaceChild("cube_r4", CubeListBuilder.create()
                .texOffs(62, 123).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.02F))
                .texOffs(72, 32).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(62, 125).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.1F))
                .texOffs(76, 38).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(14, 75).addBox(-0.5F, -1.5F, -1.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F));

        foot.addOrReplaceChild("bone3", CubeListBuilder.create()
                .texOffs(18, 75).addBox(-0.5F, -1.5F, -1.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(76, 35).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(62, 125).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.1F))
                .texOffs(68, 32).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(62, 123).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.02F)),
                PartPose.offset(0.0F, 0.5F, -1.5F));

        foot.addOrReplaceChild("bone5", CubeListBuilder.create()
                .texOffs(62, 59).addBox(-0.5F, -1.5F, 0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(64, 32).addBox(-0.5F, -1.5F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(62, 123).addBox(-0.5F, -1.5F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.02F))
                .texOffs(24, 64).addBox(-0.5F, -1.5F, 2.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(58, 125).addBox(-0.5F, -1.5F, 2.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 0.5F, 0.5F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(@NotNull DeluxeEntityRenderState state) {
        RIG.apply(state, this, AnimContext.from(state));
    }
}
