package net.darkblade.mythosmortals.content.owl;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.darkblade.mythosmortals.MythosMortals;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.function.Function;

/**
 * Render type aditivo para el halo de ojos del Copper Owl.
 *
 * <p>Clona {@code RenderPipelines.EYES} cambiando <em>solo</em> el blending, de
 * {@link BlendFunction#TRANSLUCENT} a {@link BlendFunction#LIGHTNING}. No hay GLSL propio: reusa
 * los shaders {@code core/entity} de vanilla con sus mismos defines.
 *
 * <p>Por qué {@code LIGHTNING} y no las opciones de vanilla: es
 * {@code (SourceFactor.SRC_ALPHA, DestFactor.ONE)}, o sea aditivo <em>escalado por alpha</em>. Suma
 * luz sobre el fondo como un aditivo puro — que es lo que hace que un glow se lea como luz y no como
 * calcomanía — pero el alpha sigue mandando, así que el latido del halo sobrevive.
 * {@code RenderTypes.eyes()} usa {@code TRANSLUCENT}, que no suma luz;
 * {@code RenderTypes.energySwirl()} usa {@code ADDITIVE}, que es {@code (ONE, ONE)} e ignora el
 * alpha, lo que mataría el pulso. Ninguna fábrica de vanilla combina {@code LIGHTNING} con una
 * textura de entidad, de ahí el pipeline propio.
 *
 * <p>Se clona el snippet mínimo de {@code EYES} ({@code MATRICES_FOG_SNIPPET}) a propósito, en vez
 * del más gordo {@code ENTITY_SNIPPET}: con el define {@code EMISSIVE} el shader no declara
 * {@code Sampler2}, así que declararlo en el pipeline sería un desajuste que revienta en runtime.
 *
 * <p>{@code withCull(false)} porque el halo es un quad plano generado en código: si el winding sale
 * al revés, el backface culling lo haría invisible sin ningún error. Desactivarlo elimina toda esa
 * clase de fallo.
 *
 * <p>{@code CompareOp.ALWAYS_PASS} — el halo no testea profundidad. El billboard es un plano que
 * pasa por el ojo, así que en cuanto se mira al búho en ángulo parte de su propia cabeza queda más
 * cerca de la cámara y lo secciona con un corte diagonal duro. Peor todavía: un halo recortado
 * asimétricamente <em>parece descentrado</em>, porque solo se ve un trozo y su centro visual se
 * corre — al orbitar la cabeza recorta una porción distinta y el halo aparenta bailar sobre la cara.
 *
 * <p>Empujar el quad hacia la cámara solo pospone el problema: siempre queda un ángulo que lo
 * derrota. Quitar el test lo elimina de raíz. El precio es que el halo se ve a través de bloques
 * cuando el búho está detrás de algo; en un aura de ojos eso se lee como intencional, y es el
 * intercambio que casi todos los mods de glow aceptan.
 *
 * <p>{@code writeDepth} sigue en {@code false}: el halo tampoco debe ocluir a nada.
 *
 * @see #USE_VANILLA_FALLBACK
 */
@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class OwlEyeGlowRenderType {

    /**
     * Interruptor de emergencia para compatibilidad con shaderpacks.
     *
     * <p>Iris/OptiFine reconocen {@code RenderTypes.eyes()} por nombre y lo tratan como pase
     * emisivo. Este pipeline es distinto, así que ese reconocimiento no está garantizado. Si con
     * shaderpack el halo se pone opaco, negro o desaparece, poner esto en {@code true} vuelve al
     * render type de vanilla: se pierde el punch del aditivo y nada más — el resto del diseño
     * (rampa de color, billboard, latido, parpadeo) sobrevive intacto.
     */
    public static final boolean USE_VANILLA_FALLBACK = false;

    private static final RenderPipeline ADDITIVE_GLOW = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MythosMortals.MODID, "pipeline/owl_eye_glow"))
            .withVertexShader("core/entity")
            .withFragmentShader("core/entity")
            .withShaderDefine("EMISSIVE")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .build();

    /** Memoizado como hace vanilla: construir un RenderType por frame sería un desperdicio. */
    private static final Function<Identifier, RenderType> CACHE = Util.memoize(
            texture -> RenderType.create("owl_eye_glow",
                    RenderSetup.builder(ADDITIVE_GLOW).withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));

    private OwlEyeGlowRenderType() {
    }

    public static RenderType get(Identifier texture) {
        return USE_VANILLA_FALLBACK ? RenderTypes.eyes(texture) : CACHE.apply(texture);
    }

    @SubscribeEvent
    static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ADDITIVE_GLOW);
    }
}
