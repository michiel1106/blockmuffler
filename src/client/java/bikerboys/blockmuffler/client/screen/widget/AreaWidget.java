package bikerboys.blockmuffler.client.screen.widget;

import bikerboys.blockmuffler.client.areas.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.input.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;

public class AreaWidget extends AbstractWidget {

    public interface Listener {
        void onConfigure(Area area);

        void onTogglePlacement(Area area);

        void onDelete(Area area);

        void onSelect(Area area);
    }

    private enum Action { CONFIGURE, PLACEMENT, DELETE }

    private record Slot(Action action, int x0, int x1) {
        boolean contains(double mouseX) {
            return mouseX >= x0 && mouseX < x1;
        }
    }

    private static final WidgetSprites BUTTON_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("widget/button"),
            Identifier.withDefaultNamespace("widget/button_disabled"),
            Identifier.withDefaultNamespace("widget/button_highlighted")
    );

    private static final int COLOR_BG = 0x99000000;
    private static final int COLOR_BG_HOVER = 0x99121212;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_OUTLINE = 0xFFFFFFFF;

    private static final int NAME_PAD_LEFT = 8;
    private static final int NAME_PAD_RIGHT = 8;
    private static final int OUTER_PAD = 4;
    private static final int BTN_GAP = 3;
    private static final int BTN_TEXT_PAD = 6;
    private static final int BTN_V_MARGIN = 2;

    private static final Component LABEL_CONFIGURE = Component.literal("Configure");
    private static final Component LABEL_PLACEMENT_ON = Component.literal("Placement: ON");
    private static final Component LABEL_PLACEMENT_OFF = Component.literal("Placement: OFF");
    private static final Component LABEL_DELETE = Component.literal("Delete");

    private final Area area;
    private final Listener listener;

    private int clipTop = Integer.MIN_VALUE;
    private int clipBottom = Integer.MAX_VALUE;
    private boolean selected;

    public AreaWidget(Area area, int x, int y, int width, int height, Listener listener) {
        super(x, y, width, height, Component.literal(area.getName()));
        this.area = area;
        this.listener = listener;
    }

    public Area getArea() {
        return area;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setClip(int top, int bottom) {
        this.clipTop = top;
        this.clipBottom = bottom;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseY >= clipTop && mouseY < clipBottom && super.isMouseOver(mouseX, mouseY);
    }

    private int buttonTop() {
        return getY() + BTN_V_MARGIN;
    }

    private int buttonHeight() {
        return getHeight() - BTN_V_MARGIN * 2;
    }

    private Slot[] layoutSlots(Font font) {
        int configureW = font.width(LABEL_CONFIGURE) + BTN_TEXT_PAD * 2;
        int placementW = Math.max(font.width(LABEL_PLACEMENT_ON), font.width(LABEL_PLACEMENT_OFF)) + BTN_TEXT_PAD * 2;
        int deleteW = font.width(LABEL_DELETE) + BTN_TEXT_PAD * 2;

        int right = getX() + getWidth() - OUTER_PAD;
        Slot delete = new Slot(Action.DELETE, right - deleteW, right);
        Slot placement = new Slot(Action.PLACEMENT, delete.x0() - BTN_GAP - placementW, delete.x0() - BTN_GAP);
        Slot configure = new Slot(Action.CONFIGURE, placement.x0() - BTN_GAP - configureW, placement.x0() - BTN_GAP);
        return new Slot[]{configure, placement, delete};
    }

    private Slot slotAt(double mouseX, double mouseY, Font font) {
        if (!isMouseOver(mouseX, mouseY)) return null;
        int top = buttonTop();
        if (mouseY < top || mouseY >= top + buttonHeight()) return null;
        for (Slot slot : layoutSlots(font)) {
            if (slot.contains(mouseX)) return slot;
        }
        return null;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        listener.onSelect(area);

        Slot slot = slotAt(event.x(), event.y(), Minecraft.getInstance().font);
        if (slot == null) return;

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        switch (slot.action()) {
            case CONFIGURE -> listener.onConfigure(area);
            case PLACEMENT -> listener.onTogglePlacement(area);
            case DELETE -> listener.onDelete(area);
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        Font font = Minecraft.getInstance().font;
        boolean rowHovered = isMouseOver(mouseX, mouseY);

        int x = getX();
        int y = getY();
        int right = x + getWidth();
        int bottom = y + getHeight();

        g.fill(x, y, right, bottom, rowHovered ? COLOR_BG_HOVER : COLOR_BG);

        if (selected) {
            g.fill(x, y, right, y + 1, COLOR_OUTLINE);
            g.fill(x, bottom - 1, right, bottom, COLOR_OUTLINE);
            g.fill(x, y + 1, x + 1, bottom - 1, COLOR_OUTLINE);
            g.fill(right - 1, y + 1, right, bottom - 1, COLOR_OUTLINE);
        }

        Slot[] slots = layoutSlots(font);

        int nameX = x + NAME_PAD_LEFT;
        int nameMaxWidth = slots[0].x0() - NAME_PAD_RIGHT - nameX;
        int nameY = y + (getHeight() - font.lineHeight) / 2 + 1;
        g.text(font, fitName(font, nameMaxWidth), nameX, nameY, COLOR_TEXT);

        int btnTop = buttonTop();
        int btnHeight = buttonHeight();
        for (Slot slot : slots) {
            boolean hovered = rowHovered
                    && slot.contains(mouseX)
                    && mouseY >= btnTop && mouseY < btnTop + btnHeight;

            Component label = switch (slot.action()) {
                case CONFIGURE -> LABEL_CONFIGURE;
                case PLACEMENT -> area.isRenderPlacement() ? LABEL_PLACEMENT_ON : LABEL_PLACEMENT_OFF;
                case DELETE -> LABEL_DELETE;
            };

            int btnWidth = slot.x1() - slot.x0();
            g.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    BUTTON_SPRITES.get(true, hovered),
                    slot.x0(),
                    btnTop,
                    btnWidth,
                    btnHeight,
                    ARGB.white(1.0F)
            );

            int labelX = slot.x0() + (btnWidth - font.width(label)) / 2;
            int labelY = btnTop + (btnHeight - font.lineHeight) / 2 + 1;
            g.text(font, label, labelX, labelY, COLOR_TEXT);
        }
    }

    private Component fitName(Font font, int maxWidth) {
        Component full = getMessage();
        if (font.width(full) <= maxWidth) return full;

        String name = full.getString();
        while (!name.isEmpty() && font.width(Component.literal(name + "...")) > maxWidth) {
            name = name.substring(0, name.length() - 1);
        }
        return Component.literal(name + "...");
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}