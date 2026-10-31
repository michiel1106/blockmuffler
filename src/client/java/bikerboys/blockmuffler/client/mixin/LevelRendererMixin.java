package bikerboys.blockmuffler.client.mixin;

import bikerboys.blockmuffler.client.areas.*;
import com.mojang.blaze3d.resource.*;
import com.mojang.renderpearl.api.buffers.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.state.level.*;
import org.joml.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void blockmuffler$renderAreas(
            GraphicsResourceAllocator resourceAllocator, boolean renderOutline, CameraRenderState cameraState, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, boolean consistentDepthRequired, CallbackInfo ci
    ) {
        AreaManager.renderOverlay(cameraState);
    }

}
