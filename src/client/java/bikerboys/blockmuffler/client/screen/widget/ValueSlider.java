package bikerboys.blockmuffler.client.screen.widget;

import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.*;

import java.util.function.*;

public class ValueSlider extends AbstractSliderButton {
    private final String label;
    private final int scale;
    private final String suffix;
    private final DoubleConsumer onChange;

    public ValueSlider(int x, int y, int width, int height, String label, double value, DoubleConsumer onChange) {
        this(x, y, width, height, label, value, 100, "%", onChange);
    }

    public ValueSlider(int x, int y, int width, int height, String label, double value, int scale, String suffix, DoubleConsumer onChange) {
        super(x, y, width, height, Component.empty(), Math.max(0.0, Math.min(1.0, value)));
        this.label = label;
        this.scale = scale;
        this.suffix = suffix;
        this.onChange = onChange;
        updateMessage();
    }

    public void setProgress(double progress) {
        this.value = Math.max(0.0, Math.min(1.0, progress));
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(label + ": " + Math.round(value * scale) + suffix));
    }

    @Override
    protected void applyValue() {
        onChange.accept(Math.round(value * scale) / (double) scale);
    }
}