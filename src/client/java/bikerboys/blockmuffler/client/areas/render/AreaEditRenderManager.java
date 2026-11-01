package bikerboys.blockmuffler.client.areas.render;

import bikerboys.blockmuffler.*;
import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.config.*;
import bikerboys.blockmuffler.client.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.impl.client.rendering.hud.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import org.jspecify.annotations.*;

import java.util.*;

public class AreaEditRenderManager {
    public static void init() {
        HudElementRegistryImpl.attachElementAfter(
                VanillaHudElements.SUBTITLES,
                Identifier.fromNamespaceAndPath(BlockMuffler.MOD_ID, "block_muffler"),
                new HudElement() {
                    private static final int MARGIN = 4;
                    private static final int LINE_SPACING = 1;

                    @Override
                    public void extractRenderState(@NonNull GuiGraphicsExtractor g, @NonNull DeltaTracker deltaTracker) {
                        Minecraft mc = Minecraft.getInstance();
                        if (!WandUtil.isEditModeEnabled(mc.player)) return;
                        Font font = mc.font;

                        List<Component> lines = new ArrayList<>();
                        lines.add(Component.literal("Currently selected area: " + AreaManager.getSelectedAreaOrNone()));

                        Area area = AreaManager.selectedArea;
                        if (area != null) {
                            Vec3i size = area.getEnclosingSize();
                            lines.add(Component.literal("Origin: " + area.getPos1().toShortString()));
                            lines.add(Component.literal(String.format("Size: %s x %s x %s", size.getX(), size.getY(), size.getZ())));
                            lines.add(Component.literal("Rotation: " + area.getRotation()));
                            lines.add(Component.literal("Placement: ").append(
                                    Component.literal(String.valueOf(area.isRenderPlacement()))
                                            .withColor(area.isRenderPlacement() ? TextColor.GREEN : TextColor.RED)));
                            lines.add(Component.literal("Locked: ").append(
                                    Component.literal(String.valueOf(area.isLocked()))
                                            .withColor(area.isLocked() ? TextColor.GREEN : TextColor.RED)));
                        }

                        int lineStep = font.lineHeight + LINE_SPACING;
                        int blockWidth = lines.stream().mapToInt(font::width).max().orElse(0);
                        int blockHeight = lines.size() * lineStep - LINE_SPACING;

                        var alignment = BlockMufflerConfig.hudAlignmentOptions;
                        boolean right = switch (alignment) {
                            case TOP_RIGHT, BOTTOM_RIGHT -> true;
                            default -> false;
                        };
                        boolean bottom = switch (alignment) {
                            case BOTTOM_LEFT, BOTTOM_RIGHT -> true;
                            default -> false;
                        };

                        int x = right ? g.guiWidth() - MARGIN - blockWidth : MARGIN;
                        int y = bottom ? g.guiHeight() - MARGIN - blockHeight : MARGIN;

                        for (int i = 0; i < lines.size(); i++) {
                            Component line = lines.get(i);
                            int lineX = right ? x + blockWidth - font.width(line) : x;
                            renderText(g, line, lineX, y + i * lineStep);
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
