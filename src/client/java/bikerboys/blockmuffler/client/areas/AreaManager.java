package bikerboys.blockmuffler.client.areas;

import bikerboys.blockmuffler.*;
import bikerboys.blockmuffler.client.IOptions.*;
import bikerboys.blockmuffler.client.areas.render.*;
import bikerboys.blockmuffler.client.config.*;
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
        } else {
            if (player != null && WandUtil.isHoldingWand(player)) {
                if (BlockMufflerConfig.logEditMode) {
                    player.sendOverlayMessage(Component.literal("Editmode disabled. To disable this message, disable 'Log Edit Mode'."));
                }
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

    public static String getSelectedAreaOrNone() {
        if (selectedArea != null) {
            return selectedArea.getName();
        } else {
            return "None";
        }

    }

    public static boolean continueFunc() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!WandUtil.isEditModeEnabled(player)) {
            if (player != null) {
                if (BlockMufflerConfig.logEditMode) {
                    player.sendOverlayMessage(Component.literal("Editmode disabled. To disable this message, disable 'Log Edit Mode'."));
                }
            }
            return false;
        }
        if (selectedArea == null) return false;
        if (selectedArea.isLocked()) {
            if (BlockMufflerConfig.logAreaLocked) {
                player.sendOverlayMessage(Component.literal("Area: " + selectedArea.getName() + " is locked! Unlock it to edit it again! To disable this message, disable 'Log Area Locked'"));
            }
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

        assert selectedArea != null;

        if (BlockMufflerConfig.areaEditOptions == EditOptions.BOX_FACE) {
            if (areaHit == null) return;
            if (grow) {
                selectedArea.grow(areaHit.face());
            } else {
                selectedArea.shrink(areaHit.face());
            }
        } else if (BlockMufflerConfig.areaEditOptions == EditOptions.LOOK_DIRECTIOM) {
            if (!grow) {
                if (Utils.isInArea(Minecraft.getInstance().getCameraEntity().getEyePosition(), selectedArea)) {
                    selectedArea.shrink(Minecraft.getInstance().player.getNearestViewDirection());
                } else {
                    selectedArea.grow(Minecraft.getInstance().player.getNearestViewDirection());
                }
            } else {
                if (Utils.isInArea(Minecraft.getInstance().getCameraEntity().getEyePosition(), selectedArea)) {
                    selectedArea.grow(Minecraft.getInstance().player.getNearestViewDirection());
                } else {
                    selectedArea.shrink(Minecraft.getInstance().player.getNearestViewDirection());
                }
            }
        }



    }

    public static void renderOverlay(CameraRenderState cameraState) {
        if (!BlockMufflerConfig.rendering) return;
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
                } else if (BlockMufflerConfig.renderBoxSidesWhenInside) {
                    RenderUtils.drawBoxSides(
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
                            target.getDepthTextureView(),
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



    private static void drawInsideSides(PoseStack.Pose pose, BufferBuilder b, AABB box, int color, Vec3 cam) {
        color = ARGB.color((int) (ARGB.alpha(color) * 0.5f), color);

        float x0 = (float) box.minX, x1 = (float) box.maxX;
        float y0 = (float) box.minY, y1 = (float) box.maxY;
        float z0 = (float) box.minZ, z1 = (float) box.maxZ;

        quad(pose, b, color, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0);
        quad(pose, b, color, x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1);
        quad(pose, b, color, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1);
        quad(pose, b, color, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0);
        quad(pose, b, color, x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0);
        quad(pose, b, color, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1);
    }


    private static void quad(PoseStack.Pose pose, BufferBuilder b, int color,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        b.addVertex(pose, ax, ay, az).setColor(color);
        b.addVertex(pose, bx, by, bz).setColor(color);
        b.addVertex(pose, cx, cy, cz).setColor(color);
        b.addVertex(pose, dx, dy, dz).setColor(color);
    }
}





















