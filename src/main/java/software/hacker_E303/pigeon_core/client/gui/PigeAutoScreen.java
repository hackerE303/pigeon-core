package software.hacker_E303.pigeon_core.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.hacker_E303.pigeon_core.common.PigeGui;
import software.hacker_E303.pigeon_core.common.PigeGui.Background;
import software.hacker_E303.pigeon_core.common.gui.GuiContext;
import software.hacker_E303.pigeon_core.common.gui.DisplayAction;
import software.hacker_E303.pigeon_core.common.gui.LayoutBounds;
import software.hacker_E303.pigeon_core.common.gui.PigeAutoContainer;
import software.hacker_E303.pigeon_core.common.gui.PressAction;
import software.hacker_E303.pigeon_core.init.PigeUtils;
import software.hacker_E303.pigeon_core.main.event.network.PigeNetworking;
import software.hacker_E303.pigeon_core.main.event.network.gui.GuiButtonPacket;
import software.hacker_E303.pigeon_core.main.event.network.gui.GuiCharPacket;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Auto-generated screen for every {@link PigeGui} subclass.
 * Created by the framework — do not extend or instantiate manually.
 */
public final class PigeAutoScreen extends AbstractContainerScreen<PigeAutoContainer> {

    private record ButtonRegion(int x, int y, int w, int h, int index) {}

    private final List<ButtonRegion>    buttonRegions  = new ArrayList<>();
    private final Set<ResourceLocation> warnedTextures = new HashSet<>();
    /** Cached texture dimensions — read once per ResourceLocation per screen open. */
    private final Map<ResourceLocation, int[]> texSizeCache = new HashMap<>();
    /** Cached ResourceLocations to avoid re-resolving them every frame. */
    private final Map<String, ResourceLocation> resourceLocationCache = new HashMap<>();
    /** Reusable per-frame DisplayAction to avoid one allocation per entity/item element. */
    private final DisplayAction            scratchDisplay = new DisplayAction();

    private boolean hasBackgroundTexture = false;

    @Nullable private ResourceLocation backgroundTexture = null;

    // Deferred tooltip — set during renderBg, consumed in render()
    @Nullable private Component pendingTooltip = null;
    private int tooltipMX, tooltipMY;

