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

@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class OwlEyeGlowRenderType {

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
