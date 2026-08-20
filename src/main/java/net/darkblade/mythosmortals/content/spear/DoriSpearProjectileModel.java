package net.darkblade.mythosmortals.content.spear;

import net.darkblade.mythosmortals.MythosMortals;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * Static (non-animated) geometry for the thrown Dori spear, ported from a Blockbench export.
 * Rendered via {@link net.minecraft.client.model.Model.Simple} (see {@link net.darkblade.deluxelib.client.render.ThrownWeaponRenderer}) —
 * no dedicated {@code Model} subclass is needed since the geometry never changes at runtime.
 */
public final class DoriSpearProjectileModel {
    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "thrown_dori_spear"), "main");

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // The Blockbench export's original offset (1.6, 10.5, -8.0) was authored for holding this
        // model relative to a rig's hand bone, not for a standalone entity centered on its own
        // origin (compare TridentModel's pole, which uses PartPose.ZERO). Starting from zero here
        // instead — nudge these two values if it still needs a small correction once seen in-game.
        PartDefinition spear = root.addOrReplaceChild("spear", CubeListBuilder.create()
                .texOffs(8, 0).addBox(5.8F, -17.5F, -1.0F, 0.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(5.4F, -14.5F, -1.0F, 1.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 11).addBox(5.4F, -8.5F, 0.0F, 1.0F, 32.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 8).addBox(4.4F, -9.5F, -1.0F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
            PartPose.ZERO);

        spear.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                .texOffs(4, 9).addBox(-0.7F, -2.0F, -1.5F, 0.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(6.4F, 25.5F, 0.0F, 0.0F, 0.7854F, 0.0F));

        spear.addOrReplaceChild("cube_r2", CubeListBuilder.create()
                .texOffs(7, 9).addBox(-0.7F, -2.0F, 0.0F, 0.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(7.4F, 25.5F, 0.0F, 0.0F, -0.7854F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    private DoriSpearProjectileModel() {}
}
