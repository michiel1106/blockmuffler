package bikerboys.blockmuffler.client.areas;

import bikerboys.blockmuffler.client.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.player.*;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import net.minecraft.world.phys.*;
import org.jspecify.annotations.*;

import java.util.*;

public class AreaManager {
    public static double RAYCAST_RANGE = 1024d;

    public static @Nullable Area selectedArea = null;
    public static List<Area> areaList = new ArrayList<>();


    public static void init() {
        LevelRenderEvents.COLLECT_SUBMITS.register(AreaManager::render);
        ClientTickEvents.START_LEVEL_TICK.register(AreaManager::tick);
    }

    private static void tick(ClientLevel level) {


    }

    public static void selectBox(@Nullable LocalPlayer player) {
        if (WandUtil.isEditModeEnabled(player)) {
            WandUtil.AreaHit areaHit = WandUtil.raycastAreaHit(RAYCAST_RANGE);

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



    private static void render(LevelRenderContext context) {
        Vec3 camera = context.levelState().cameraRenderState.pos;

        context.poseStack().pushPose();
        context.poseStack().translate(-camera.x, -camera.y, -camera.z);

        areaList.stream().filter(Area::isRenderPlacement).forEach(area -> {
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.debugFilledBox(), (pose, buffer) -> {
                int color = area.getColor();

                RenderUtils.drawFilledBox(pose, buffer, area.getAabb(), color);
            });


            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.linesTranslucent(), (pose, buffer) ->
                    RenderUtils.drawBoxLines(pose, buffer, area.getAabb(), area));
        });

        context.poseStack().popPose();

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

        WandUtil.AreaHit areaHit = WandUtil.raycastAreaHit(AreaManager.RAYCAST_RANGE);
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
}





















