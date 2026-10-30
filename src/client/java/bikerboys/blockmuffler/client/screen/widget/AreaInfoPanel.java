package bikerboys.blockmuffler.client.screen.widget;

import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.screen.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.*;

import java.util.*;
import java.util.function.*;

public class AreaInfoPanel {
    private record Entry(String label, String value) {
    }

    private enum Mode {
        ORIGIN_1("Origin 1"),
        ORIGIN_2("Origin 2"),
        BOTH("Both");

        private final Component label;

        Mode(String text) {
            this.label = Component.literal(text);
        }

        Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final int PADDING = 6;
    private static final int SECTION_GAP = 4;
    private static final int ROW_GAP = 2;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MODE_BUTTON_HEIGHT = 12;
    private static final int MODE_BUTTON_PADDING = 8;
    private static final int AXIS_LABEL_WIDTH = 14;
    private static final int STEP_BUTTON_WIDTH = 30;
    private static final int ROTATE_BUTTON_WIDTH = 28;

    private static final String[] AXIS_NAMES = {"X:", "Y:", "Z:"};

    private static final Component TITLE = Component.literal("Area Info");
    private static final Component EMPTY = Component.literal("Select an area");
    private static final Component ORIGIN_TITLE = Component.literal("Placement origin");
    private static final Component EDIT_LABEL = Component.literal("Edit:");

    private final EditBox[] boxes = new EditBox[3];
    private final PanelButton[] steppers = new PanelButton[3];
    private final String[] lastText = {"", "", ""};
    private final List<AbstractWidget> widgets = new ArrayList<>();

    private PanelButton lockButton;
    private PanelButton modeButton;
    private PanelButton moveButton;
    private PanelButton rotateLeftButton;
    private PanelButton rotateRightButton;

    private Mode mode = Mode.BOTH;

    private Font font;
    private Area area;
    private Area lastArea;
    private boolean syncing;

    private int left, top, right, bottom;

    public void setBounds(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    private int boxWidth() {
        return right - left - PADDING * 2 - AXIS_LABEL_WIDTH - STEP_BUTTON_WIDTH - 4;
    }

    public void init(Font font, Consumer<AbstractWidget> register) {
        this.font = font;
        this.lastArea = null;
        widgets.clear();

        lockButton = new PanelButton(0, 0, 10, BUTTON_HEIGHT, Component.empty(), this::toggleLocked);
        widgets.add(lockButton);

        modeButton = new PanelButton(0, 0, 10, MODE_BUTTON_HEIGHT, mode.label, this::cycleMode);
        widgets.add(modeButton);

        for (int i = 0; i < 3; i++) {
            int axis = i;

            EditBox box = new EditBox(font, 0, 0, boxWidth(), BUTTON_HEIGHT, Component.literal(AXIS_NAMES[i]));
            box.setResponder(s -> onBoxChanged(axis, s));
            boxes[i] = box;
            widgets.add(box);

            PanelButton stepper = PanelButton.stepper(0, 0, STEP_BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("+/-"), delta -> step(axis, delta));
            steppers[i] = stepper;
            widgets.add(stepper);
        }

        moveButton = new PanelButton(0, 0, 10, BUTTON_HEIGHT, Component.literal("Move to player"), this::moveToPlayer);
        widgets.add(moveButton);

        rotateLeftButton = new PanelButton(0, 0, ROTATE_BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("-90"), () -> rotate(-1));
        widgets.add(rotateLeftButton);

        rotateRightButton = new PanelButton(0, 0, ROTATE_BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("+90"), () -> rotate(1));
        widgets.add(rotateRightButton);

        for (AbstractWidget widget : widgets) {
            register.accept(widget);
        }
    }

    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, Area area) {
        this.area = area;

        boolean hasArea = area != null;
        for (AbstractWidget widget : widgets) {
            widget.visible = hasArea;
        }

        if (!hasArea) {
            unfocusBoxes();
            lastArea = null;
            MenuStyle.centeredText(g, font, EMPTY, (left + right) / 2, top + PADDING, MenuStyle.EMPTY_COLOR);
            return;
        }

        int boxWidth = boxWidth();
        for (EditBox box : boxes) {
            box.setWidth(boxWidth);
        }

        if (area != lastArea) {
            lastArea = area;
            unfocusBoxes();
            syncBoxes(true);
        } else {
            syncBoxes(false);
        }

        boolean locked = area.isLocked();
        lockButton.setMessage(Component.literal("Locked: " + (locked ? "ON" : "OFF")));
        modeButton.setMessage(mode.label);
        moveButton.active = !locked;
        rotateLeftButton.active = !locked;
        rotateRightButton.active = !locked;
        for (int i = 0; i < 3; i++) {
            steppers[i].active = !locked;
            boxes[i].setEditable(!locked);
        }

        int x = left + PADDING;
        int innerWidth = right - left - PADDING * 2;
        int y = top + PADDING;

        g.enableScissor(left, top, right, bottom);

        MenuStyle.centeredText(g, font, TITLE, (left + right) / 2, y, MenuStyle.TEXT_COLOR);
        y += font.lineHeight + 6;

        for (Entry entry : entries(area)) {
            Component label = Component.literal(entry.label());
            g.text(font, label, x, y, MenuStyle.SUBTEXT_COLOR);
            int valueX = x + font.width(label) + 3;
            g.text(font, fit(entry.value(), x + innerWidth - valueX), valueX, y, MenuStyle.TEXT_COLOR);
            y += font.lineHeight + 2;
        }
        y += SECTION_GAP;

        place(lockButton, x, y, innerWidth);
        y += BUTTON_HEIGHT + SECTION_GAP;

        g.text(font, ORIGIN_TITLE, x, y, MenuStyle.SUBTEXT_COLOR);
        y += font.lineHeight + 2;

        int modeWidth = modeButtonWidth();
        int modeTextY = y + (MODE_BUTTON_HEIGHT - font.lineHeight) / 2 + 1;
        g.text(font, EDIT_LABEL, x, modeTextY, MenuStyle.SUBTEXT_COLOR);
        place(modeButton, x + innerWidth - modeWidth, y, modeWidth);
        y += MODE_BUTTON_HEIGHT + ROW_GAP;

        for (int i = 0; i < 3; i++) {
            int textY = y + (BUTTON_HEIGHT - font.lineHeight) / 2 + 1;
            g.text(font, Component.literal(AXIS_NAMES[i]), x, textY, MenuStyle.TEXT_COLOR);
            place(boxes[i], x + AXIS_LABEL_WIDTH + 2, y, boxWidth);
            place(steppers[i], x + innerWidth - STEP_BUTTON_WIDTH, y, STEP_BUTTON_WIDTH);
            y += BUTTON_HEIGHT + ROW_GAP;
        }
        y += SECTION_GAP - ROW_GAP;

        place(moveButton, x, y, innerWidth);
        y += BUTTON_HEIGHT + SECTION_GAP;

        int rotateRightX = x + innerWidth - ROTATE_BUTTON_WIDTH;
        int rotateLeftX = rotateRightX - 2 - ROTATE_BUTTON_WIDTH;
        int rotationTextY = y + (BUTTON_HEIGHT - font.lineHeight) / 2 + 1;
        g.text(font, fit("Rotation: " + area.getRotation(), rotateLeftX - 4 - x), x, rotationTextY, MenuStyle.TEXT_COLOR);
        place(rotateLeftButton, rotateLeftX, y, ROTATE_BUTTON_WIDTH);
        place(rotateRightButton, rotateRightX, y, ROTATE_BUTTON_WIDTH);

        for (AbstractWidget widget : widgets) {
            widget.extractRenderState(g, mouseX, mouseY, a);
        }

        g.disableScissor();
    }

