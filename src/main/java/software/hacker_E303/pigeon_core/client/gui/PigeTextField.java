package software.hacker_E303.pigeon_core.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Styled text field: FIELD_Xn / FIELD_ENABLED_Xn textures, three width tiers (40/60/80 px),
 * automatic width adjustment while typing, validation colouring and auto-correct on defocus.
 *
 * Width tiers are chosen based on the text content (4 px padding each side):
 *   FIELD_W_S (40 px) ← narrow values
 *   FIELD_W_M (60 px)
 *   FIELD_W_L (80 px) ← long strings / resource locations
 *
 * Text rendering:
 *   – Not focused: drawn centered, §7 (valid) or §c (invalid).
 *   – Focused:     EditBox text + cursor at left, vertically centred via PoseStack
 *                  translation (setBordered(false) normally renders at y+0).
 *
 * Validation (§c):
 *   – Integer: must be a whole number, within [min, max] if set.
 *   – Double:  must parse as a number, within [min, max] if set.
 *   – Item:    must resolve to a registered item.
 *   – String:  always valid.
 *
 * Auto-correct on defocus:
 *   – Integer: rounds fractional input, clamps to [min, max], resets on parse failure.
 *   – Double:  clamps to [min, max], resets on parse failure.
 *              Uses String.valueOf() to preserve ".0" suffix.
 *   – Item:    resets to default if RL cannot be resolved.
 */
@OnlyIn(Dist.CLIENT)
final class PigeTextField extends EditBox {

    // Width tiers (referenced by PigeConfigScreen and PigeBoolField)
    static final int FIELD_W_S = 40;
    static final int FIELD_W_M = 60;
    static final int FIELD_W_L = 80;

    // Full-alpha ARGB: alpha=0 can be transparent in MC 1.20.1 ARGB text rendering
    private static final int COLOR_VALID   = 0xFFAAAAAA; // §7 gray,  full alpha
    private static final int COLOR_INVALID = 0xFFFF5555; // §c red,   full alpha

    // Mutable — change when width tier changes while typing
    private ResourceLocation texNormal;
    private ResourceLocation texEnabled;

    private final int      rightEdge;  // x + w at construction → field stays right-aligned here
    private final Class<?> type;
    @Nullable private final Object min;
    @Nullable private final Object max;
    private final Object   defaultVal; // raw value (or RL string for Item) used for reset

    PigeTextField(Font font, int x, int y, int w, int h,
                  ResourceLocation texNormal, ResourceLocation texEnabled,
                  Class<?> type, @Nullable Object min, @Nullable Object max, Object defaultVal) {
        super(font, x, y, w, h, Component.empty());
        setBordered(false); // no border/background drawn by EditBox
        setTextColor(COLOR_VALID);
        this.texNormal  = texNormal;
        this.texEnabled = texEnabled;
        this.rightEdge  = x + w;
        this.type       = type;
        this.min        = min;
        this.max        = max;
        this.defaultVal = defaultVal;
    }

    // ── Width tier helpers (also used by PigeConfigScreen) ────────────────────

    /** Returns the narrowest tier that accommodates {@code text} (4 px padding each side). */
    static int widthFor(Font f, String text) {
        int needed = f.width(text) + 8;
        if (needed <= FIELD_W_S) return FIELD_W_S;
        if (needed <= FIELD_W_M) return FIELD_W_M;
        return FIELD_W_L;
    }

    static ResourceLocation[] texForWidth(int w) {
        return switch (w) {
            case FIELD_W_S -> new ResourceLocation[]{
                    PigeConfigTextures.FIELD_X1, PigeConfigTextures.FIELD_ENABLED_X1};
            case FIELD_W_M -> new ResourceLocation[]{
                    PigeConfigTextures.FIELD_X2, PigeConfigTextures.FIELD_ENABLED_X2};
            default        -> new ResourceLocation[]{
                    PigeConfigTextures.FIELD_X4, PigeConfigTextures.FIELD_ENABLED_X4};
        };
    }

    // ── Dynamic width ─────────────────────────────────────────────────────────

    private void adjustWidth() {
        int newW = widthFor(Minecraft.getInstance().font, getValue());
        if (newW != width) {
            width = newW;
            setX(rightEdge - newW);
            ResourceLocation[] tx = texForWidth(newW);
            texNormal  = tx[0];
            texEnabled = tx[1];
        }
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        boolean r = super.charTyped(c, modifiers);
        adjustWidth();
        return r;
    }

    // ── Validation ────────────────────────────────────────────────────────────

