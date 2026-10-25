package bikerboys.blockmuffler.client.keybindhandler;

import bikerboys.blockmuffler.*;
import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.util.*;
import com.mojang.blaze3d.platform.*;
import eu.midnightdust.lib.config.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.keymapping.v1.*;
import net.minecraft.client.*;
import net.minecraft.resources.*;

import java.util.*;

public class ModKeybindings {
    public static List<KeyMapping> keyMappings = new ArrayList<>();
    public static List<KeyMapping> wandMappings = new ArrayList<>();

    public static KeyMapping.Category blockMuffler = new KeyMapping.Category(Identifier.fromNamespaceAndPath(BlockMuffler.MOD_ID, "blockmuffler"));

    public static KeyMapping openGui = r(new KeyMapping("blockmuffler.key.opengui", InputConstants.KEY_G, blockMuffler));

    public static KeyMapping selectArea = wand(new KeyMapping("blockmuffler.key.selectarea", InputConstants.MOUSE_BUTTON_MIDDLE, blockMuffler));
    public static KeyMapping setFirstPoint = wand(new KeyMapping("blockmuffler.key.setfirstpoint", InputConstants.MOUSE_BUTTON_LEFT, blockMuffler));
    public static KeyMapping setSecondPoint = wand(new KeyMapping("blockmuffler.key.setsecondpoint", InputConstants.MOUSE_BUTTON_RIGHT, blockMuffler));

    public static void init() {
        KeyMapping.Category.register(blockMuffler.id());

        keyMappings.forEach(KeyMappingHelper::registerKeyMapping);

        ClientTickEvents.START_CLIENT_TICK.register(ModKeybindings::tick);
    }

    private static void tick(Minecraft mc) {
        if (openGui.consumeClick()) {
            MidnightConfigScreen screen = MidnightConfig.getScreen(mc.gui.screen(), BlockMuffler.MOD_ID);
            mc.setScreenAndShow(screen);
        }

        if (!WandUtil.isHoldingWand(mc.player)) return;

        if (selectArea.consumeClick()) AreaManager.selectBox(mc.player);
        if (setFirstPoint.consumeClick()) AreaManager.setFirstPoint();
        if (setSecondPoint.consumeClick()) AreaManager.setSecondPoint();

        suppressConflictingMappings(mc);
    }

    private static void suppressConflictingMappings(Minecraft mc) {
        for (KeyMapping other : mc.options.keyMappings) {
            if (wandMappings.contains(other)) continue;

            for (KeyMapping mine : wandMappings) {
                if (!mine.isUnbound() && other.same(mine)) {
                    while (other.consumeClick()) { }
                    other.setDown(false);
                    break;
                }
            }
        }
    }

    private static KeyMapping r(KeyMapping keyMapping) {
        keyMappings.add(keyMapping);
        return keyMapping;
    }

    private static KeyMapping wand(KeyMapping keyMapping) {
        wandMappings.add(keyMapping);
        return r(keyMapping);
    }
}