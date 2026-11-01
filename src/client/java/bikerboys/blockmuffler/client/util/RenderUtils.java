package bikerboys.blockmuffler.client.util;

import bikerboys.blockmuffler.client.areas.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;

public class RenderUtils {

    public static int CORNER_1_COLOR = 0xFFFF0000;
    public static int CORNER_2_COLOR = 0xFF0000FF;
    public static int EDGE_COLOR     = 0xFFFFFFFF;

    public static final float EDGE_WIDTH   = 2.0f;
    public static final float CORNER_WIDTH = 3.0f;

    public static void drawFilledBox(PoseStack.Pose pose, VertexConsumer vertexConsumer, AABB box, int color) {
        // Front
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.minZ).setColor(color);
        // Back
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.maxZ).setColor(color);
        // Left
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.maxZ).setColor(color);
        // Right
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.minZ).setColor(color);
        // Top
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.maxZ).setColor(color);
        // Bottom
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.minZ).setColor(color);
    }

    public static void drawBoxLines(PoseStack.Pose pose, VertexConsumer lines, AABB box, Area area) {

        if (area.isSelected()) {
            EDGE_COLOR = 0xFFFFFFFF;
        } else {
            EDGE_COLOR = 0xFF808080;
        }

        drawBoxOutline(pose, lines, box, EDGE_COLOR, EDGE_WIDTH);

        double sx = Math.min(1.0, box.getXsize());
        double sy = Math.min(1.0, box.getYsize());
        double sz = Math.min(1.0, box.getZsize());

        if (area.isSelected()) {
            AABB corner1 = new AABB(box.minX, box.minY, box.minZ, box.minX + sx, box.minY + sy, box.minZ + sz).inflate(0.005);
            AABB corner2 = new AABB(box.maxX - sx, box.maxY - sy, box.maxZ - sz, box.maxX, box.maxY, box.maxZ).inflate(0.005);
            drawBoxOutline(pose, lines, new AABB(area.getPos1()), CORNER_1_COLOR, CORNER_WIDTH);
            drawBoxOutline(pose, lines, new AABB(area.getPos2()), CORNER_2_COLOR, CORNER_WIDTH);
        }
    }

    public static void drawBoxOutline(PoseStack.Pose pose, VertexConsumer lines, AABB box, int color, float lineWidth) {
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;

        // Bottom face
        edge(pose, lines, color, lineWidth, x1, y1, z1, x2, y1, z1);
        edge(pose, lines, color, lineWidth, x2, y1, z1, x2, y1, z2);
        edge(pose, lines, color, lineWidth, x2, y1, z2, x1, y1, z2);
        edge(pose, lines, color, lineWidth, x1, y1, z2, x1, y1, z1);

        // Top face
        edge(pose, lines, color, lineWidth, x1, y2, z1, x2, y2, z1);
        edge(pose, lines, color, lineWidth, x2, y2, z1, x2, y2, z2);
        edge(pose, lines, color, lineWidth, x2, y2, z2, x1, y2, z2);
        edge(pose, lines, color, lineWidth, x1, y2, z2, x1, y2, z1);

        // Vertical edges
        edge(pose, lines, color, lineWidth, x1, y1, z1, x1, y2, z1);
        edge(pose, lines, color, lineWidth, x2, y1, z1, x2, y2, z1);
        edge(pose, lines, color, lineWidth, x2, y1, z2, x2, y2, z2);
        edge(pose, lines, color, lineWidth, x1, y1, z2, x1, y2, z2);
    }

    public static AABB selectionBox(BlockPos corner1, BlockPos corner2) {
        return new AABB(
                Math.min(corner1.getX(), corner2.getX()),
                Math.min(corner1.getY(), corner2.getY()),
                Math.min(corner1.getZ(), corner2.getZ()),
                Math.max(corner1.getX(), corner2.getX()) + 1,
                Math.max(corner1.getY(), corner2.getY()) + 1,
                Math.max(corner1.getZ(), corner2.getZ()) + 1
        );
    }

    private static void edge(PoseStack.Pose pose, VertexConsumer lines, int color, float width,
                             float ax, float ay, float az, float bx, float by, float bz) {
        float dx = bx - ax;
        float dy = by - ay;
        float dz = bz - az;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6f) return;
        dx /= length;
        dy /= length;
        dz /= length;

        lines.addVertex(pose, ax, ay, az).setColor(color).setNormal(pose, dx, dy, dz).setLineWidth(width);
        lines.addVertex(pose, bx, by, bz).setColor(color).setNormal(pose, dx, dy, dz).setLineWidth(width);
    }

    public static void drawBoxSides(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            AABB box,
            int color
    ) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;

        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        buffer.addVertex(pose, minX, minY, minZ).setColor(color);
        buffer.addVertex(pose, minX, minY, maxZ).setColor(color);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(color);
        buffer.addVertex(pose, minX, maxY, minZ).setColor(color);

        buffer.addVertex(pose, maxX, minY, minZ).setColor(color);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(color);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(color);
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(color);

        buffer.addVertex(pose, minX, minY, minZ).setColor(color);
        buffer.addVertex(pose, minX, maxY, minZ).setColor(color);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(color);
        buffer.addVertex(pose, maxX, minY, minZ).setColor(color);

        buffer.addVertex(pose, minX, minY, maxZ).setColor(color);
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(color);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(color);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(color);

        buffer.addVertex(pose, minX, maxY, minZ).setColor(color);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(color);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(color);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(color);

        buffer.addVertex(pose, minX, minY, minZ).setColor(color);
        buffer.addVertex(pose, maxX, minY, minZ).setColor(color);
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(color);
        buffer.addVertex(pose, minX, minY, maxZ).setColor(color);
    }

}