package bikerboys.blockmuffler.client.util;

import bikerboys.blockmuffler.*;
import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.config.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.*;

import java.util.*;

public class WandUtil {

    public static boolean isHoldingWand(@Nullable Player player) {
        if (player == null) return false;

        Identifier key = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem());

        return key.equals(BlockMufflerConfig.wand);
    }


    public static @Nullable BlockPos raycastBlockPos() {
        Entity cam = Minecraft.getInstance().getCameraEntity();
        if (cam == null) return null;
        HitResult hit = cam.pick(AreaManager.RAYCAST_RANGE, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return ((BlockHitResult) hit).getBlockPos();
    }


    public static Area raycastArea(double range) {
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity == null) {
            return null;
        }

        Vec3 from = cameraEntity.getEyePosition(1.0f);
        Vec3 view = cameraEntity.getViewVector(1.0f);
        Vec3 to = from.add(view.scale(range));

        Area closestArea = null;
        double closestDistance = Double.MAX_VALUE;

        for (Area area : AreaManager.areaList) {
            Optional<Vec3> hit = area.getAabb().clip(from, to);

            if (hit.isEmpty()) {
                continue;
            }

            double distance = from.distanceToSqr(hit.get());

            if (distance < closestDistance) {
                closestDistance = distance;
                closestArea = area;
            }
        }

        return closestArea;
    }




}
