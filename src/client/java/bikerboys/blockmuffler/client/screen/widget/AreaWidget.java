package bikerboys.blockmuffler.client.screen.widget;

import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.screen.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.input.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Vec3i;
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

        void onToggleEnabled(Area area);
    }

    private enum Action { CONFIGURE, PLACEMENT, DELETE, TOGGLE }

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

    private static final int NAME_PAD_LEFT = 6;
    private static final int NAME_PAD_RIGHT = 8;
    private static final int NAME_TOP = 5;
    private static final int LINE_GAP = 2;
    private static final int SELECTION_GROW = 2;
    private static final int OUTER_PAD = 4;
    private static final int BTN_GAP = 3;
    private static final int BTN_TEXT_PAD = 6;
    private static final int BTN_HEIGHT = 20;

    private static final Component LABEL_CONFIGURE = Component.literal("Configure");
    private static final Component LABEL_PLACEMENT_ON = Component.literal("Placement: ON");
    private static final Component LABEL_PLACEMENT_OFF = Component.literal("Placement: OFF");
    private static final Component LABEL_ENABLED_ON = Component.literal("Enabled").withColor(TextColor.GREEN);
    private static final Component LABEL_ENABLED_OFF = Component.literal("Disabled").withColor(TextColor.RED);
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
        return getY() + (getHeight() - BTN_HEIGHT) / 2;
    }

    private Slot[] layoutSlots(Font font) {
        int configureW = font.width(LABEL_CONFIGURE) + BTN_TEXT_PAD * 2;
        int placementW = Math.max(font.width(LABEL_PLACEMENT_ON), font.width(LABEL_PLACEMENT_OFF)) + BTN_TEXT_PAD * 2;
        int toggleW = Math.max(font.width(LABEL_ENABLED_ON), font.width(LABEL_ENABLED_OFF)) + BTN_TEXT_PAD * 2;
        int deleteW = font.width(LABEL_DELETE) + BTN_TEXT_PAD * 2;

        int right = getX() + getWidth() - OUTER_PAD;
        Slot delete = new Slot(Action.DELETE, right - deleteW, right);
        Slot placement = new Slot(Action.PLACEMENT, delete.x0() - BTN_GAP - placementW, delete.x0() - BTN_GAP);
        Slot toggle = new Slot(Action.TOGGLE, placement.x0() - BTN_GAP - toggleW, placement.x0() - BTN_GAP);
        Slot configure = new Slot(Action.CONFIGURE, toggle.x0() - BTN_GAP - configureW, toggle.x0() - BTN_GAP);
        return new Slot[]{configure, toggle, placement, delete};
    }

    private Slot slotAt(double mouseX, double mouseY, Font font) {
        if (!isMouseOver(mouseX, mouseY)) return null;
        int top = buttonTop();
        if (mouseY < top || mouseY >= top + BTN_HEIGHT) return null;
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
            case TOGGLE -> listener.onToggleEnabled(area);
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

        if (selected) {
            MenuStyle.selection(g, x - SELECTION_GROW, y - SELECTION_GROW, right + SELECTION_GROW, bottom + SELECTION_GROW);
        } else if (rowHovered) {
            g.fill(x, y, right, bottom, MenuStyle.HOVER_OVERLAY);
        }

        Slot[] slots = layoutSlots(font);

        int textX = x + NAME_PAD_LEFT;
        int textMaxWidth = slots[0].x0() - NAME_PAD_RIGHT - textX;
        g.text(font, fit(font, getMessage().getString(), textMaxWidth), textX, y + NAME_TOP, MenuStyle.TEXT_COLOR);
        g.text(font, fit(font, describe(), textMaxWidth), textX, y + NAME_TOP + font.lineHeight + LINE_GAP, MenuStyle.SUBTEXT_COLOR);

        int btnTop = buttonTop();
        for (Slot slot : slots) {
            boolean hovered = rowHovered
                    && slot.contains(mouseX)
                    && mouseY >= btnTop && mouseY < btnTop + BTN_HEIGHT;

            Component label = switch (slot.action()) {
                case CONFIGURE -> LABEL_CONFIGURE;
                case PLACEMENT -> area.isRenderPlacement() ? LABEL_PLACEMENT_ON : LABEL_PLACEMENT_OFF;
                case DELETE -> LABEL_DELETE;
                case TOGGLE -> area.isEnabled() ? LABEL_ENABLED_ON : LABEL_ENABLED_OFF;
            };

            int btnWidth = slot.x1() - slot.x0();
            g.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    BUTTON_SPRITES.get(true, hovered),
                    slot.x0(),
                    btnTop,
                    btnWidth,
                    BTN_HEIGHT,
                    ARGB.white(1.0F)
            );

            int labelX = slot.x0() + (btnWidth - font.width(label)) / 2;
            int labelY = btnTop + (BTN_HEIGHT - font.lineHeight) / 2 + 1;
            g.text(font, label, labelX, labelY, MenuStyle.TEXT_COLOR);
        }
    }

    private String describe() {
        Vec3i size = area.getEnclosingSize();
        return size.getX() + " x " + size.getY() + " x " + size.getZ();
    }

    private Component fit(Font font, String text, int maxWidth) {
        Component full = Component.literal(text);
        if (font.width(full) <= maxWidth) return full;

        String shortened = text;
        while (!shortened.isEmpty() && font.width(Component.literal(shortened + "...")) > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return Component.literal(shortened + "...");
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}