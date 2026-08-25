package net.darkblade.mythosmortals.entity.owl.client;

import net.darkblade.mythosmortals.core.MythosMortals;
import net.darkblade.deluxelib.block.StatueConfig;
import net.darkblade.mythosmortals.entity.owl.client.render.CopperOwlModel;
import net.darkblade.mythosmortals.entity.owl.client.render.OwlAnimation;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

import java.util.Map;


public final class OwlStatueClient {
    private static final float TEXELS_PER_BLOCK = 16.0F;

    public static final StatueConfig CONFIG = new StatueConfig(
            "owl",
            CopperOwlModel.LAYER_LOCATION,
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/copper_owl.png"),
            OwlAnimation.UNACTIVE,
            0.50F, 1.36F, 0.50F, 0.90F,
            Map.of(
                    ItemDisplayContext.GUI, tf(200, 705, 0, 3.5F, 6.5F, -0.5F, 0.57F),
                    ItemDisplayContext.GROUND, tf(165, 0, 0, 4.0F, 7.5F, -10.0F, 0.60F),
                    ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, tf(-200, -315, 0, 4.0F, 7.0F, -0.5F, 0.40F),
                    ItemDisplayContext.FIRST_PERSON_LEFT_HAND, tf(-200, 390, 0, -2.0F, 5.5F, -5.0F, 0.40F),
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, tf(240, 185, 0, -5.5F, 2.5F, 6.5F, 0.63F),
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND, tf(240, -185, 0, 5.5F, 2.5F, 6.5F, 0.63F)));

    private static ItemTransform tf(float rx, float ry, float rz, float tx, float ty, float tz, float scale) {
        return new ItemTransform(
                new Vector3f(rx, ry, rz),
                new Vector3f(tx / TEXELS_PER_BLOCK, ty / TEXELS_PER_BLOCK, tz / TEXELS_PER_BLOCK),
                new Vector3f(scale, scale, scale));
    }

    private OwlStatueClient() {}
}
