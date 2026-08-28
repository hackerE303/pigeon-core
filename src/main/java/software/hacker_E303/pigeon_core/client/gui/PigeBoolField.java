package software.hacker_E303.pigeon_core.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Boolean toggle rendered with FIELD / FIELD_ENABLED textures.
 * Clicking flips the value; "True" is shown in green, "False" in red.
 * Replaces PigeCheckbox in entry rows.
 */
@OnlyIn(Dist.CLIENT)
final class PigeBoolField extends AbstractWidget {

    private boolean value;

    PigeBoolField(int x, int y, int w, int h, boolean value) {
        super(x, y, w, h, Component.empty());
        this.value = value;
    }

    public boolean getValue() { return value; }

    @Override
    public void onClick(double x, double y) {
        value = !value;
    }

    @Override
    public void renderWidget(GuiGraphics gg, int mx, int my, float dt) {
        gg.blit(isHovered() ? PigeConfigTextures.FIELD_ENABLED_X1 : PigeConfigTextures.FIELD_X1,
                getX(), getY(), 0, 0, width, height, width, height);
        gg.drawCenteredString(Minecraft.getInstance().font,
                value ? "true" : "false",
                getX() + width / 2, getY() + (height - 8) / 2,
                0xAAAAAA); // §7 — same color regardless of value
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
