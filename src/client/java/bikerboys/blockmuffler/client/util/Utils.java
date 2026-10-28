package bikerboys.blockmuffler.client.util;

import bikerboys.blockmuffler.client.areas.*;
import net.minecraft.client.*;
import net.minecraft.client.player.*;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;

public class Utils {

    public static void sendKeyNotWorkingMessage(Minecraft mc) {
        if (mc.player == null) return;

        mc.player.sendOverlayMessage(Component.translatable("blockmuffler.message.key_disabled"));
    }

    public static float getModifiedFloat(float original) {
        LocalPlayer player = Minecraft.getInstance().player;
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (player == null) return original;
        if (cameraEntity == null) return original;
        if (!AreaManager.continueFunc()) return original;

        if (isInArea(cameraEntity.getEyePosition(), AreaManager.selectedArea)) {
            return original * -1.0f;
        }

        return original;
    }


    public static boolean isInArea(Vec3 pos, Area area) {
        return area.getAabb().contains(pos);
    }

}
