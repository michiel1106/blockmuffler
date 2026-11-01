package bikerboys.blockmuffler.client.IOptions;

import net.minecraft.util.*;
import org.jspecify.annotations.*;

public enum KeybindOptions implements StringRepresentable {
    REGULAR_KEYBINDS("blockmuffler.midnightconfig.areaKeybindOptions.REGULAR_KEYBINDS"),
    SPECIAL_KEY_PLUS_SCROLL("blockmuffler.midnightconfig.areaKeybindOptions.SPECIAL_KEY_PLUS_SCROLL"),
    BOTH("blockmuffler.midnightconfig.areaKeybindOptions.both"),

    ;


    final String id;
    KeybindOptions(String id) {
        this.id = id;
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.id;
    }
}
