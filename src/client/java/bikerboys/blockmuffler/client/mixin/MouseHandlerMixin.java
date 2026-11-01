package bikerboys.blockmuffler.client.mixin;

import bikerboys.blockmuffler.client.IOptions.*;
import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.config.*;
import bikerboys.blockmuffler.client.keybindhandler.*;
import bikerboys.blockmuffler.client.util.*;
import com.llamalad7.mixinextras.sugar.*;
import net.minecraft.client.*;
import org.joml.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Debug(export = true)
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z", shift = At.Shift.BEFORE), cancellable = true)
    private void onScrollRedirect(long handle, double xoffset, double yoffset, CallbackInfo ci, @Local(name = "wheel") int wheel) {
        if (!WandUtil.isEditModeEnabled(minecraft.player)) return;
        if (BlockMufflerConfig.areaKeybindOptions == KeybindOptions.REGULAR_KEYBINDS) return;
        if (!ModKeybindings.specialKey.isDown()) return;

        float offsetThing = Utils.getModifiedFloat(wheel);

        if (offsetThing == 1.0d) {
            AreaManager.changeAreaSize(false);
        } else if (offsetThing == -1.0d) {
            AreaManager.changeAreaSize(true);
        }


        ci.cancel();
    }
}
