package net.darkblade.mythosmortals.content.spartan;

import net.darkblade.mythosmortals.MythosMortals;
import net.darkblade.mythosmortals.content.athenian.AthenianHelmetInteriorModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

public final class SpartanHelmetInteriorModel {
    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "spartan_helmet_interior"), "main");

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("helmet", CubeListBuilder.create()
                .texOffs(0, 15).addBox(-12.0F, 22.0F, -0.2F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.5F))
                .texOffs(0, -14).addBox(-8.0F, 11.0F, -0.2F, 0.0F, 15.0F, 14.0F, new CubeDeformation(0.0F))
                .texOffs(0, 27).addBox(-9.0F, 15.0F, -0.2F, 2.0F, 8.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(28, 0).addBox(-8.5F, 13.0F, -0.2F, 1.0F, 11.0F, 12.0F, new CubeDeformation(0.0F))
                .texOffs(0, 55).addBox(-12.0F, 17.0F, -0.2F, 8.0F, 8.0F, 1.0F, new CubeDeformation(0.5F))
                .texOffs(26, 55).addBox(-12.0F, 17.0F, 6.8F, 8.0F, 8.0F, 1.0F, new CubeDeformation(0.5F))
                .texOffs(28, 29).addBox(-12.0F, 17.0F, 6.2F, 8.0F, 8.0F, 1.0F, new CubeDeformation(0.5F))
                .texOffs(9, 48).addBox(-5.0F, 17.0F, -0.2F, 1.0F, 8.0F, 8.0F, new CubeDeformation(0.5F))
                .texOffs(46, 22).addBox(-5.1F, 17.0F, -0.2F, 1.0F, 8.0F, 8.0F, new CubeDeformation(0.5F))
                .texOffs(32, 47).addBox(-12.0F, 17.0F, -0.2F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.5F))
                .texOffs(42, 0).addBox(-12.0F, 17.5F, -0.2F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.5F))
                .texOffs(32, 38).addBox(-12.0F, 24.0F, -0.2F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.5F)),
            PartPose.offset(8.0F, -4.0F, -6.8F));

        root.addOrReplaceChild("helmet2", CubeListBuilder.create()
                .texOffs(9, 48).mirror().addBox(4.0F, 17.0F, -0.2F, 1.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)).mirror(false)
                .texOffs(46, 22).mirror().addBox(4.1F, 17.0F, -0.2F, 1.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)).mirror(false),
            PartPose.offset(-8.0F, -4.0F, -6.8F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    private SpartanHelmetInteriorModel() {}
}
