package bikerboys.blockmuffler.client.areas;

import bikerboys.blockmuffler.client.areas.render.*;
import bikerboys.blockmuffler.client.util.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.buffers.*;
import com.mojang.renderpearl.api.commands.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.player.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.renderer.state.level.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import net.minecraft.world.phys.*;
import org.joml.*;
import org.jspecify.annotations.*;

import java.nio.*;
import java.util.*;

public class AreaManager {
    public static double RAYCAST_RANGE = 1024d;

    public static @Nullable Area selectedArea = null;
    public static List<Area> areaList = new ArrayList<>();


    public static void init() {
        ClientTickEvents.START_LEVEL_TICK.register(AreaManager::tick);
    }

    private static void tick(ClientLevel level) {


    }

    public static void selectBox(@Nullable LocalPlayer player) {
        if (WandUtil.isEditModeEnabled(player)) {
            WandUtil.AreaHit areaHit = WandUtil.raycastAreaHit(RAYCAST_RANGE, false);

            if (areaHit != null) {
                selectedArea = areaHit.area();
            } else {
                selectedArea = null;
            }
        }
    }

    public static void addTestBoxes() {
        addBox(
                new BlockPos(0, 100, 0),
                new BlockPos(10, 110, 10),
                ARGB.colorFromFloat(0.5f, 0, 1, 0), "Test");

        addBox(
                new BlockPos(30, 100, 30),
                new BlockPos(40, 110, 40),
                ARGB.colorFromFloat(0.5f, 0, 1, 0), "Test2");
    }

    public static void addBox(BlockPos pos1, BlockPos pos2, int color, String name) {
        areaList.add(new Area(pos1, pos2, color, name));
    }



    /*
    private static void render(LevelRenderContext context) {
        Vec3 camera = context.levelState().cameraRenderState.pos;

        context.poseStack().pushPose();
        context.poseStack().translate(-camera.x, -camera.y, -camera.z);

        areaList.stream().filter(Area::isRenderPlacement).forEach(area -> {
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), AreaRenderTypes.FILL, (pose, buffer) -> {
                int color = area.getColor();

                RenderUtils.drawFilledBox(pose, buffer, area.getAabb(), color);
            });


            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), AreaRenderTypes.LINES, (pose, buffer) ->
                    RenderUtils.drawBoxLines(pose, buffer, area.getAabb(), area));
        });

        context.poseStack().popPose();

    }

     */



    public static String getSelectedAreaOrNone() {
        if (selectedArea != null) {
            return selectedArea.getName();
        } else {
            return "None";
        }

    }

