package bikerboys.blockmuffler.client.screen.widget;

import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.*;

import java.util.function.*;

public class PanelButton extends AbstractButton {
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_COLOR_INACTIVE = 0xFFA0A0A0;

    private final Runnable action;
    private final IntConsumer onStep;

    private PanelButton(int x, int y, int width, int height, Component message, Runnable action, IntConsumer onStep) {
        super(x, y, width, height, message);
        this.action = action;
        this.onStep = onStep;
    }

    public PanelButton(int x, int y, int width, int height, Component message, Runnable action) {
        this(x, y, width, height, message, action, null);
    }

    public static PanelButton stepper(int x, int y, int width, int height, Component message, IntConsumer onStep) {
        return new PanelButton(x, y, width, height, message, null, onStep);
    }

    private static int stepSize() {
        boolean shift = Minecraft.getInstance().hasShiftDown();
        boolean alt = Minecraft.getInstance().hasAltDown();
        if (shift && alt) return 32;
        if (shift) return 8;
        if (alt) return 4;
        return 1;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (onStep != null) {
            onStep.accept(stepSize());
        } else if (action != null) {
            action.run();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (onStep != null && event.button() == 1 && isActive() && isMouseOver(event.x(), event.y())) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            onStep.accept(-stepSize());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (onStep == null || !isActive() || scrollY == 0 || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        onStep.accept(scrollY > 0 ? stepSize() : -stepSize());
        return true;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        extractDefaultSprite(g);

        Font font = Minecraft.getInstance().font;
        Component label = getMessage();
        int labelX = getX() + (getWidth() - font.width(label)) / 2;
        int labelY = getY() + (getHeight() - font.lineHeight) / 2 + 1;
        g.text(font, label, labelX, labelY, isActive() ? TEXT_COLOR : TEXT_COLOR_INACTIVE);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}