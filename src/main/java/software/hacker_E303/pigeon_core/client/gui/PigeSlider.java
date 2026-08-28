package software.hacker_E303.pigeon_core.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Bounded slider for {@link PigeConfigScreen} numeric entries.
 * The button's label always shows the current value.
 */
@OnlyIn(Dist.CLIENT)
final class PigeSlider extends AbstractSliderButton {

    private final double  realMin;
    private final double  realMax;
    private final boolean integer;

    PigeSlider(int x, int y, int w, int h, double min, double max, double current, boolean integer) {
        super(x, y, w, h, Component.empty(), clamp01((current - min) / (max - min)));
        this.realMin = min;
        this.realMax = max;
        this.integer = integer;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        double real = realMin + (realMax - realMin) * this.value;
        setMessage(integer
                ? Component.literal(String.valueOf(snap(real)))
                : Component.literal(String.format("%.2f", real)));
    }

    @Override
    protected void applyValue() { /* updateMessage() handles everything */ }

    double getRealValue()  { return realMin + (realMax - realMin) * this.value; }
    int    getIntValue()   { return snap(getRealValue()); }

    void setRealValue(double real) {
        this.value = clamp01((real - realMin) / (realMax - realMin));
        updateMessage();
    }

    private static int    snap(double d) { return (int) Math.round(d); }
    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
