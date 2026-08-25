package net.darkblade.mythosmortals.entity.pegasus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;
import net.darkblade.mythosmortals.core.MythosMortals;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PegasusEquipmentLayer extends RenderLayer<DeluxeEntityRenderState, PegasusModel> {

    private static final Identifier BRIDLE = texture("bridle_layer");
    private static final Identifier SADDLE = texture("saddle_layer");

    private static final Identifier[] ARMOR = {
            null,
            texture("iron_armor_layer"),
            texture("gold_armor_layer"),
            texture("diamond_armor_layer"),
            texture("netherite_armor_layer"),
    };

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/pegasus/" + name + ".png");
    }

    public PegasusEquipmentLayer(RenderLayerParent<DeluxeEntityRenderState, PegasusModel> parent) {
        super(parent);
    }

    @Override
    public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector collector,
                       int lightCoords, @NotNull DeluxeEntityRenderState state,
                       float yRot, float xRot) {
        if (state.isInvisible || !(state instanceof PegasusRenderState pegasus)) {
            return;
        }
        if (pegasus.hasBridle) {
            this.submitLayer(poseStack, collector, lightCoords, pegasus, BRIDLE);
        }
        if (pegasus.hasSaddle) {
            this.submitLayer(poseStack, collector, lightCoords, pegasus, SADDLE);
        }
        Identifier armor = pegasus.armorTier > 0 && pegasus.armorTier < ARMOR.length ? ARMOR[pegasus.armorTier] : null;
        if (armor != null) {
            this.submitLayer(poseStack, collector, lightCoords, pegasus, armor);
        }
    }

    private void submitLayer(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                             PegasusRenderState state, @Nullable Identifier texture) {
        if (texture == null) {
            return;
        }
        collector.order(1).submitModel(
                this.getParentModel(),
                state,
                poseStack,
                RenderTypes.entityCutout(texture),
                lightCoords,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                state.outlineColor,
                null);
    }
}
