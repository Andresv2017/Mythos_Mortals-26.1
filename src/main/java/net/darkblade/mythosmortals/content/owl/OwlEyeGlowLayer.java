package net.darkblade.mythosmortals.content.owl;
import net.darkblade.mythosmortals.MythosMortals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.darkblade.deluxelib.client.render.DeluxeEntityRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * Halo de luz en cada ojo del Copper Owl.
 *
 * <p>Un quad por ojo, coplanar con la cara, con una rampa de color horneada en la textura (núcleo
 * casi blanco → verdigrís → teal profundo) y blending aditivo vía {@link OwlEyeGlowRenderType}. El
 * color va en el PNG y no como tinte: un tinte único sobre una textura blanca da el mismo matiz en
 * todo el halo, y eso es exactamente lo que hace que un glow se lea plano.
 *
 * <p>Coplanar y no billboard, pese a que el billboard llegó a estar implementado y funcionando. Se
 * probaron los dos in-game y el plano gana claro, por una razón que no es un bug: un billboard no
 * se escorza. Al mirar al búho en ángulo la cara se comprime pero el halo sigue siendo un círculo
 * del mismo tamaño, así que se despega de la cabeza y se lee como una calcomanía encima. El quad
 * coplanar se comprime con la cara y por eso parece parte del búho. Que desaparezca de perfil no
 * es problema: el ojo, que también es un quad de profundidad 0, desaparece con él.
 *
 * <p>Usa {@code submitCustomGeometry} y no {@code submitModel} por una diferencia que importa: el
 * collector guarda {@code poseStack.last().copy()}, una instantánea real de la matriz, mientras que
 * {@code submitModel} guarda la referencia al modelo y le re-corre {@code setupAnim} en el flush.
 * Con la instantánea, caminar la jerarquía aquí dentro es correcto.
 */
public class OwlEyeGlowLayer extends RenderLayer<DeluxeEntityRenderState, CopperOwlModel> {

    /** Un único halo de 128x128 que ocupa la imagen entera, con su {@code .mcmeta} pidiendo
     *  {@code blur} y {@code clamp}. Ya no vive dentro del atlas 64x64 del búho: es una textura
     *  aparte con su propio sampler, así que no hereda ni su tamaño ni su filtrado nearest. Eso es
     *  lo que quita el escalonado cuando el búho va en la percha y se ve de cerca. */
    private static final Identifier GLOW_TEXTURE =
            Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/copper_owl_glow.png");

    /** Semiancho del halo, en unidades de modelo (las mismas de {@code addBox}), para que sea
     *  comparable a simple vista con el resto del rig: el ojo mide 3x2 y la cabeza 7 de ancho.
     *
     *  <p>Los ojos están a 4 unidades entre sí, así que a partir de ~2.0 los dos halos se solapan en
     *  el centro y —con blending aditivo— esa zona suma. Puede quedar como un puente de luz sobre el
     *  pico o como un blob único que borra los dos ojos: es la primera perilla a mover.
     *
     *  <p>Se divide por {@link ModelPart.Vertex#SCALE_FACTOR} al emitir. No es opcional: todo
     *  {@code ModelPart} divide sus coordenadas por 16 ({@code translateAndRotate} lo hace con la
     *  traslación y {@code Vertex.worldX} con los vértices), así que tras {@code applyEyeTransform}
     *  el espacio del pose está en bloques. Emitir el quad en crudo lo hacía 16 veces más grande. */
    private static final float HALF_SIZE = 3.0F;

    /** Cuánto baja el halo respecto del centro de la pupila, en unidades de modelo ({@code +Y} es
     *  hacia abajo). Medio píxel: la pupila mide 1, así que esto deja el halo exactamente en su
     *  <b>borde inferior</b>.
     *
     *  <p>Ese punto no es arbitrario. La pupila ocupa la mitad superior del ojo en los dos ojos
     *  ({@code eye} va de -1.00 a 1.00 con la pupila en -1.00..0.00; {@code eye2} de -0.75 a 1.25 con
     *  la pupila en -0.75..0.25), así que su borde inferior coincide con el centro vertical de la
     *  caja del ojo. Es a la vez "abajo de la pupila" y "en medio del ojo".
     *
     *  <p>Centrar en la pupila a secas deja el halo visiblemente alto, porque la pupila es 1px dentro
     *  de un ojo de 2px. Es una decisión estética y por eso vive aquí: {@code applyEyeTransform} sigue
     *  dejando el pose sobre la pupila, que es el hecho derivable de los huesos. */
    private static final float VERTICAL_NUDGE = 0.5F;

