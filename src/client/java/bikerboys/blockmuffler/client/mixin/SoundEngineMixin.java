package bikerboys.blockmuffler.client.mixin;

import bikerboys.blockmuffler.client.areas.AreaSoundFilter;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @ModifyReturnValue(
            method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("RETURN")
    )
    private float blockmuffler$applyAreaRules(float original, SoundInstance instance) {
        return original * AreaSoundFilter.multiplier(instance);
    }

    @ModifyExpressionValue(
            method = "play",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"
            )
    )
    private float blockmuffler$applyAreaRulesOnPlay(float original, @Local(argsOnly = true) SoundInstance instance) {
        return original * AreaSoundFilter.multiplier(instance);
    }
}