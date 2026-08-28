package software.hacker_E303.pigeon_core.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Textured button with separate normal / hover textures.
 * Textures are blitted at 1:1 natural size — the texture file must be
 * exactly {@code width × height} pixels.
 */
@OnlyIn(Dist.CLIENT)
final class PigeButton extends AbstractWidget {

    private final ResourceLocation texNormal;
    private final ResourceLocation texHover;
    private final Runnable action;

    PigeButton(int x, int y, int w, int h, Component label,
               ResourceLocation normal, ResourceLocation hover, Runnable action) {
        super(x, y, w, h, label);
        this.texNormal = normal;
        this.texHover  = hover;
        this.action    = action;
    }

    @Override
    public void onClick(double x, double y) {
        action.run();
    }

    @Override
    public void renderWidget(GuiGraphics gg, int mx, int my, float dt) {
        // Blit at natural 1:1 size (texW/texH == widget w/h, no scaling)
        gg.blit(isHovered() ? texHover : texNormal,
                getX(), getY(), 0, 0, width, height, width, height);
        gg.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, 0xFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
