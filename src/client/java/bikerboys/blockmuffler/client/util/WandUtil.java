package bikerboys.blockmuffler.client.util;

import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.config.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.*;

import java.util.*;

public class WandUtil {

    public static boolean isEditModeEnabled(@Nullable Player player) {
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

    public static @Nullable AreaHit raycastAreaHit(double range) {
        Entity cam = Minecraft.getInstance().getCameraEntity();
        if (cam == null) return null;

        Vec3 from = cam.getEyePosition(1.0f);
        Vec3 to = from.add(cam.getViewVector(1.0f).scale(range));
        Vec3 dir = to.subtract(from);

        AreaHit best = null;
        double bestDist = Double.MAX_VALUE;

        for (Area area : AreaManager.areaList) {
            if (!area.isRenderPlacement()) continue;
            AABB box = area.getAabb();
            AreaHit hit;

            if (box.contains(from)) {
                hit = hitFromInside(area, box, from, dir);
            } else {
                BlockHitResult result = AABB.clip(List.of(box), from, to, BlockPos.ZERO);
                hit = result == null ? null
                        : new AreaHit(area, result.getDirection(), result.getLocation());
            }
            if (hit == null) continue;

            double dist = from.distanceToSqr(hit.pos());
            if (dist < bestDist) {
                bestDist = dist;
                best = hit;
            }
        }
        return best;
    }

    private static @Nullable AreaHit hitFromInside(Area area, AABB box, Vec3 from, Vec3 dir) {
        double tX = exitT(from.x, dir.x, box.minX, box.maxX);
        double tY = exitT(from.y, dir.y, box.minY, box.maxY);
        double tZ = exitT(from.z, dir.z, box.minZ, box.maxZ);

        double t = Math.min(tX, Math.min(tY, tZ));
        if (t > 1.0) return null;

        Direction face;
        if (t == tX)      face = dir.x > 0 ? Direction.EAST  : Direction.WEST;
        else if (t == tY) face = dir.y > 0 ? Direction.UP    : Direction.DOWN;
        else              face = dir.z > 0 ? Direction.SOUTH : Direction.NORTH;

        return new AreaHit(area, face, from.add(dir.scale(t)));
    }

    private static double exitT(double origin, double d, double min, double max) {
        if (d > 0) return (max - origin) / d;
        if (d < 0) return (min - origin) / d;
        return Double.POSITIVE_INFINITY;
    }

    public record AreaHit(Area area, Direction face, Vec3 pos) {}

}
