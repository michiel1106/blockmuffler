package bikerboys.blockmuffler.client.areas;

import net.minecraft.core.*;
import net.minecraft.util.*;
import net.minecraft.world.phys.*;

import java.util.*;

public class Area {
    private AABB aabb;
    private BlockPos pos1, pos2;
    private int color;
    private String name;
    private boolean locked = false;


    public Area(BlockPos pos1, BlockPos pos2, int color, String name) {
        setPos1(pos1);
        setPos2(pos2);
        this.color = color;
        this.name = name;
    }


    public AABB getAabb() {
        return aabb;
    }

    public int getColor() {
        return color;
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
}
