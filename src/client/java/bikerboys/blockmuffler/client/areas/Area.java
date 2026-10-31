package bikerboys.blockmuffler.client.areas;

import bikerboys.blockmuffler.client.areas.rules.*;
import net.minecraft.core.*;
import net.minecraft.world.phys.*;

public class Area {
    private final AreaRule areaRule = new AreaRule();
    private AABB aabb;
    private BlockPos pos1, pos2;
    private int color;
    private String name;
    private int enclosingSize;
    private int totalVolume;
    private boolean enabled = true;
    private int rotation = 0;
    private boolean locked = false;
    private boolean renderPlacement = false;


    public Area(BlockPos pos1, BlockPos pos2, int color, String name) {
        setPos1(pos1);
        setPos2(pos2);
        this.color = color;
        this.name = name;
    }

    public AreaRule getAreaRule() {
        return areaRule;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggleEnabled() {
        this.enabled = !enabled;
    }

    public AABB getAabb() {
        return aabb;
    }

    public int getColor() {
        return color;
    }

    public void setRenderPlacement(boolean renderPlacement) {
        this.renderPlacement = renderPlacement;
    }

    public boolean isRenderPlacement() {
        return renderPlacement;
    }

    public boolean isSelected() {
        return this == AreaManager.selectedArea;
    }

    public boolean isLocked() {
        return locked;
    }

    public String getName() {
        return name;
    }

    public int getRotation() {
        return rotation;
    }

    public void setAabb(AABB aabb) {
        this.aabb = aabb;
    }

    public void setColor(int color) {
        this.color = color;
    }

    private void rebuild() {
        if (pos1 == null || pos2 == null) return;
        this.aabb = new AABB(
                Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()),
                Math.max(pos1.getX(), pos2.getX()) + 1, Math.max(pos1.getY(), pos2.getY()) + 1, Math.max(pos1.getZ(), pos2.getZ()) + 1);
    }

    public void setPos1(BlockPos p) { pos1 = p; rebuild(); }
    public void setPos2(BlockPos p) { pos2 = p; rebuild(); }

    public BlockPos getPos1() {
        return pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void resize(Direction face, int amount) {
        if (locked || amount == 0) return;

        Direction.Axis axis = face.getAxis();
        boolean positiveFace = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;

        int c1 = pos1.get(axis);
        int c2 = pos2.get(axis);
        int size = Math.abs(c1 - c2) + 1;

        amount = Math.max(amount, 1 - size);
        if (amount == 0) return;

        boolean movePos1 = positiveFace ? c1 >= c2 : c1 <= c2;

        if (movePos1) {
            pos1 = pos1.relative(face, amount);
        } else {
            pos2 = pos2.relative(face, amount);
        }

        rebuild();
    }

    public void grow(Direction face) {
        resize(face, 1);
    }

    public void shrink(Direction face) {
        resize(face, -1);
    }

    public void toggleRenderPlacement() {
        this.renderPlacement = !this.renderPlacement;
    }

    public void moveTo(BlockPos newOrigin) {
        if (locked || pos1 == null || pos2 == null) return;

        BlockPos origin = getOrigin();
        int dx = newOrigin.getX() - origin.getX();
        int dy = newOrigin.getY() - origin.getY();
        int dz = newOrigin.getZ() - origin.getZ();
        if (dx == 0 && dy == 0 && dz == 0) return;

        pos1 = pos1.offset(dx, dy, dz);
        pos2 = pos2.offset(dx, dy, dz);
        rebuild();
    }

    public void moveBy(int dx, int dy, int dz) {
        if (locked || pos1 == null || pos2 == null) return;

        BlockPos origin = getOrigin();
        moveTo(origin.offset(dx, dy, dz));
    }

    public void movePos1To(BlockPos target) {
        if (locked || pos1 == null || pos2 == null) return;

        pos1 = target;
        rebuild();
    }

    public void movePos2To(BlockPos target) {
        if (locked || pos1 == null || pos2 == null) return;

        pos2 = target;
        rebuild();
    }

    public void rotate(int quarterTurns) {
        if (locked || pos1 == null || pos2 == null) return;

        int turns = Math.floorMod(quarterTurns, 4);
        if (turns == 0) return;

        int dx = pos2.getX() - pos1.getX();
        int dz = pos2.getZ() - pos1.getZ();
        for (int i = 0; i < turns; i++) {
            int previousDx = dx;
            dx = -dz;
            dz = previousDx;
        }

        pos2 = new BlockPos(pos1.getX() + dx, pos2.getY(), pos1.getZ() + dz);
        rotation = Math.floorMod(rotation + turns * 90, 360);
        rebuild();
    }

    public BlockPos getOrigin() {
        return BlockPos.containing(getAabb().getMinPosition());
    }

    public Vec3i getEnclosingSize() {
        return new Vec3i(
                Math.abs(pos1.getX() - pos2.getX()) + 1,
                Math.abs(pos1.getY() - pos2.getY()) + 1,
                Math.abs(pos1.getZ() - pos2.getZ()) + 1
        );
    }

    public long getTotalVolume() {
        Vec3i size = getEnclosingSize();
        return (long) size.getX() * size.getY() * size.getZ();
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }
}