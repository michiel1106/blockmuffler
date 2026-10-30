package bikerboys.blockmuffler.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public final class MenuStyle {
    public static final int SCROLLBAR_WIDTH = 6;

    public static final int LIST_BACKGROUND = 0x70000000;
    public static final int SEPARATOR_DARK = 0xFF000000;
    public static final int SEPARATOR_LIGHT = 0xFF373737;
    public static final int SELECTION_OUTER = 0xFFFFFFFF;
    public static final int SELECTION_INNER = 0xFF000000;
    public static final int HOVER_OVERLAY = 0x20FFFFFF;

    public static final int TEXT_COLOR = 0xFFFFFFFF;
    public static final int SUBTEXT_COLOR = 0xFFA0A0A0;
    public static final int EMPTY_COLOR = 0xFF808080;

    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("widget/scroller");
    private static final Identifier SCROLLER_BACKGROUND = Identifier.withDefaultNamespace("widget/scroller_background");

    private MenuStyle() {
    }

    public static void body(GuiGraphicsExtractor g, int left, int top, int right, int bottom) {
        g.fill(left, top, right, bottom, LIST_BACKGROUND);
    }

    public static void headerSeparator(GuiGraphicsExtractor g, int left, int right, int y) {
        g.fill(left, y, right, y + 1, SEPARATOR_LIGHT);
        g.fill(left, y + 1, right, y + 2, SEPARATOR_DARK);
    }

    public static void footerSeparator(GuiGraphicsExtractor g, int left, int right, int y) {
        g.fill(left, y, right, y + 1, SEPARATOR_DARK);
        g.fill(left, y + 1, right, y + 2, SEPARATOR_LIGHT);
    }

    public static void verticalSeparator(GuiGraphicsExtractor g, int x, int top, int bottom) {
        g.fill(x, top, x + 1, bottom, SEPARATOR_DARK);
        g.fill(x + 1, top, x + 2, bottom, SEPARATOR_LIGHT);
    }

    public static void selection(GuiGraphicsExtractor g, int left, int top, int right, int bottom) {
        g.fill(left, top, right, bottom, SELECTION_OUTER);
        g.fill(left + 1, top + 1, right - 1, bottom - 1, SELECTION_INNER);
    }

    public static void scrollbar(GuiGraphicsExtractor g, int x, int top, int bottom, int contentHeight, double scroll, int maxScroll) {
        int viewport = bottom - top;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_BACKGROUND, x, top, SCROLLBAR_WIDTH, viewport, ARGB.white(1.0F));

        int thumbHeight = viewport * viewport / Math.max(1, contentHeight);
        thumbHeight = Math.min(viewport, Math.max(32, Math.min(viewport - 8, thumbHeight)));
        int thumbY = top + (int) ((viewport - thumbHeight) * (maxScroll <= 0 ? 0 : scroll / maxScroll));
        g.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER, x, thumbY, SCROLLBAR_WIDTH, thumbHeight, ARGB.white(1.0F));
    }

    public static void centeredText(GuiGraphicsExtractor g, Font font, Component text, int centerX, int y, int color) {
        g.text(font, text, centerX - font.width(text) / 2, y, color);
    }
}