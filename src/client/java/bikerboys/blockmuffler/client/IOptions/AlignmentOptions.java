package bikerboys.blockmuffler.client.IOptions;

import net.minecraft.util.*;
import org.jspecify.annotations.*;

public enum AlignmentOptions implements StringRepresentable {
    TOP_LEFT    ("blockmuffler.midnightconfig.TOP_LEFT"     ),
    TOP_RIGHT   ("blockmuffler.midnightconfig.TOP_RIGHT"    ),
    BOTTOM_RIGHT("blockmuffler.midnightconfig.BOTTOM_RIGHT" ),
    BOTTOM_LEFT ("blockmuffler.midnightconfig.BOTTOM_LEFT"  ),
    ;


    final String id;
    AlignmentOptions(String id) {
        this.id = id;
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.id;
    }
}
