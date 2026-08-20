package net.darkblade.mythosmortals.content.owl;
import net.darkblade.mythosmortals.MythosMortals;

import com.mojang.blaze3d.vertex.PoseStack;
import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;
import net.darkblade.deluxelib.client.render.FlyingMobRenderer;
import net.darkblade.mythosmortals.content.owl.perch.OwlPerchPlacement;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer for the Copper Owl, using its own {@link CopperOwlModel} and texture.
 */
public class OwlRenderer extends FlyingMobRenderer<OwlEntity, CopperOwlModel> {

    /** {@link CopperOwlModel} is authored at its own native scale, unlike the borrowed-arpy
     * placeholder this used to be — tuned in-game via {@code /deluxelib debug owlperch} OWL mode
     * (7/1) against the owl's {@code sized(0.6F, 0.6F)} footprint (see MythosMortalsRegistry).
     *
     * <p>The value itself lives in {@link OwlPerchPlacement}: the perched bird needs the same scale
     * handed to the library inside its {@code PerchPlacement}, and that path is common-side, so it
     * cannot read the constant from this client-only class. {@code PerchClient} then reapplies it by
     * hand — drawing the owl on its host's arm goes through {@code RiderPassengerLayer.submitRider},
     * which deliberately bypasses {@code setupRotations}, so the scale below never runs on that
     * path. */
    public static final float OWL_MODEL_SCALE = OwlPerchPlacement.MODEL_SCALE;

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/copper_owl.png");

    public OwlRenderer(EntityRendererProvider.Context context) {
        super(context, new CopperOwlModel(context.bakeLayer(CopperOwlModel.LAYER_LOCATION)), 0.25f);
        this.addLayer(new OwlEyeGlowLayer(this));
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull DeluxeEntityRenderState state) {
        return TEXTURE;
    }

    /** Rides the perch flag across to the render state: {@code CopperOwlModel}'s rig gates its idle
     *  head motion on it, and a {@code RigComponent} never sees the entity. */
    @Override
    public void extractRenderState(@NotNull OwlEntity entity, @NotNull DeluxeEntityRenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.perched = entity.isPerched();
    }

    @Override
    protected void setupRotations(@NotNull DeluxeEntityRenderState state, @NotNull PoseStack poseStack,
                                  float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        poseStack.scale(OWL_MODEL_SCALE, OWL_MODEL_SCALE, OWL_MODEL_SCALE);
    }
}