    public PigeAutoScreen(PigeAutoContainer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        
        PigeGui gui = menu.getGui();
        if (gui == null) {
            super.init();
            return;
        }
        LayoutBounds bounds = LayoutBounds.create(-1, -1, 200,  menu.hasPlayerInventorySlots() ? 180 : 160);
        Background background = gui.getBackground(bounds);

        bounds = background.getBounds();
        this.hasBackgroundTexture = background.hasTexture();

        if (bounds.hasSize()) {
            this.imageWidth = bounds.getWidth();
            this.imageHeight = bounds.getHeight();
        }
        super.init();

        if (bounds.getX() != -1)
            this.leftPos = bounds.getX();
        if (bounds.getY() != -1)
            this.topPos = bounds.getY();

        this.inventoryLabelY = this.imageHeight + 10; // hide default inventory label
        
        // Pre-populate cache with background texture (if exists)
        if (this.hasBackgroundTexture) {
            ResourceLocation guiTex = Location.create(Path.TEXTURE.GUI, gui.getId()).from(gui.modid());
            if (textureExists(guiTex)) {
                backgroundTexture = guiTex;
                String rlKey = "rl:" + gui.modid() + ":" + guiTex.toString();
                resourceLocationCache.put(rlKey, guiTex);
            } else {
                warnAboutMissingTexture(guiTex);
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        pendingTooltip = null;
        super.render(gg, mouseX, mouseY, partialTick);
        // Render tip tooltip on top of everything else
        if (pendingTooltip != null) {
            gg.renderTooltip(font, pendingTooltip, tooltipMX, tooltipMY);
        }
        menu.getGui().mouseX = mouseX;
        menu.getGui().mouseY = mouseY;
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        buttonRegions.clear();

        int gx = leftPos, gy = topPos;
        PigeGui gui = menu.getGui();

        // ── Background ────────────────────────────────────────────────────────
        if (backgroundTexture != null) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            gg.blit(backgroundTexture, gx, gy, 0, 0, imageWidth, imageHeight);
        } else if (gui != null && this.hasBackgroundTexture) {
            gg.fill(gx, gy, gx + imageWidth, gy + imageHeight, 0xD0_1E1E2E);
            gg.fill(gx, gy, gx + imageWidth, gy + 16, 0xD0_2D2D4E);
            gg.fill(gx, gy,              gx + imageWidth, gy + 1,              0xFF_5555AA);
            gg.fill(gx, gy + imageHeight - 1, gx + imageWidth, gy + imageHeight, 0xFF_5555AA);
            gg.fill(gx, gy,              gx + 1,              gy + imageHeight, 0xFF_5555AA);
            gg.fill(gx + imageWidth - 1, gy,  gx + imageWidth, gy + imageHeight, 0xFF_5555AA);
        }

        if (gui == null) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        GuiContext ctx = new GuiContext();
        menu.callRenderInterface(ctx, player);

        // ── Auto-layout cursor ────────────────────────────────────────────────
        int autoX    = gx + 10;
        int autoY    = gy + 22;
        int contentW = imageWidth - 20;

        // ── Images ────────────────────────────────────────────────────────────
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        for (GuiContext.ImageElement img : ctx.data().images()) {
            if (!img.visible().getAsBoolean()) continue;
            String rlKey = "rl:" + gui.modid() + ":" + img.texture().toString();
            ResourceLocation rl = resourceLocationCache.computeIfAbsent(rlKey, k -> img.texture().from(gui.modid()));
            if (!textureExists(rl)) { warnAboutMissingTexture(rl); continue; }
            if (img.bounds() != null) {
                LayoutBounds b = img.bounds();
                int[] ts = getTexSize(rl);
                int w = b.hasSize() ? b.getWidth()  : ts[0];
                int h = b.hasSize() ? b.getHeight() : ts[1];
                gg.blit(rl, gx + b.getX(), gy + b.getY(), 0, 0, w, h, ts[0], ts[1]);
            } else {
                // Use cached layout bounds when available
                int[] ts = getTexSize(rl);
                gg.blit(rl, autoX, autoY, 0, 0, ts[0], ts[1], ts[0], ts[1]);
                autoY += ts[1] + 4;
            }
        }

        // ── Entities ──────────────────────────────────────────────────────────
        for (GuiContext.EntityElement el : ctx.data().entities()) {
            if (!el.visible().getAsBoolean()) continue;
            scratchDisplay.reset();
            DisplayAction action = scratchDisplay;
            el.displayAction().accept(action);
            float scale = el.bounds() != null ? el.bounds().getScale() : 30f;
            int ex, ey;
            if (el.bounds() != null) {
                ex = gx + el.bounds().getX();
                ey = gy + el.bounds().getY();
            } else {
                ex = gx + imageWidth / 2;
                ey = autoY + (int) scale;
                autoY += (int) (scale * 2f) + 5;
            }
            renderEntityInGui(gg, ex, ey, scale, el.entity(), action, mouseX, mouseY);
        }

        // ── Item displays ─────────────────────────────────────────────────────
        for (GuiContext.ItemDisplayElement el : ctx.data().itemDisplays()) {
            if (!el.visible().getAsBoolean()) continue;
            scratchDisplay.reset();
            DisplayAction action = scratchDisplay;
            el.displayAction().accept(action);
            float scale = el.bounds() != null ? el.bounds().getScale() : 1.5f;
            int ix, iy;
            if (el.bounds() != null) {
                ix = gx + el.bounds().getX();
                iy = gy + el.bounds().getY();
            } else {
                int pxSize = (int) (16 * scale);
                ix = gx + imageWidth / 2;
                iy = autoY + pxSize / 2;
                autoY += pxSize + 4;
            }
            renderItemInGui(gg, el.item(), ix, iy, scale, action, mouseX, mouseY);
        }

        // ── Texts ─────────────────────────────────────────────────────────────
        for (GuiContext.TextElement txt : ctx.data().texts()) {
            if (!txt.visible().getAsBoolean()) continue;
            if (txt.bounds() != null) {
                LayoutBounds b = txt.bounds();
                gg.drawString(font, txt.text(), gx + b.getX(), gy + b.getY(), 0xDDDDDD, false);
            } else {
                gg.drawCenteredString(font, txt.text(), gx + imageWidth / 2, autoY, 0xDDDDDD);
                autoY += 12;
            }
        }

        // ── Tips ──────────────────────────────────────────────────────────────
        for (GuiContext.TooltipElement tip : ctx.data().tooltips()) {
            if (!tip.visible().getAsBoolean()) continue;
            int tx, ty;
            int hoverW = 0;
            int hoverH = font.lineHeight;

            if (tip.bounds() != null) {
                tx = gx + tip.bounds().getX();
                ty = gy + tip.bounds().getY();
                hoverW = tip.bounds().getWidth();
                hoverH = tip.bounds().getHeight();
            } else {
                tx = autoX;
                ty = autoY;
                autoY += 14;
            }

            if (tip.label() != null) {
                gg.drawString(font, tip.label(), tx, ty, 0xDDDDDD, false);
                int labelW = font.width(tip.label());
                gg.drawString(font, " (?)", tx + labelW, ty, 0x88AAFF, false);
                if (tip.bounds() == null) {
                    hoverW = labelW + font.width(" (?)");
                }
            } else {
                if (tip.bounds() == null) {
                    hoverW = 14;
                    hoverH = 14;
                }
            }
            // Schedule tooltip if hovered
            if (mouseX >= tx && mouseX < tx + hoverW
                    && mouseY >= ty && mouseY < ty + hoverH) {
                pendingTooltip = tip.tip();
                tooltipMX = mouseX;
                tooltipMY = mouseY;
            }
        }

        autoY += 4;

        // ── Buttons ───────────────────────────────────────────────────────────
        List<GuiContext.ButtonElement> buttons = ctx.data().buttons();
        for (int i = 0; i < buttons.size(); i++) {
            GuiContext.ButtonElement btn = buttons.get(i);
            if (!btn.visible().getAsBoolean()) continue;

            int btnX, btnY, btnW, btnH;
            if (btn.bounds() != null) {
                LayoutBounds b = btn.bounds();
                btnX = gx + b.getX();
                btnY = gy + b.getY();
                btnW = b.hasSize() ? b.getWidth()  : contentW;
                btnH = b.hasSize() ? b.getHeight() : 18;
            } else {
                btnX = autoX;
                btnY = autoY;
                btnW = contentW;
                btnH = 18;
                autoY += 20;
            }

            boolean hovered = mouseX >= btnX && mouseX < btnX + btnW
                           && mouseY >= btnY && mouseY < btnY + btnH;

            if (btn.texture() != null) {
                Location loc = btn.texture();
                String baseKey = "rl:" + gui.modid() + ":" + loc.toString();
                ResourceLocation btnTex = resourceLocationCache.computeIfAbsent(baseKey, k -> loc.from(gui.modid()));

                Path btnPath = Path.create(loc.getPath(), loc.getSuffix());
                String enabledKey = baseKey + ":enabled";
                ResourceLocation btxTexEnabled = resourceLocationCache.computeIfAbsent(enabledKey,
                        k -> Location.create(btnPath, loc.getObject() + "_enabled").from(gui.modid()));

                boolean canUseEnabledTexture = hovered && btxTexEnabled != null && textureExists(btxTexEnabled);
                ResourceLocation textureToRender = canUseEnabledTexture ? btxTexEnabled : btnTex;

                if (textureExists(textureToRender)) {
                    int[] ts = getTexSize(textureToRender);
                    int vOffset = 0;
                    if (!canUseEnabledTexture && ts[1] > btnH && hovered) vOffset = btnH;
 
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                    gg.blit(textureToRender, btnX, btnY, 0, vOffset, btnW, btnH, ts[0], ts[1]);
                } else {
                    warnAboutMissingTexture(btnTex);
                    renderDefaultButton(gg, btnX, btnY, btnW, btnH, hovered);
                }
            } else {
                renderDefaultButton(gg, btnX, btnY, btnW, btnH, hovered);
            }
            gg.drawCenteredString(font, btn.label(), btnX + btnW / 2, btnY + (btnH - 8) / 2, 0xFFFFFF);
            buttonRegions.add(new ButtonRegion(btnX, btnY, btnW, btnH, i));
        }
    }

    // ── Labels / title ────────────────────────────────────────────────────────

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        gg.drawCenteredString(font, title, imageWidth / 2, 4, 0xFFFFFF);
    }