    /** Debajo de esto el ojo está prácticamente cerrado y el quad no aporta nada. */
    private static final float BLINK_EPSILON = 0.01F;

    /** Centro y amplitud del alpha del latido. Verificado in-game: por debajo de ~4x de ratio el
     *  ojo compensa el cambio y no se percibe latido alguno. */
    private static final float ALPHA_BASE = 160.0F;
    private static final float ALPHA_SWING = 95.0F;

    /** Período del latido, en ticks. 52 ≈ 2.6 s: un respirar lento, no un parpadeo nervioso. */
    private static final float PULSE_PERIOD_TICKS = 52.0F;
    private static final float PULSE_FREQ = (float) (2.0 * Math.PI / PULSE_PERIOD_TICKS);


    public OwlEyeGlowLayer(RenderLayerParent<DeluxeEntityRenderState, CopperOwlModel> parent) {
        super(parent);
    }

    @Override
    public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector collector,
                       int lightCoords, @NotNull DeluxeEntityRenderState state,
                       float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }

        // ageInTicks ya trae los partial ticks, así que el latido es suave a cualquier framerate.
        // Es un pulso de brillo, no de tamaño: el alpha multiplica el degradado entero por el mismo
        // factor, así que no mueve la frontera visible del halo — solo su intensidad.
        float pulse = ALPHA_BASE + ALPHA_SWING * Mth.sin(state.ageInTicks * PULSE_FREQ);
        RenderType renderType = OwlEyeGlowRenderType.get(GLOW_TEXTURE);
        CopperOwlModel model = this.getParentModel();

        for (CopperOwlModel.Eye eye : CopperOwlModel.Eye.values()) {
            // El halo ya no es hijo de "eye", así que el parpadeo hay que leerlo y aplicarlo a mano.
            float blink = model.eyeScale(eye);
            if (blink <= BLINK_EPSILON) {
                continue;
            }

            poseStack.pushPose();
            model.applyEyeTransform(poseStack, eye);

            // El nudge va aquí, en los ejes del ojo, para que siga a la cabeza cuando el búho la gira.
            poseStack.translate(0.0F, VERTICAL_NUDGE / ModelPart.Vertex.SCALE_FACTOR, 0.0F);

            float half = HALF_SIZE * blink / ModelPart.Vertex.SCALE_FACTOR;
            int alpha = Mth.clamp(Math.round(pulse * blink), 0, 255);
            int color = (alpha << 24) | 0xFFFFFF;

            collector.order(1).submitCustomGeometry(poseStack, renderType,
                    (pose, buffer) -> quad(pose, buffer, half, color, lightCoords));

            poseStack.popPose();
        }
    }

    /**
     * Emite el quad en el plano XY del ojo, o sea coplanar con la cara. El winding no importa: el
     * pipeline lleva {@code withCull(false)}.
     *
     * <p>La textura es un único halo que ocupa la imagen entera, así que ambos ojos usan el rango UV
     * completo.
     */
    private static void quad(PoseStack.Pose pose, VertexConsumer buffer,
                             float half, int color, int light) {
        // Un solo scratch reutilizado para los cuatro vértices, igual que ModelPart.Cube.compile:
        // esto corre por ojo, por búho, por frame.
        Vector3f scratch = new Vector3f();
        vertex(pose, buffer, scratch, -half, -half, 0.0F, 1.0F, color, light);
        vertex(pose, buffer, scratch, -half, half, 0.0F, 0.0F, color, light);
        vertex(pose, buffer, scratch, half, half, 1.0F, 0.0F, color, light);
        vertex(pose, buffer, scratch, half, -half, 1.0F, 1.0F, color, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vector3f scratch,
                               float x, float y, float u, float v, int color, int light) {
        Vector3f p = pose.pose().transformPosition(x, y, 0.0F, scratch);
        buffer.addVertex(p.x(), p.y(), p.z(), color, u, v, OverlayTexture.NO_OVERLAY, light, 0.0F, 0.0F, -1.0F);
    }
}
