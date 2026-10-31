package bikerboys.blockmuffler.client.areas;

import bikerboys.blockmuffler.client.areas.rules.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class AreaSoundFilter {
    private AreaSoundFilter() {
    }

    public static float multiplier(SoundInstance instance) {
        if (AreaManager.areaList.isEmpty()) return 1.0f;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return 1.0f;

        double x = instance.getX();
        double y = instance.getY();
        double z = instance.getZ();
        if (instance.isRelative()) {
            Vec3 listener = minecraft.player.position();
            x = listener.x;
            y = listener.y;
            z = listener.z;
        }

        SoundSource source = instance.getSource();
        boolean entitySound = isEntitySound(instance, source);
        String id = instance.getIdentifier().toString();

        float result = 1.0f;
        for (Area area : AreaManager.areaList) {
            if (!area.isEnabled()) continue;
            if (!area.getAabb().contains(x, y, z)) continue;

            AreaRule rule = area.getAreaRule();
            if (entitySound && !rule.isIncludeEntities()) continue;

            result *= rule.getVolumeMultiplier(id, source);
        }
        return result;
    }

    private static boolean isEntitySound(SoundInstance instance, SoundSource source) {
        return instance instanceof EntityBoundSoundInstance
                || source == SoundSource.HOSTILE
                || source == SoundSource.NEUTRAL
                || source == SoundSource.PLAYERS;
    }
}