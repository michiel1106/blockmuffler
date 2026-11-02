package bikerboys.blockmuffler.client.config;

import bikerboys.blockmuffler.*;
import bikerboys.blockmuffler.client.IOptions.*;
import eu.midnightdust.lib.config.*;
import net.minecraft.client.*;
import net.minecraft.resources.*;

public class BlockMufflerConfig extends MidnightConfig {
    public static final String QUICK_SETTINGS = "Quick_Settings";
    public static final String GENERAL_SETTINGS = "General";
    public static final String RENDERING = "Rendering";
    public static final String LOGGING = "Logging";

    @Entry(category = QUICK_SETTINGS) public static Identifier wand = Identifier.fromNamespaceAndPath("minecraft", "stick");
    @Entry(category = QUICK_SETTINGS) public static boolean rendering = true;
    @Entry(category = QUICK_SETTINGS) public static boolean editMode = true;

    @Entry(category = GENERAL_SETTINGS) public static KeybindOptions areaKeybindOptions = KeybindOptions.SPECIAL_KEY_PLUS_SCROLL;
    @Entry(category = GENERAL_SETTINGS) public static EditOptions areaEditOptions = EditOptions.BOX_FACE;

    @Entry(category = RENDERING) public static AlignmentOptions hudAlignmentOptions = AlignmentOptions.TOP_LEFT;
    @Entry(category = RENDERING) public static boolean renderBoxSidesWhenInside = false;


    @Entry(category = LOGGING) public static boolean logEditMode = true;
    @Entry(category = LOGGING) public static boolean logAreaLocked = true;
    @Entry(category = LOGGING) public static boolean logRenderToggle = true;


}