    // ── Input handling ────────────────────────────────────────────────────────

    @Override
    public boolean charTyped(char c, int modifiers) {
        PigeGui gui = menu.getGui();
        if (gui != null && !gui.onCharTyped(c)) {
            PigeNetworking.sendToServer(new GuiCharPacket(c));
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        PigeGui gui = menu.getGui();
        if (gui != null && !gui.onKeyPressed(keyCode, scanCode, modifiers)) {
            PigeNetworking.sendToServer(new GuiCharPacket(keyCode));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (ButtonRegion region : buttonRegions) {
                if (mouseX >= region.x() && mouseX < region.x() + region.w()
                 && mouseY >= region.y() && mouseY < region.y() + region.h()) {
                    fireButtonClick(region.index());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void fireButtonClick(int buttonIndex) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || menu.getGui() == null) return;

        GuiContext ctx = new GuiContext();
        menu.callRenderInterface(ctx, player);

        List<GuiContext.ButtonElement> buttons = ctx.data().buttons();
        if (buttonIndex >= 0 && buttonIndex < buttons.size()) {
            PressAction clientAction = new PressAction(true, player);
            try {
                buttons.get(buttonIndex).action().accept(clientAction);
            } catch (PressAction.Abort ignored) {}

            SoundEvent sound = menu.getGui().getButtonSound();
            if (sound != null && !clientAction.isDenied())
                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), sound, SoundSource.MASTER, 1.0f, 1.0f, false);
        }
        PigeNetworking.sendToServer(new GuiButtonPacket(buttonIndex));
    }

    // ── Behaviour flags ───────────────────────────────────────────────────────

    @Override
    public boolean isPauseScreen() {
        return menu.getGui() != null && menu.getGui().shouldPauseGame();
    }

    // ── Entity rendering ──────────────────────────────────────────────────────

    /**
     * Renders an entity preview at (cx, cy) with the given pixel scale.
     * Only {@link LivingEntity} is supported; other entity types are silently skipped.
     */
    private static void renderEntityInGui(GuiGraphics gg, int cx, int cy, float scale,
                                          net.minecraft.world.entity.Entity entity,
                                          DisplayAction action, int mouseX, int mouseY) {
        if (!(entity instanceof LivingEntity living)) return;

        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        if (action.data().catchMouse()) {
            // renderEntityInInventoryFollowsMouse uses Math.atan(p / 40.0) for each param,
            // then: yBodyRot = 180 + f*20  and  xRot = -f1*20
            //
            // To make the entity look TOWARD the mouse relative to its own center:
            //   p4 = (mouseX - cx) * d  → positive when mouse right → entity turns right ✓
            //   p5 = (cy - mouseY) * d  → positive when mouse up (Y inverted: screen-Y grows down)
            //                              → f1 > 0 → xRot = -f1*20 < 0 → entity looks up ✓
            //
            // d = mouseDelayFactor: 1.0 = full tracking, 0.0 = always faces camera
            int eyeHeight = (int) (entity.getEyeHeight() * scale);
            float d  = action.data().mouseDelayFactor();
            float p4 = (cx - mouseX) * d;
            float p5 = ((cy - eyeHeight) - mouseY) * d;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    gg, cx, cy, (int) scale, p4, p5, living);

            if (action.data().glint()) {
                // Replicate the pose that renderEntityInInventoryFollowsMouse applies internally
                // so the glint pass lines up with the already-rendered entity
                float yaw   = 180f + (float)(Math.atan(p4 / 40.0)) * 20f;
                float pitch = -(float)(Math.atan(p5 / 40.0)) * 20f;
                var ps = gg.pose();
                ps.pushPose();
                ps.translate(cx, cy, 50f);
                ps.scale(scale, scale, -scale);
                ps.mulPose(Axis.ZP.rotationDegrees(180f));
                ps.mulPose(Axis.YP.rotationDegrees(yaw));
                ps.mulPose(Axis.XP.rotationDegrees(pitch));
                Lighting.setupForEntityInInventory();
                dispatcher.setRenderShadow(false);
                net.minecraft.client.renderer.RenderType glintType =
                        net.minecraft.client.renderer.RenderType.armorEntityGlint();
                dispatcher.render(living, 0, 0, 0, 0f, 1f, ps, rt -> buffers.getBuffer(glintType), 15728880);
                buffers.endBatch(glintType);
                dispatcher.setRenderShadow(true);
                ps.popPose();
                Lighting.setupFor3DItems();
            }
            return;
        }

        // Custom rotation via PoseStack + EntityRenderDispatcher
        var poseStack = gg.pose();
        poseStack.pushPose();
        poseStack.translate(cx, cy, 50.0f);
        poseStack.scale(scale, scale, -scale);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180f));

        if (action.data().spinSpeed() != 0f) {
            float angle = (System.currentTimeMillis() % 360_000L) / 1000f
                          * action.data().spinSpeed();
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        }
        if (action.data().rotX() != 0f) poseStack.mulPose(Axis.XP.rotationDegrees(action.data().rotX()));
        if (action.data().rotY() != 0f) poseStack.mulPose(Axis.YP.rotationDegrees(action.data().rotY()));
        if (action.data().rotZ() != 0f) poseStack.mulPose(Axis.ZP.rotationDegrees(action.data().rotZ()));

        Lighting.setupForEntityInInventory();
        dispatcher.setRenderShadow(false);
        dispatcher.render(living, 0, 0, 0, 0f, 1f, poseStack, buffers, 15728880);
        buffers.endBatch();

        if (action.data().glint()) {
            net.minecraft.client.renderer.RenderType glintType =
                    net.minecraft.client.renderer.RenderType.armorEntityGlint();
            dispatcher.render(living, 0, 0, 0, 0f, 1f, poseStack, rt -> buffers.getBuffer(glintType), 15728880);
            buffers.endBatch(glintType);
        }

        dispatcher.setRenderShadow(true);
        poseStack.popPose();
        Lighting.setupFor3DItems();
    }

    // ── Item display rendering ─────────────────────────────────────────────────

    private static final net.minecraft.nbt.ListTag GLINT_ENCHANTMENTS;
    static {
        GLINT_ENCHANTMENTS = new net.minecraft.nbt.ListTag();
        net.minecraft.nbt.CompoundTag ench = new net.minecraft.nbt.CompoundTag();
        ench.putString("id", "minecraft:unbreaking");
        ench.putShort("lvl", (short) 1);
        GLINT_ENCHANTMENTS.add(ench);
    }

    /** Renders an {@link ItemStack} preview centered at (cx, cy) with the given scale multiplier. */
    private static void renderItemInGui(GuiGraphics gg, ItemStack item,
                                        int cx, int cy, float scale,
                                        DisplayAction action, int mouseX, int mouseY) {
        if (item.isEmpty()) return;

        var poseStack = gg.pose();
        poseStack.pushPose();
        poseStack.translate(cx, cy, 0);
        poseStack.scale(scale, scale, scale);

        // gg.renderItem internally does poseStack.translate(x+8, y+8, 150).
        // With x=y=-8 this becomes translate(0, 0, 150), so the item center lives at z=150.
        // We must rotate around that z=150 pivot or the item orbits instead of spinning in place.
        poseStack.translate(0, 0, 150);
        if (action.data().catchMouse()) {
            // Consistent sign convention with entity catchMouse:
            //   yaw   = (mouseX - cx)*d → positive when mouse right → item turns right ✓
            //   pitch = (cy - mouseY)*d → positive when mouse up (screen-Y grows down) → item tilts up ✓
            float d  = action.data().mouseDelayFactor();
            float yaw   = (mouseX - cx) * d * 0.5f;   // 0.5 to match the atan(p/40)*20 sensitivity

            float pitchCorrection = 30.0f;
            float pitch = ((cy - mouseY) * d * 0.5f) - pitchCorrection;

            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        } else if (action.data().spinSpeed() != 0f) {
            float angle = (System.currentTimeMillis() % 360_000L) / 1000f
                          * action.data().spinSpeed();
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        }
        if (action.data().rotX() != 0f) poseStack.mulPose(Axis.XP.rotationDegrees(action.data().rotX()));
        if (action.data().rotY() != 0f) poseStack.mulPose(Axis.YP.rotationDegrees(action.data().rotY()));
        if (action.data().rotZ() != 0f) poseStack.mulPose(Axis.ZP.rotationDegrees(action.data().rotZ()));
        poseStack.translate(0, 0, -150);   // undo z-shift so renderItem re-applies its own

        ItemStack renderStack = item;
        if (action.data().glint() && !item.hasFoil()) {
            renderStack = item.copy();
            renderStack.getOrCreateTag().put("Enchantments", GLINT_ENCHANTMENTS);
        }
        gg.renderItem(renderStack, -8, -8);
        poseStack.popPose();
    }

    // ── Button style ──────────────────────────────────────────────────────────

    private void renderDefaultButton(GuiGraphics gg, int x, int y, int w, int h, boolean hovered) {
        gg.fill(x, y, x + w, y + h, hovered ? 0xFF_5577BB : 0xFF_334466);
        gg.fill(x, y, x + w, y + 1, hovered ? 0xFF_88AAEE : 0xFF_445588);
    }

    // ── Texture utilities ─────────────────────────────────────────────────────

    private boolean textureExists(ResourceLocation rl) {
        return Minecraft.getInstance().getResourceManager().getResource(rl).isPresent();
    }

    /** Returns {width, height} of the PNG at rl, falling back to {16,16} on error. Results are cached. */
    private int[] getTexSize(ResourceLocation rl) {
        return texSizeCache.computeIfAbsent(rl, loc -> {
            try {
                var res = Minecraft.getInstance().getResourceManager().getResourceOrThrow(loc);
                try (var stream = res.open(); NativeImage img = NativeImage.read(stream)) {
                    return new int[]{img.getWidth(), img.getHeight()};
                }
            } catch (Exception e) {
                return new int[]{16, 16};
            }
        });
    }

    private void warnAboutMissingTexture(ResourceLocation rl) {
        if (warnedTextures.add(rl)) {
            Player player = Minecraft.getInstance().player;
            if (player != null) PigeUtils.missingResourceWarning(player, rl);
        }
    }
}