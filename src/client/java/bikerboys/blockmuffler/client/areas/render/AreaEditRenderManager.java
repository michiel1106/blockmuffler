package bikerboys.blockmuffler.client.areas.render;

import bikerboys.blockmuffler.*;
import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.impl.client.rendering.hud.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import org.jspecify.annotations.*;

public class AreaEditRenderManager {

    public static void init() {
        HudElementRegistryImpl.attachElementAfter(VanillaHudElements.SUBTITLES, Identifier.fromNamespaceAndPath(BlockMuffler.MOD_ID, "block_muffler"), new HudElement() {
            @Override
            public void extractRenderState(@NonNull GuiGraphicsExtractor g, @NonNull DeltaTracker deltaTracker) {
                if (!WandUtil.isEditModeEnabled(Minecraft.getInstance().player)) return;
                int lineHeight = Minecraft.getInstance().font.lineHeight;

                String selectedAreaText = "Currently selected area: " + AreaManager.getSelectedAreaOrNone();

                renderText(g, Component.nullToEmpty(selectedAreaText), getNorWidth(0.005f, g), getNorHeight(0.01f, g));

                Area selectedArea = AreaManager.selectedArea;
                if (selectedArea != null) {
                    String origin = "Origin: " + selectedArea.getPos1().toShortString();
                    Vec3i enclosingSize = selectedArea.getEnclosingSize();
                    String size = String.format("Size: %s x %s x %s", enclosingSize.getX(), enclosingSize.getY(), enclosingSize.getZ());
                    String rotation = String.format("Rotation: %s", selectedArea.getRotation());
                    Component placementEnabled = Component.literal("Placement: ").append(Component.literal(String.valueOf(selectedArea.isRenderPlacement())).withColor(selectedArea.isRenderPlacement() ? TextColor.GREEN : TextColor.RED));
                    Component placementLocked = Component.literal("Locked: ").append(Component.literal(String.valueOf(selectedArea.isLocked())).withColor(selectedArea.isLocked() ? TextColor.GREEN : TextColor.RED));


                    renderText(g, Component.literal(origin), getNorWidth(0.005f, g), getNorHeight(0.01f, g) + (lineHeight + 1));
                    renderText(g, Component.literal(size), getNorWidth(0.005f, g), getNorHeight(0.01f, g) + (lineHeight + 1) * 2);
                    renderText(g, Component.literal(rotation), getNorWidth(0.005f, g), getNorHeight(0.01f, g) + (lineHeight + 1) * 3);
                    renderText(g, placementEnabled, getNorWidth(0.005f, g), getNorHeight(0.01f, g) + (lineHeight + 1) * 4);
                    renderText(g, placementLocked, getNorWidth(0.005f, g), getNorHeight(0.01f, g) + (lineHeight + 1) * 5);
                }
            }
        });
    }

    public static void renderText(GuiGraphicsExtractor graphics, Component text, int x, int y) {
        int lineHeight = Minecraft.getInstance().font.lineHeight;


        graphics.fill(x - 1, y - 1, x + getWidth(text) + 1, y + lineHeight, -1873784752);
        graphics.textWithBackdrop(Minecraft.getInstance().font, text, x, y, getWidth(text), 0xFFFFFFFF);
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
