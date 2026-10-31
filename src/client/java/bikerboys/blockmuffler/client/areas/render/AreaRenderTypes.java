package bikerboys.blockmuffler.client.areas.render;

import com.mojang.renderpearl.api.pipeline.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.oit.*;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.*;

import java.util.*;
import java.util.function.*;

public final class AreaRenderTypes {
    private static final String NAMESPACE = "blockmuffler";

    private static final Consumer<RenderPipeline.Builder> NO_DEPTH =
            builder -> builder.withDepthStencilState(Optional.empty());

    public static final RenderPipeline FILL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(NAMESPACE, "pipeline/area_fill"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(Optional.empty())
                    .withCull(false)
                    .build());

    public static final RenderPipeline LINES_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(NAMESPACE, "pipeline/area_lines"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(Optional.empty())
                    .build());




    public static final RenderType LINES = RenderType.create(
            NAMESPACE + ":area_lines",
            RenderSetup.builder(LINES_PIPELINE)
                    .createRenderSetup());





    public static final RenderType FILL = RenderType.create(
            NAMESPACE + ":area_fill",
            RenderSetup.builder(FILL_PIPELINE)
                    .sortOnUpload()
                    .createRenderSetup());



    public static void init() {
    }

}