    private int modeButtonWidth() {
        int widest = 0;
        for (Mode value : Mode.values()) {
            widest = Math.max(widest, font.width(value.label));
        }
        return widest + MODE_BUTTON_PADDING;
    }

    private void place(AbstractWidget widget, int x, int y, int width) {
        widget.setX(x);
        widget.setY(y);
        widget.setWidth(width);
    }

    private BlockPos currentPos() {
        return switch (mode) {
            case ORIGIN_1 -> area.getPos1();
            case ORIGIN_2 -> area.getPos2();
            case BOTH -> area.getOrigin();
        };
    }

    private void applyPos(BlockPos target) {
        switch (mode) {
            case ORIGIN_1 -> area.movePos1To(target);
            case ORIGIN_2 -> area.movePos2To(target);
            case BOTH -> area.moveTo(target);
        }
    }

    private void unfocusBoxes() {
        for (EditBox box : boxes) {
            if (box != null && box.isFocused()) {
                box.setFocused(false);
            }
        }
    }

    private void syncBoxes(boolean force) {
        if (area == null) return;

        BlockPos pos = currentPos();
        int[] values = {pos.getX(), pos.getY(), pos.getZ()};

        syncing = true;
        for (int i = 0; i < 3; i++) {
            String text = Integer.toString(values[i]);
            if ((force || !boxes[i].isFocused()) && !boxes[i].getValue().equals(text)) {
                boxes[i].setValue(text);
            }
            lastText[i] = boxes[i].getValue();
        }
        syncing = false;
    }

    private void onBoxChanged(int axis, String text) {
        if (syncing) return;

        if (!text.matches("-?\\d{0,9}")) {
            syncing = true;
            boxes[axis].setValue(lastText[axis]);
            syncing = false;
            return;
        }
        lastText[axis] = text;

        if (area == null || area.isLocked()) return;

        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return;
        }

        BlockPos pos = currentPos();
        applyPos(new BlockPos(
                axis == 0 ? value : pos.getX(),
                axis == 1 ? value : pos.getY(),
                axis == 2 ? value : pos.getZ()
        ));
    }

    private void step(int axis, int delta) {
        if (area == null || area.isLocked()) return;

        applyPos(currentPos().offset(axis == 0 ? delta : 0, axis == 1 ? delta : 0, axis == 2 ? delta : 0));
        syncBoxes(true);
    }

    private void cycleMode() {
        mode = mode.next();
        unfocusBoxes();
        syncBoxes(true);
    }

    private void toggleLocked() {
        if (area == null) return;
        area.setLocked(!area.isLocked());
    }

    private void moveToPlayer() {
        var player = Minecraft.getInstance().player;
        if (area == null || player == null) return;

        applyPos(player.blockPosition());
        syncBoxes(true);
    }

    private void rotate(int quarterTurns) {
        if (area == null) return;

        area.rotate(quarterTurns);
        syncBoxes(true);
    }

    private List<Entry> entries(Area area) {
        BlockPos origin = area.getOrigin();
        Vec3i size = area.getEnclosingSize();

        return List.of(
                new Entry("Name:", area.getName()),
                new Entry("Origin:", origin.getX() + ", " + origin.getY() + ", " + origin.getZ()),
                new Entry("Size:", size.getX() + " x " + size.getY() + " x " + size.getZ()),
                new Entry("Volume:", String.format(Locale.ROOT, "%,d", area.getTotalVolume()))
        );
    }

    private Component fit(String text, int maxWidth) {
        Component full = Component.literal(text);
        if (font.width(full) <= maxWidth) return full;

        String shortened = text;
        while (!shortened.isEmpty() && font.width(Component.literal(shortened + "...")) > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return Component.literal(shortened + "...");
    }
}