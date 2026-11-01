package bikerboys.blockmuffler.client.IOptions;

import net.minecraft.util.*;
import org.jspecify.annotations.*;

public enum EditOptions implements StringRepresentable {
    LOOK_DIRECTIOM("blockmuffler.midnightconfig.areaEditOptions.LOOK_DIRECTION"),
    BOX_FACE("blockmuffler.midnightconfig.areaEditOptions.BOX_FACE");


    final String id;
    EditOptions(String id) {
        this.id = id;
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.id;
    }
}