    boolean isCurrentValid() {
        String raw = getValue().trim();
        if (raw.isEmpty()) return false;

        if (type == Integer.class) {
            // Integer.parseInt rejects "5d", "5.5", type-suffix literals, etc.
            try {
                int v = Integer.parseInt(raw);
                if (min instanceof Number mn && v < mn.intValue())  return false;
                if (max instanceof Number mx && v > mx.intValue())  return false;
                return true;
            } catch (NumberFormatException e) { return false; }
        }
        // Double or Float: reject Java literal type-suffixes ('d','f') as invalid input
        if (type == Double.class || type == Float.class) {
            char last = raw.charAt(raw.length() - 1);
            if (last == 'd' || last == 'D' || last == 'f' || last == 'F') return false;
            try {
                double v = Double.parseDouble(raw);
                if (min instanceof Number mn && v < mn.doubleValue()) return false;
                if (max instanceof Number mx && v > mx.doubleValue()) return false;
                return true;
            } catch (NumberFormatException e) { return false; }
        }
        if (type == Item.class) {
            try { return ForgeRegistries.ITEMS.getValue(new ResourceLocation(raw)) != null; }
            catch (Exception e) { return false; }
        }
        return true; // String always valid
    }

    // ── Auto-correct on defocus ───────────────────────────────────────────────

    @Override
    public void setFocused(boolean focused) {
        boolean was = isFocused();
        super.setFocused(focused);
        if (was && !focused) autoCorrect();
    }

    private void autoCorrect() {
        String raw = getValue().trim();
        if (raw.isEmpty()) { setValue(String.valueOf(defaultVal)); adjustWidth(); return; }

        if (type == Integer.class) {
            try {
                int v = (int) Math.round(Double.parseDouble(raw));
                if (min instanceof Number mn) v = Math.max(v, mn.intValue());
                if (max instanceof Number mx) v = Math.min(v, mx.intValue());
                setValue(String.valueOf(v));
            } catch (NumberFormatException e) {
                setValue(String.valueOf(defaultVal));
            }
        } else if (type == Double.class || type == Float.class) {
            try {
                double v = Double.parseDouble(raw);
                if (min instanceof Number mn) v = Math.max(v, mn.doubleValue());
                if (max instanceof Number mx) v = Math.min(v, mx.doubleValue());
                setValue(String.valueOf(v));
            } catch (NumberFormatException e) {
                setValue(String.valueOf(defaultVal));
            }
        } else if (type == Item.class) {
            try {
                if (ForgeRegistries.ITEMS.getValue(new ResourceLocation(raw)) == null)
                    setValue(String.valueOf(defaultVal));
            } catch (Exception e) {
                setValue(String.valueOf(defaultVal));
            }
        }
        // String: always valid, nothing to correct
        adjustWidth(); // recompute tier after correction
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter (257) or ESC (256) while focused → defocus (auto-correct fires via setFocused)
        if (isFocused() && (keyCode == 257 || keyCode == 256)) {
            setFocused(false);
            return true;
        }
        boolean r = super.keyPressed(keyCode, scanCode, modifiers);
        adjustWidth(); // catches Backspace, Delete, Ctrl+V, Ctrl+A, etc.
        return r;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void renderWidget(GuiGraphics gg, int mx, int my, float dt) {
        boolean focused = isFocused();
        boolean valid   = isCurrentValid();
        int     color   = valid ? COLOR_VALID : COLOR_INVALID;

        // Texture — drawn before inner scissor so the full field is visible
        gg.blit(focused || isHovered() ? texEnabled : texNormal,
                getX(), getY(), 0, 0, width, height, width, height);

        // Text clipped to field interior (1 px inside border).
        // GuiGraphics scissor is stack-aware in 1.20.1: intersects with outer scissor,
        // restores it on disableScissor().
        gg.enableScissor(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1);
        if (focused) {
            Font f2  = Minecraft.getInstance().font;
            String txt = getValue();
            int textW   = f2.width(txt);
            int offsetX = textW < width - 2 ? (width - 2 - textW) / 2 : 0;
            int offsetY = (height - 8) / 2;

            setTextColor(color);
            gg.pose().pushPose();
            gg.pose().translate(offsetX, offsetY, 0.0);
            super.renderWidget(gg, mx, my, dt);
            gg.pose().popPose();
            setTextColor(COLOR_VALID);
        } else {
            Font f = Minecraft.getInstance().font;
            String txt = getValue();
            int textX = getX() + (width - f.width(txt)) / 2;
            int textY = getY() + (height - 8) / 2;
            gg.drawString(f, txt, textX, textY, color, false);
        }
        gg.disableScissor();
    }
}
