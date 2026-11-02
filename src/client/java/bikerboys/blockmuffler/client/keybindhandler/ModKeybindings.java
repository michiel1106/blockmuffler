package bikerboys.blockmuffler.client.keybindhandler;

import bikerboys.blockmuffler.*;
import bikerboys.blockmuffler.client.IOptions.*;
import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.config.*;
import bikerboys.blockmuffler.client.screen.*;
import bikerboys.blockmuffler.client.util.*;
import com.mojang.blaze3d.platform.*;
import eu.midnightdust.lib.config.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.keymapping.v1.*;
import net.minecraft.client.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;

import java.util.*;

public class ModKeybindings {
    public static List<KeyMapping> keyMappings = new ArrayList<>();
    public static List<KeyMapping> wandMappings = new ArrayList<>();
    public static List<KeyMapping> laterWandMappings = new ArrayList<>();

    public static KeyMapping.Category blockMuffler = new KeyMapping.Category(Identifier.fromNamespaceAndPath(BlockMuffler.MOD_ID, "blockmuffler"));

    public static KeyMapping openConfig = r(new KeyMapping("blockmuffler.key.openconfig", InputConstants.KEY_G, blockMuffler));
    public static KeyMapping openManager = r(new KeyMapping("blockmuffler.key.openmanager", InputConstants.KEY_M, blockMuffler));

    public static KeyMapping specialKey = r(new KeyMapping("blockmuffler.key.specialkey", InputConstants.KEY_LALT, blockMuffler));

    public static KeyMapping toggleRendering = r(new KeyMapping("blockmuffler.key.toggleRendering", InputConstants.KEY_END, blockMuffler));

    public static KeyMapping selectArea = wand(new KeyMapping("blockmuffler.key.selectarea", InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_MIDDLE, blockMuffler));
    public static KeyMapping setFirstPoint = wand(new KeyMapping("blockmuffler.key.setfirstpoint", InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_LEFT, blockMuffler));
    public static KeyMapping setSecondPoint = wand(new KeyMapping("blockmuffler.key.setsecondpoint", InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_RIGHT, blockMuffler));

    public static KeyMapping growAreaSize = wand(new KeyMapping("blockmuffler.key.growareasize", InputConstants.KEY_EQUALS, blockMuffler));
    public static KeyMapping shrinkAreaSize = wand(new KeyMapping("blockmuffler.key.shrinkareasize", InputConstants.KEY_MINUS, blockMuffler));


    public static void init() {
        KeyMapping.Category.register(blockMuffler.id());

        keyMappings.forEach(KeyMappingHelper::registerKeyMapping);
        laterWandMappings.forEach(KeyMappingHelper::registerKeyMapping);

        ClientTickEvents.START_CLIENT_TICK.register(ModKeybindings::tick);
        ClientTickEvents.END_CLIENT_TICK.register(ModKeybindings::tick);
    }

    private static void tick(Minecraft mc) {
        if (openConfig.consumeClick()) {
            MidnightConfigScreen screen = MidnightConfig.getScreen(mc.gui.screen(), BlockMuffler.MOD_ID);
            mc.setScreenAndShow(screen);
        }

        if (openManager.consumeClick()) {
            mc.setScreenAndShow(new AreaManagerScreen());
        }

        if (toggleRendering.consumeClick()) {
            BlockMufflerConfig.rendering = !BlockMufflerConfig.rendering;
            MidnightConfig.write(BlockMuffler.MOD_ID);
            if (mc.player != null) {
                String msg = BlockMufflerConfig.rendering ? "Enabled" : "Disabled";

                if (BlockMufflerConfig.logRenderToggle) {
                    mc.player.sendOverlayMessage(Component.literal("Rendering " + msg + "! Disable 'Log Render Toggle' in settings to disable this message."));
                }
            }
        }

        if (selectArea.consumeClick()) AreaManager.selectBox(mc.player);
        if (setFirstPoint.consumeClick()) AreaManager.setFirstPoint();
        if (setSecondPoint.consumeClick()) AreaManager.setSecondPoint();

        if (BlockMufflerConfig.areaKeybindOptions == KeybindOptions.REGULAR_KEYBINDS || BlockMufflerConfig.areaKeybindOptions == KeybindOptions.BOTH) {
            if (growAreaSize.consumeClick()) {
                AreaManager.changeAreaSize(true);
            }

            if (shrinkAreaSize.consumeClick()) {
                AreaManager.changeAreaSize(false);
            }
        } else {
            if (growAreaSize.consumeClick() || shrinkAreaSize.consumeClick()) {
                Utils.sendKeyNotWorkingMessage(mc);
            }
        }

        if (WandUtil.isEditModeEnabled(mc.player)) {
            suppressConflictingMappings(mc);
        }
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
        laterWandMappings.add(keyMapping);
        return keyMapping;
    }
}