    public static boolean continueFunc() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!WandUtil.isEditModeEnabled(player)) return false;
        if (selectedArea == null) return false;
        if (selectedArea.isLocked()) {
            player.sendOverlayMessage(Component.literal("Area: " + selectedArea.getName() + " is locked! Unlock it to edit it again!"));
            return false;
        }
        return true;
    }

    public static void setFirstPoint() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!continueFunc()) return;

        BlockPos blockPos = WandUtil.raycastBlockPos();
        if (blockPos == null) return;
        selectedArea.setPos1(blockPos);

        assert player != null;
        player.sendOverlayMessage(Component.literal("Set point 1 of '" + selectedArea.getName() + "' to " + blockPos.toShortString()));


    }

    public static void setSecondPoint() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!continueFunc()) return;

        BlockPos blockPos = WandUtil.raycastBlockPos();
        if (blockPos == null) return;
        selectedArea.setPos2(blockPos);

        assert player != null;
        player.sendOverlayMessage(Component.literal("Set point 2 of '" + selectedArea.getName() + "' to " + blockPos.toShortString()));

    }

    public static void changeAreaSize(boolean grow) {
        if (!continueFunc()) return;

        WandUtil.AreaHit areaHit = WandUtil.raycastAreaHit(AreaManager.RAYCAST_RANGE, true);
        if (areaHit == null) return;
        assert selectedArea != null;

        if (grow) {
            selectedArea.grow(areaHit.face());
        } else {
            selectedArea.shrink(areaHit.face());
        }
    }

    public static void toggleAllPlacements() {
        areaList.forEach(Area::toggleRenderPlacement);
    }

    public static void renderOverlay(CameraRenderState cameraState) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || areaList.isEmpty()) {
            return;
        }

        RenderTarget target = minecraft.gameRenderer.mainRenderTarget();

        if (target.getColorTextureView() == null) {
            return;
        }

        PoseStack poseStack = new PoseStack();

        poseStack.translate(
                -cameraState.pos.x,
                -cameraState.pos.y,
                -cameraState.pos.z
        );

        drawAreas(target, poseStack, cameraState);
    }

    private static void drawAreas(
            RenderTarget target,
            PoseStack poseStack,
            CameraRenderState cameraState
    ) {
        ByteBufferBuilder fillAllocator = new ByteBufferBuilder(4096);
        ByteBufferBuilder linesAllocator = new ByteBufferBuilder(4096);
        ByteBufferBuilder fillSortAllocator = new ByteBufferBuilder(1024);

        MeshData fillMesh = null;
        MeshData linesMesh = null;

        GpuBuffer fillVertexBuffer = null;
        GpuBuffer fillIndexBuffer = null;
        GpuBuffer linesVertexBuffer = null;

        try {
            BufferBuilder fillBuilder = new BufferBuilder(
                    fillAllocator,
                    AreaRenderTypes.FILL.primitiveTopology(),
                    AreaRenderTypes.FILL.format()
            );

            BufferBuilder linesBuilder = new BufferBuilder(
                    linesAllocator,
                    AreaRenderTypes.LINES.primitiveTopology(),
                    AreaRenderTypes.LINES.format()
            );

            for (Area area : areaList) {
                if (!area.isRenderPlacement()) {
                    continue;
                }

                AABB box = area.getAabb();

                if (!box.contains(cameraState.pos)) {
                    RenderUtils.drawFilledBox(
                            poseStack.last(),
                            fillBuilder,
                            box,
                            area.getColor()
                    );
                }

                RenderUtils.drawBoxLines(
                        poseStack.last(),
                        linesBuilder,
                        box,
                        area
                );
            }

            fillMesh = fillBuilder.build();
            linesMesh = linesBuilder.build();

            if (fillMesh == null && linesMesh == null) {
                return;
            }

            if (fillMesh != null && AreaRenderTypes.FILL.sortOnUpload()) {
                fillMesh.sortQuads(
                        fillSortAllocator,
                        RenderSystem.getProjectionType().vertexSorting()
                );
            }

            if (fillMesh != null) {
                fillVertexBuffer = RenderSystem.getDevice().createBuffer(
                        () -> "Block Muffler area fill vertices",
                        GpuBuffer.USAGE_VERTEX,
                        fillMesh.vertexBuffer()
                );

                ByteBuffer indices = fillMesh.indexBuffer();

                if (indices != null) {
                    fillIndexBuffer = RenderSystem.getDevice().createBuffer(
                            () -> "Block Muffler area fill indices",
                            GpuBuffer.USAGE_INDEX,
                            indices
                    );
                }
            }

            if (linesMesh != null) {
                linesVertexBuffer = RenderSystem.getDevice().createBuffer(
                        () -> "Block Muffler area line vertices",
                        GpuBuffer.USAGE_VERTEX,
                        linesMesh.vertexBuffer()
                );
            }

            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                    .writeTransform(new Matrix4f(cameraState.viewRotationMatrix));

            try (RenderPass pass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(
                            () -> "Block Muffler Areas",
                            target.getColorTextureView(),
                            Optional.empty(),
                            null,
                            OptionalDouble.empty()
                    )) {

                if (fillMesh != null && fillVertexBuffer != null) {
                    drawAreaMesh(
                            pass,
                            AreaRenderTypes.FILL,
                            fillMesh,
                            fillVertexBuffer,
                            fillIndexBuffer,
                            transforms
                    );
                }

                if (linesMesh != null && linesVertexBuffer != null) {
                    drawAreaMesh(
                            pass,
                            AreaRenderTypes.LINES,
                            linesMesh,
                            linesVertexBuffer,
                            null,
                            transforms
                    );
                }
            }

        } finally {
            if (fillVertexBuffer != null) {
                fillVertexBuffer.close();
            }

            if (fillIndexBuffer != null) {
                fillIndexBuffer.close();
            }

            if (linesVertexBuffer != null) {
                linesVertexBuffer.close();
            }

            // Release CPU-side mesh data and builders.
            if (fillMesh != null) {
                fillMesh.close();
            }

            if (linesMesh != null) {
                linesMesh.close();
            }

            fillSortAllocator.close();
            fillAllocator.close();
            linesAllocator.close();
        }
    }

    private static void drawAreaMesh(
            RenderPass pass,
            RenderType renderType,
            MeshData mesh,
            GpuBuffer vertexBuffer,
            @Nullable GpuBuffer customIndexBuffer,
            GpuBufferSlice transforms
    ) {
        pass.setPipeline(
                RenderSystem.getCompiledPipeline(renderType.pipeline())
        );

        RenderSystem.bindDefaultUniforms(pass);

        pass.setUniform("DynamicTransforms", transforms);

        pass.setVertexBuffer(0, vertexBuffer.slice());

        if (customIndexBuffer != null) {
            pass.setIndexBuffer(
                    customIndexBuffer,
                    mesh.drawState().indexType()
            );
        } else {
            RenderSystem.AutoStorageIndexBuffer indices =
                    RenderSystem.getSequentialBuffer(mesh.drawState().primitiveTopology());

            pass.setIndexBuffer(
                    indices.getBuffer(mesh.drawState().indexCount()),
                    indices.type()
            );
        }

        pass.drawIndexed(
                mesh.drawState().indexCount(),
                1,
                0,
                0,
                0
        );
    }
}





















