package bikerboys.blockmuffler.client.areas.render;

import bikerboys.blockmuffler.*;
import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.impl.client.rendering.hud.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import org.jspecify.annotations.*;

public class AreaEditRenderManager {

    public static void init() {
        HudElementRegistryImpl.attachElementAfter(VanillaHudElements.SUBTITLES, Identifier.fromNamespaceAndPath(BlockMuffler.MOD_ID, "block_muffler"), new HudElement() {
            @Override
            public void extractRenderState(@NonNull GuiGraphicsExtractor g, @NonNull DeltaTracker deltaTracker) {
                if (!WandUtil.isEditModeEnabled(Minecraft.getInstance().player)) return;

                String text = "Currently selected area: " + AreaManager.getSelectedAreaOrNone();

                renderText(g, text, getNorWidth(0.005f, g), getNorHeight(0.01f, g));
            }
        });
    }

    public static void renderText(GuiGraphicsExtractor graphics, String text, int x, int y) {
        MutableComponent component = Component.literal(text);
        int lineHeight = Minecraft.getInstance().font.lineHeight;


        graphics.fill(x - 1, y - 1, x + getWidth(component) + 1, y + lineHeight, -1873784752);
        graphics.textWithBackdrop(Minecraft.getInstance().font, component, x, y, getWidth(component), 0xFFFFFFFF);
    }

    public static int getNorWidth(float normVal,GuiGraphicsExtractor g) {
        return (int) (g.guiWidth() * normVal);
    }

    public static int getNorHeight(float normVal,GuiGraphicsExtractor g) {
        return (int) (g.guiHeight() * normVal);
    }

    public static int getWidth(Component component) {
        return Minecraft.getInstance().font.width(component.getString());
    }

}
