package net.darkblade.mythosmortals.content.spartan;
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

public class SpartanModel extends EntityModel<DeluxeEntityRenderState> {

	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(MythosMortals.MODID, "spartan"), "main");

	private static final Rig<SpartanModel> RIG =
			Rig.<SpartanModel>builder()
					.resetPoses()
					.keyframeBlend(200L, 0)
					.lookAt(m -> m.head, 40f, 30f)
					.build();

	private final ModelPart base;
	private final ModelPart spartan;
	private final ModelPart waist;
	public final ModelPart head;
	private final ModelPart helmet;
	private final ModelPart body;
	private final ModelPart skirt;
	private final ModelPart skirt_front;
	private final ModelPart skirt_back;
	private final ModelPart cape;
	private final ModelPart right_arm;
	private final ModelPart xifos;
	private final ModelPart left_arm;
	private final ModelPart shield;
	private final ModelPart right_leg;
	private final ModelPart left_leg;

	public SpartanModel(ModelPart root) {
		// root() must return the LAYER root (not the "base" bone): getAnyDescendantWithName finds a
		// bone by searching root()'s subtree for its parent, so if root() were "base" itself the
		// death animation's "base" channels would silently never resolve. Athenian already does this.
		super(root);
		this.base = root.getChild("base");
		this.spartan = this.base.getChild("spartan");
		this.waist = this.spartan.getChild("waist");
		this.head = this.waist.getChild("head");
		this.helmet = this.head.getChild("helmet");
		this.body = this.waist.getChild("body");
		this.skirt = this.body.getChild("skirt");
		this.skirt_front = this.skirt.getChild("skirt_front");
		this.skirt_back = this.skirt.getChild("skirt_back");
		this.cape = this.body.getChild("cape");
		this.right_arm = this.waist.getChild("right_arm");
		this.xifos = this.right_arm.getChild("xifos");
		this.left_arm = this.waist.getChild("left_arm");
		this.shield = this.left_arm.getChild("shield");
		this.right_leg = this.spartan.getChild("right_leg");
		this.left_leg = this.spartan.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		// Named "base", NOT "root" — see the constructor note and AthenianModel.createBodyLayer.
		PartDefinition root = partdefinition.addOrReplaceChild("base", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

		PartDefinition spartan = root.addOrReplaceChild("spartan", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition waist = spartan.addOrReplaceChild("waist", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition head = waist.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(64, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.1F))
				.texOffs(52, 17).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition helmet = head.addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(96, 0).addBox(-4.0F, 5.0F, -5.2F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.5F))
				.texOffs(56, 6).addBox(0.0F, -10.0F, -5.2F, 0.0F, 15.0F, 14.0F, new CubeDeformation(0.0F))
				.texOffs(86, 12).addBox(-1.0F, -6.0F, -5.2F, 2.0F, 8.0F, 10.0F, new CubeDeformation(0.0F))
				.texOffs(98, 18).addBox(-0.5F, -8.0F, -5.2F, 1.0F, 11.0F, 12.0F, new CubeDeformation(0.0F))
				.texOffs(32, 0).addBox(-4.0F, -4.0F, -5.2F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, -4.0F, 1.2F));

		PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F))
				.texOffs(56, 40).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.4F)), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create(), PartPose.offset(0.0F, 12.225F, 0.0F));

		PartDefinition skirt_front = skirt.addOrReplaceChild("skirt_front", CubeListBuilder.create().texOffs(56, 35).addBox(-4.0F, 0.275F, -0.5F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, 0.0F, -1.5F));

		PartDefinition skirt_back = skirt.addOrReplaceChild("skirt_back", CubeListBuilder.create().texOffs(56, 35).addBox(-4.0F, 0.275F, -0.5F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0.0F, 0.0F, 1.5F, 0.0F, 3.1416F, 0.0F));

		PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(110, 41).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 18.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 2.0F));

		PartDefinition right_arm = waist.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offset(-5.0F, -10.0F, 0.0F));

		PartDefinition xifos = right_arm.addOrReplaceChild("xifos", CubeListBuilder.create().texOffs(110, 120).addBox(-0.5F, -0.5F, -1.7F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(124, 121).addBox(-0.5F, -1.5F, -2.7F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(104, 122).addBox(-0.5F, -1.0F, -6.7F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(114, 119).addBox(-0.5F, -1.5F, -11.7F, 1.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(122, 115).addBox(0.0F, -1.5F, -14.7F, 0.1F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 8.5F, -1.3F));

		PartDefinition left_arm = waist.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offset(5.0F, -10.0F, 0.0F));

		PartDefinition shield = left_arm.addOrReplaceChild("shield", CubeListBuilder.create().texOffs(0, 96).addBox(-0.5F, -8.0F, -8.0F, 1.0F, 16.0F, 16.0F, new CubeDeformation(0.0F))
				.texOffs(34, 102).addBox(-0.5F, 8.0F, -6.0F, 1.0F, 1.0F, 12.0F, new CubeDeformation(0.0F))
				.texOffs(18, 103).addBox(-0.5F, 9.0F, -4.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(12, 99).addBox(-0.5F, -6.0F, 8.0F, 1.0F, 12.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(4, 103).addBox(-0.5F, -4.0F, 9.0F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(8, 99).addBox(-0.5F, -6.0F, -9.0F, 1.0F, 12.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 103).addBox(-0.5F, -4.0F, -10.0F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(34, 115).addBox(-0.5F, -9.0F, -6.0F, 1.0F, 1.0F, 12.0F, new CubeDeformation(0.0F))
				.texOffs(18, 94).addBox(-0.5F, -10.0F, -4.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.5F, 7.0F, 0.0F, -0.7854F, 0.0F, 0.0F));

		PartDefinition right_leg = spartan.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.22F)), PartPose.offset(-1.9F, 0.0F, 0.0F));

		PartDefinition left_leg = spartan.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.22F)), PartPose.offset(1.9F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(DeluxeEntityRenderState state) {
		RIG.apply(state, this, AnimContext.from(state));
	}
}