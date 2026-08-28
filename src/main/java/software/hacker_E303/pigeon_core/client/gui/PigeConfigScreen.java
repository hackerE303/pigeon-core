package software.hacker_E303.pigeon_core.client.gui;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import software.hacker_E303.pigeon_core.PigeonCore;
import software.hacker_E303.pigeon_core.common.PigeConfig;
import software.hacker_E303.pigeon_core.common.config.ConfigEntry;
import software.hacker_E303.pigeon_core.common.config.ConfigFolder;
import software.hacker_E303.pigeon_core.common.config.FolderItem;

/**
 * Forge config screen generated dynamically from a {@link PigeConfig}.
 *
 * <ul>
 *   <li>Top-level folders → tabs across the top.</li>
 *   <li>Nested folders → clickable <em>folder buttons</em>.  Clicking navigates
 *       into that folder (file-manager style); a breadcrumb bar shows the path
 *       and a ← back button returns to the parent level.</li>
 *   <li>Integer / Double entries with both {@code min} + {@code max} → slider widget.</li>
 *   <li>Item entries → item icon + ResourceLocation EditBox (icon updates live while typing).</li>
 *   <li>List entries → element count label + <em>Edit List…</em> button.</li>
 *   <li>All text comes from {@link Component#translatable}.</li>
 *   <li>Values persist to {@code config/<modid>-pigeon.properties}.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class PigeConfigScreen extends Screen {

    // ── Layout constants ──────────────────────────────────────────────────────

    private static final int TABS_TOP     = 28;
    private static final int TAB_H        = 20;
    private static final int CRUMB_H      = 18;
    private static final int BACK_W       = 16;
    private static final int FOLDER_BTN_H = 26;
    private static final int ENTRY_H      = 38;
    private static final int ENTRY_W      = 200;
    private static final int SCROLLBAR    = 6;

    // derived in init()
    private int contentLeft, contentRight, contentTop, contentBottom;
    private int crumbTop;

    // ── Content model ─────────────────────────────────────────────────────────

    private sealed interface ContentItem {
        record FolderBtn(ConfigFolder folder, int naturalY)                                        implements ContentItem {}
        record Entry(ConfigEntry<?> entry, List<AbstractWidget> widgets, int naturalY)             implements ContentItem {}
        record ListElem(int index, AbstractWidget field, AbstractWidget deleteBtn, int naturalY)   implements ContentItem {}
        record ListAdd(AbstractWidget addBtn, int naturalY)                                        implements ContentItem {}
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Screen     parent;
    private final PigeConfig config;
    private final String     modid;

    private int          selectedTab  = 0;
    private int          scrollOffset = 0;
    private final List<String> navPath = new ArrayList<>();

    // List editing — non-null while the user edits a List<E> entry in-place
    private ConfigEntry<?> editingList = null;
    private final List<Object> tempList = new ArrayList<>();

    // ── Init-scoped lists (rebuilt every rebuildWidgets()) ────────────────────
    private record TabRgn(int x, int y, int w, int h, int idx) {}
    private final List<TabRgn>                tabRegions   = new ArrayList<>();
    private final List<ContentItem>           contentItems = new ArrayList<>();
    private final List<ContentItem.FolderBtn> folderBtns   = new ArrayList<>();
    private final List<ContentItem.Entry>     entryList    = new ArrayList<>();
    private int totalContentH = 0;

    // Deferred tooltip — captured during renderContent(), drawn after disableScissor()
    private List<Component> pendingTooltip = null;
    private int tooltipX, tooltipY;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PigeConfigScreen(Screen parent, PigeConfig config, String modid) {
        super(Component.translatable("config." + modid + ".title"));
        this.parent = parent;
        this.config = config;
        this.modid  = modid;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        contentItems.clear();
        folderBtns.clear();
        entryList.clear();
        tabRegions.clear();

        contentLeft   = 12;
        contentRight  = this.width - 12 - SCROLLBAR - 4;
        crumbTop      = TABS_TOP + TAB_H + 2;
        contentTop    = crumbTop + CRUMB_H + 2;
        contentBottom = this.height - 36;

        List<ConfigFolder> folders = folders();

        // Tab hit-regions (drawn manually)
        if (!folders.isEmpty()) {
            int tabW = Math.min(90, (this.width - 24) / folders.size());
            for (int i = 0; i < folders.size(); i++)
                tabRegions.add(new TabRgn(contentLeft + i * (tabW + 2), TABS_TOP, tabW, TAB_H, i));
        }

        // Back button — shown in sub-folders and in list edit mode
        if (!navPath.isEmpty() || editingList != null) {
            addRenderableWidget(new net.minecraft.client.gui.components.AbstractWidget(
                    contentLeft, crumbTop, BACK_W, CRUMB_H, Component.empty()) {
                @Override
                public void onClick(double x, double y) {
                    if (editingList != null) {
                        cancelListEdit();
                    } else {
                        persistCurrent();
                        navPath.remove(navPath.size() - 1);
                        scrollOffset = 0;
                        rebuildWidgets();
                    }
                }
                @Override
                public void renderWidget(GuiGraphics gg, int mx, int my, float dt) {
                    gg.blit(isHovered() ? PigeConfigTextures.ICON_BACK_ENABLED : PigeConfigTextures.ICON_BACK,
                            getX(), getY() + (CRUMB_H - 16) / 2, 0, 0, 16, 16, 16, 16);
                }
                @Override
                protected void updateWidgetNarration(
                        net.minecraft.client.gui.narration.NarrationElementOutput o) {}
            });
        }

        // Build flat content list for the current folder view
        buildContent(getCurrentFolderItems());

        // Clamp scroll
        int maxScroll = Math.max(0, totalContentH - (contentBottom - contentTop));
        scrollOffset  = Math.max(0, Math.min(scrollOffset, maxScroll));

        // Register entry field widgets (keyboard focus, not auto-rendered)
        for (ContentItem.Entry e : entryList)
            for (AbstractWidget w : e.widgets()) addWidget(w);

        // Register list element fields and action buttons
        for (ContentItem ci : contentItems) {
            if (ci instanceof ContentItem.ListElem le) {
                addWidget(le.field());
                addWidget(le.deleteBtn());
            } else if (ci instanceof ContentItem.ListAdd la) {
                addWidget(la.addBtn());
            }
        }

        // Save / Cancel — switch to Done / Cancel when editing a list
        int btnY  = this.height - 26;
        int btnCX = this.width / 2;
        if (editingList != null) {
            addRenderableWidget(new PigeButton(btnCX - 102, btnY, 100, 20,
                    Component.translatable("common.pigeon_core.done"),
                    PigeConfigTextures.BTN_WIDE, PigeConfigTextures.BTN_WIDE_ENABLED, this::doneListEdit));
            addRenderableWidget(new PigeButton(btnCX + 2, btnY, 100, 20,
                    Component.translatable("common.pigeon_core.cancel"),
                    PigeConfigTextures.BTN_WIDE, PigeConfigTextures.BTN_WIDE_ENABLED, this::cancelListEdit));
        } else {
            addRenderableWidget(new PigeButton(btnCX - 102, btnY, 100, 20,
                    Component.translatable("common.pigeon_core.save"),
                    PigeConfigTextures.BTN_WIDE, PigeConfigTextures.BTN_WIDE_ENABLED, this::save));
            addRenderableWidget(new PigeButton(btnCX + 2, btnY, 100, 20,
                    Component.translatable("common.pigeon_core.cancel"),
                    PigeConfigTextures.BTN_WIDE, PigeConfigTextures.BTN_WIDE_ENABLED, this::onClose));
        }
    }

    // ── Content builder ───────────────────────────────────────────────────────

    private void buildContent(List<FolderItem> items) {
        if (editingList != null) { buildListContent(); return; }

        int[] y   = {0};
        int   cx  = cx();

        for (FolderItem item : items) {
            if (item instanceof ConfigFolder sub) {
                var btn = new ContentItem.FolderBtn(sub, y[0]);
                contentItems.add(btn);
                folderBtns.add(btn);
                y[0] += FOLDER_BTN_H;

            } else if (item instanceof ConfigEntry<?> entry) {
                List<AbstractWidget> ws = buildWidgets(entry, cx, y[0]);
                var e = new ContentItem.Entry(entry, ws, y[0]);
                contentItems.add(e);
                entryList.add(e);
                y[0] += ENTRY_H;
            }
        }
        totalContentH = y[0];
    }

    private void buildListContent() {
        int y  = 0;
        int cx = cx();

        for (int i = 0; i < tempList.size(); i++) {
            final int idx = i;
            String strVal = elemToStr(tempList.get(i));
            int fw = PigeTextField.widthFor(font, strVal);
            ResourceLocation[] tx = PigeTextField.texForWidth(fw);

            int fieldY = contentTop + y + (ENTRY_H - FIELD_H) / 2 - scrollOffset;
            int btnY   = contentTop + y + (ENTRY_H - 18)     / 2 - scrollOffset;

            PigeTextField field = new PigeTextField(font, cx + ENTRY_W - fw, fieldY, fw, FIELD_H,
                    tx[0], tx[1], editingList.elementType(), null, null, strVal);
            field.setValue(strVal);
            field.setMaxLength(256);

            PigeButton deleteBtn = new PigeButton(
                    cx + ENTRY_W + 4, btnY, 18, 18,
                    Component.empty(),
                    PigeConfigTextures.BTN_DELETE, PigeConfigTextures.BTN_DELETE_ENABLED,
                    () -> { syncTempList(); if (idx < tempList.size()) tempList.remove(idx); rebuildWidgets(); });

            contentItems.add(new ContentItem.ListElem(i, field, deleteBtn, y));
            y += ENTRY_H;
        }

        // Add-element button
        int addBtnY = contentTop + y + (ENTRY_H - 18) / 2 - scrollOffset;
        PigeButton addBtn = new PigeButton(
                cx + ENTRY_W + 4, addBtnY, 18, 18,
                Component.empty(),
                PigeConfigTextures.BTN_ADD, PigeConfigTextures.BTN_ADD_ENABLED,
                () -> { syncTempList(); tempList.add(defaultElem()); rebuildWidgets(); });

        contentItems.add(new ContentItem.ListAdd(addBtn, y));
        totalContentH = y + ENTRY_H;
    }

    // Field widget height (matches FIELD_Xn texture height)
    private static final int FIELD_H = 16;

    private List<AbstractWidget> buildWidgets(ConfigEntry<?> entry, int cx, int natY) {
        List<AbstractWidget> ws = new ArrayList<>();
        int baseY = contentTop + natY - scrollOffset;
        int wy    = baseY + (ENTRY_H - FIELD_H) / 2; // centre 16 px field/button in 38 px row

        if (entry.type() == Boolean.class) {
            ws.add(new PigeBoolField(cx + ENTRY_W - PigeTextField.FIELD_W_S, wy,
                    PigeTextField.FIELD_W_S, FIELD_H, (Boolean) entry.value()));

        } else if (entry.type() == List.class) {
            // LIST_BTW is 80×16 — right-aligned, same tier as FIELD_W_L
            ws.add(new PigeButton(cx + ENTRY_W - 80, wy, 80, FIELD_H,
                    Component.translatable("config.pigeon_core.edit_list"),
                    PigeConfigTextures.LIST_BTW, PigeConfigTextures.LIST_BTN_ENABLED,
                    () -> openListEditor(entry)));

        } else if (entry.type() == Item.class) {
            Item cur = (Item) entry.value();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(cur);
            String initVal = rl != null ? rl.toString() : "minecraft:air";
            int fw = PigeTextField.widthFor(font, initVal);
            ResourceLocation[] tx = PigeTextField.texForWidth(fw);
            PigeTextField tf = new PigeTextField(font, cx + ENTRY_W - fw, wy, fw, FIELD_H,
                    tx[0], tx[1], Item.class, null, null, initVal);
            tf.setValue(initVal);
            tf.setMaxLength(256);
            ws.add(tf);

        } else {
            // Integer, Double, String — PigeTextField with auto-correct/clamp on defocus
            String initVal = String.valueOf(entry.value());
            int fw = PigeTextField.widthFor(font, initVal);
            ResourceLocation[] tx = PigeTextField.texForWidth(fw);
            PigeTextField tf = new PigeTextField(font, cx + ENTRY_W - fw, wy, fw, FIELD_H,
                    tx[0], tx[1], entry.type(), entry.min(), entry.max(), entry.value());
            tf.setValue(initVal);
            tf.setMaxLength(512);
            ws.add(tf);
        }
        return ws;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float dt) {
        pendingTooltip = null; // reset each frame
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        CosmicBackgroundRenderer.render(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        gg.drawCenteredString(font, title, this.width / 2, 10, 0xFFFFFF);

        renderTabs(gg, mouseX, mouseY);

        gg.fill(contentLeft, TABS_TOP + TAB_H + 1, this.width - contentLeft,
                TABS_TOP + TAB_H + 2, 0xFF445588);

        renderBreadcrumb(gg);

        gg.fill(contentLeft, contentTop - 1, contentRight + SCROLLBAR + 4, contentTop, 0xFF2A2A44);
        gg.fill(contentLeft, contentTop, contentRight + SCROLLBAR + 4, contentBottom, 0xAA0C0C1A);

        // Renders back button + Save/Cancel (addRenderableWidget)
        super.render(gg, mouseX, mouseY, dt);

        gg.enableScissor(contentLeft, contentTop, contentRight + 1, contentBottom);
        renderContent(gg, mouseX, mouseY, dt);
        gg.disableScissor();

        renderScrollbar(gg);

        // Tooltip AFTER scissor is disabled so it never gets clipped at the GUI edge
        if (pendingTooltip != null)
            gg.renderComponentTooltip(font, pendingTooltip, tooltipX, tooltipY);
    }

    /**
     * Namespace to use for a folder's translation key: built-in tabs
     * ({@code server}/{@code client}/{@code common}) are labeled from
     * pigeon_core's own lang file, everything else from the consuming mod's.
     */
    private String folderLangKey(String folderId) {
        String owner = PigeConfig.isBuiltInFolder(folderId) ? PigeonCore.MOD_ID : modid;
        return "config." + owner + ".folder." + folderId;
    }

    private void renderTabs(GuiGraphics gg, int mouseX, int mouseY) {
        List<ConfigFolder> fs = folders();
        for (TabRgn tab : tabRegions) {
            boolean sel = tab.idx() == selectedTab;
            boolean hov = !sel
                    && mouseX >= tab.x() && mouseX < tab.x() + tab.w()
                    && mouseY >= tab.y() && mouseY < tab.y() + tab.h();

            gg.fill(tab.x(), tab.y(), tab.x() + tab.w(), tab.y() + tab.h(),
                    sel ? 0xFF4466AA : hov ? 0xFF2D3A55 : 0xFF1A2133);
            if (sel)
                gg.fill(tab.x(), tab.y() + tab.h() - 2,
                        tab.x() + tab.w(), tab.y() + tab.h(), 0xFF88AAEE);
            gg.fill(tab.x(),               tab.y(), tab.x() + 1,           tab.y() + tab.h(), 0xFF445588);
            gg.fill(tab.x() + tab.w() - 1, tab.y(), tab.x() + tab.w(),    tab.y() + tab.h(), 0xFF445588);
            gg.fill(tab.x(),               tab.y(), tab.x() + tab.w(),     tab.y() + 1,       0xFF445588);

            if (tab.idx() < fs.size())
                gg.drawCenteredString(font,
                        Component.translatable(folderLangKey(fs.get(tab.idx()).id())),
                        tab.x() + tab.w() / 2, tab.y() + (tab.h() - 8) / 2, 0xFFFFFF);
        }
    }

    private void renderBreadcrumb(GuiGraphics gg) {
        List<ConfigFolder> fs = folders();
        if (fs.isEmpty() || selectedTab >= fs.size()) return;

        boolean atRoot = navPath.isEmpty() && editingList == null;
        int ty = crumbTop + (CRUMB_H - 8) / 2;
        int tx = contentLeft + (atRoot ? 0 : BACK_W + 4);

        // Tab label: light only when it is the final (active) segment
        boolean tabIsLast = navPath.isEmpty() && editingList == null;
        Component tabLabel = Component.translatable(folderLangKey(fs.get(selectedTab).id()));
        gg.drawString(font, tabLabel, tx, ty, tabIsLast ? 0xAABBDD : 0x7788AA, false);
        tx += font.width(tabLabel);

        // Sub-folder segments: only the last one is light (active)
        for (int i = 0; i < navPath.size(); i++) {
            Component sep = Component.literal(" › ");
            gg.drawString(font, sep, tx, ty, 0x445566, false);
            tx += font.width(sep);
            Component seg = Component.translatable(folderLangKey(navPath.get(i)));
            boolean segIsLast = (i == navPath.size() - 1) && editingList == null;
            gg.drawString(font, seg, tx, ty, segIsLast ? 0xBBCCEE : 0x7788AA, false);
            tx += font.width(seg);
        }

        // List name — always the last segment when in list-edit mode
        if (editingList != null) {
            Component sep = Component.literal(" › ");
            gg.drawString(font, sep, tx, ty, 0x445566, false);
            tx += font.width(sep);
            Component listLabel = Component.translatable("config." + modid + "." + editingList.id());
            gg.drawString(font, listLabel, tx, ty, 0xBBCCEE, false);
        }
    }

    private void renderContent(GuiGraphics gg, int mouseX, int mouseY, float dt) {
        int cx = cx();

        for (ContentItem ci : contentItems) {

            if (ci instanceof ContentItem.FolderBtn btn) {
                int ay = contentTop + btn.naturalY() - scrollOffset;
                if (ay + FOLDER_BTN_H < contentTop || ay > contentBottom) continue;

                int x0 = contentLeft + 4, x1 = contentRight - 4;
                boolean hov = mouseX >= x0 && mouseX < x1
                           && mouseY >= ay  && mouseY < ay + FOLDER_BTN_H;

                gg.fill(x0, ay, x1, ay + FOLDER_BTN_H, hov ? 0xFF263352 : 0xFF1A2240);
                gg.fill(x0, ay, x1, ay + 1, 0xFF2E3D5F);
                gg.fill(x0, ay + FOLDER_BTN_H - 1, x1, ay + FOLDER_BTN_H, 0xFF2E3D5F);

                gg.blit(PigeConfigTextures.ICON_FOLDER, cx, ay + (FOLDER_BTN_H - 16) / 2,
                        0, 0, 16, 16, 16, 16);

                gg.drawString(font,
                        Component.translatable(folderLangKey(btn.folder().id())),
                        cx + 20, ay + (FOLDER_BTN_H - 8) / 2, 0xDDDDEE, false);
                gg.drawString(font, "›", x1 - 9, ay + (FOLDER_BTN_H - 8) / 2, 0x778899, false);

            } else if (ci instanceof ContentItem.Entry e) {
                int ay = contentTop + e.naturalY() - scrollOffset;
                if (ay + ENTRY_H < contentTop || ay > contentBottom) continue;

                Class<?> t = e.entry().type();

                // Label — always single row, vertically centred in ENTRY_H
                String labelKey = "config." + modid + "." + e.entry().id();
                int labelY = ay + (ENTRY_H - 8) / 2;
                gg.drawString(font, Component.translatable(labelKey), cx, labelY, 0xCCCCCC, false);

                if (t == List.class) {
                    List<?> list = (List<?>) e.entry().value();
                    int labelW = font.width(Component.translatable(labelKey));
                    gg.drawString(font, " (" + list.size() + ")", cx + labelW, labelY, 0x777788, false);
                }

                // Item icon — live-resolved, spins on Y axis
                if (t == Item.class && !e.widgets().isEmpty()
                        && e.widgets().get(0) instanceof EditBox box) {
                    Item cur = liveItem(box.getValue().trim(), (Item) e.entry().value());
                    int iconX = box.getX() - 20;
                    int iconY = ay + (ENTRY_H - 16) / 2 - 1;
                    float angle = (System.currentTimeMillis() % 4000L) / 4000.0f * 360.0f;
                    // renderItem internally does translate(x+8, y+8, 150); rotation must happen
                    // at that same z=150 pivot or the item orbits instead of spinning in place.
                    // Pattern from PigeAutoScreen: pre-shift z by +150, rotate, undo, render at -8,-8.
                    gg.pose().pushPose();
                    gg.pose().translate(iconX + 8.0, iconY + 8.0, 0.0);
                    gg.pose().translate(0.0, 0.0, 150.0);
                    gg.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));
                    gg.pose().translate(0.0, 0.0, -150.0);
                    gg.renderItem(new ItemStack(cur), -8, -8);
                    gg.pose().popPose();
                }

                // Widget render (non-renderable children, drawn manually)
                for (AbstractWidget w : e.widgets()) w.render(gg, mouseX, mouseY, dt);

                // (?) hint — same row as the label
                int hintX = cx + ENTRY_W + 4;
                int hintY = labelY;
                gg.drawString(font, "(?)", hintX, hintY, 0x4488BB, false);

                if (mouseX >= hintX && mouseX <= hintX + 14
                 && mouseY >= hintY  && mouseY <= hintY + 9) {
                    pendingTooltip = buildTooltip(e.entry());
                    tooltipX = mouseX;
                    tooltipY = mouseY;
                }

            } else if (ci instanceof ContentItem.ListElem le) {
                int ay = contentTop + le.naturalY() - scrollOffset;
                if (ay + ENTRY_H < contentTop || ay > contentBottom) continue;

                int labelY = ay + (ENTRY_H - 8) / 2;
                gg.drawString(font, "#" + (le.index() + 1), cx, labelY, 0xCCCCCC, false);
                le.field().render(gg, mouseX, mouseY, dt);
                le.deleteBtn().render(gg, mouseX, mouseY, dt);

            } else if (ci instanceof ContentItem.ListAdd la) {
                int ay = contentTop + la.naturalY() - scrollOffset;
                if (ay + ENTRY_H < contentTop || ay > contentBottom) continue;
                la.addBtn().render(gg, mouseX, mouseY, dt);
            }
        }
    }

    /** Builds the multi-line tooltip for an entry: §8 description + optional min/max line. */
    private List<Component> buildTooltip(ConfigEntry<?> entry) {
        List<Component> lines = new ArrayList<>();

        lines.add(Component.translatable("config." + modid + "." + entry.id() + ".description")
                .withStyle(ChatFormatting.DARK_GRAY));

        boolean hasMin = entry.min() != null;
        boolean hasMax = entry.max() != null;
        if (hasMin || hasMax) {
            MutableComponent mmLine;
            if (hasMin && hasMax) {
                mmLine = Component.translatable("config.pigeon_core.min_max",
                        numLiteral(entry.min()), numLiteral(entry.max()));
            } else if (hasMin) {
                mmLine = Component.translatable("config.pigeon_core.min", numLiteral(entry.min()));
            } else {
                mmLine = Component.translatable("config.pigeon_core.max", numLiteral(entry.max()));
            }
            lines.add(mmLine);
        }

        return lines;
    }

    private static Component numLiteral(Object val) {
        return Component.literal(formatNum(val)).withStyle(ChatFormatting.BLUE);
    }

    private static String formatNum(Object val) {
        if (val instanceof Double d)
            return d == Math.floor(d) ? String.valueOf((long) d.doubleValue()) : String.format("%.2f", d);
        return String.valueOf(val);
    }

    private void renderScrollbar(GuiGraphics gg) {
        int ch = contentBottom - contentTop;
        if (totalContentH <= ch) return;
        int maxScroll = totalContentH - ch;
        int barH      = Math.max(16, ch * ch / totalContentH);
        int barY      = contentTop + (int) ((long) scrollOffset * (ch - barH) / maxScroll);
        int sbX       = contentRight + 4;
        gg.fill(sbX,     contentTop, sbX + SCROLLBAR, contentBottom,       0xFF0F0F1E);
        gg.fill(sbX + 1, barY,       sbX + SCROLLBAR - 1, barY + barH,     0xFF5577AA);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // Tab clicks
        for (TabRgn tab : tabRegions) {
            if (mx >= tab.x() && mx < tab.x() + tab.w()
             && my >= tab.y() && my < tab.y() + tab.h()) {
                if (tab.idx() != selectedTab) {
                    persistCurrent();
                    selectedTab  = tab.idx();
                    navPath.clear();
                    scrollOffset = 0;
                    rebuildWidgets();
                }
                return true;
            }
        }

        // Folder button clicks
        for (ContentItem.FolderBtn fBtn : folderBtns) {
            int ay = contentTop + fBtn.naturalY() - scrollOffset;
            if (ay < contentTop || ay >= contentBottom) continue;
            int x0 = contentLeft + 4, x1 = contentRight - 4;
            if (mx >= x0 && mx < x1 && my >= ay && my < ay + FOLDER_BTN_H) {
                persistCurrent();
                navPath.add(fBtn.folder().id());
                scrollOffset = 0;
                rebuildWidgets();
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (my < contentTop || my > contentBottom)
            return super.mouseScrolled(mx, my, delta);
        int maxScroll = Math.max(0, totalContentH - (contentBottom - contentTop));
        scrollOffset  = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 14));
        updateWidgetY();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            // Priority 1: defocus any active text field (entry or list element)
            for (ContentItem.Entry e : entryList) {
                for (AbstractWidget w : e.widgets()) {
                    if (w instanceof PigeTextField tf && tf.isFocused()) {
                        tf.setFocused(false);
                        return true;
                    }
                }
            }
            for (ContentItem ci : contentItems) {
                if (ci instanceof ContentItem.ListElem le
                        && le.field() instanceof PigeTextField tf && tf.isFocused()) {
                    tf.setFocused(false);
                    return true;
                }
            }
            // Priority 2: exit list edit mode (discard list changes)
            if (editingList != null) {
                cancelListEdit();
                return true;
            }
            // Priority 3: exit current sub-folder
            if (!navPath.isEmpty()) {
                persistCurrent();
                navPath.remove(navPath.size() - 1);
                scrollOffset = 0;
                rebuildWidgets();
                return true;
            }
            // Priority 4: fall through → super closes the screen
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private int cx() {
        return contentLeft + (contentRight - contentLeft - ENTRY_W) / 2;
    }

    private List<FolderItem> getCurrentFolderItems() {
        List<ConfigFolder> fs = folders();
        if (fs.isEmpty() || selectedTab >= fs.size()) return List.of();
        ConfigFolder cur = fs.get(selectedTab);
        for (String id : navPath) {
            ConfigFolder next = null;
            for (FolderItem fi : cur.items()) {
                if (fi instanceof ConfigFolder sf && sf.id().equals(id)) { next = sf; break; }
            }
            if (next == null) { navPath.clear(); return cur.items(); }
            cur = next;
        }
        return cur.items();
    }

    private void updateWidgetY() {
        for (ContentItem ci : contentItems) {
            if (ci instanceof ContentItem.Entry e) {
                int ay = contentTop + e.naturalY() - scrollOffset;
                int wy = ay + (ENTRY_H - FIELD_H) / 2;
                for (AbstractWidget w : e.widgets()) w.setY(wy);
            } else if (ci instanceof ContentItem.ListElem le) {
                int ay = contentTop + le.naturalY() - scrollOffset;
                le.field().setY(ay + (ENTRY_H - FIELD_H) / 2);
                le.deleteBtn().setY(ay + (ENTRY_H - 18) / 2);
            } else if (ci instanceof ContentItem.ListAdd la) {
                int ay = contentTop + la.naturalY() - scrollOffset;
                la.addBtn().setY(ay + (ENTRY_H - 18) / 2);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void persistCurrent() {
        for (ContentItem.Entry e : entryList) {
            ConfigEntry entry = e.entry();
            if (e.widgets().isEmpty()) continue;
            AbstractWidget w = e.widgets().get(0);

            if (entry.type() == Boolean.class && w instanceof PigeBoolField bf)
                entry.set(bf.getValue());
            else if (entry.type() == Item.class && w instanceof EditBox box)
                applyItemFromRL(entry, box.getValue().trim());
            else if (w instanceof EditBox box)
                parseAndSet(entry, box.getValue().trim());
            // Numeric fields: auto-corrected/clamped at defocus — value already valid here
        }
    }

    private static void applyItemFromRL(ConfigEntry<Item> entry, String raw) {
        try {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(raw));
            if (item != null) entry.set(item);
        } catch (Exception ignored) {}
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void parseAndSet(ConfigEntry entry, String raw) {
        try {
            if      (entry.type() == Integer.class) entry.set(Integer.valueOf(raw));
            else if (entry.type() == Double.class)  entry.set(Double.valueOf(raw));
            else if (entry.type() == String.class)  entry.set(raw);
        } catch (NumberFormatException ignored) {}
    }

    private static Item liveItem(String rlStr, Item fallback) {
        try {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(rlStr));
            return item != null ? item : fallback;
        } catch (Exception ignored) { return fallback; }
    }

    private void openListEditor(ConfigEntry<?> entry) {
        persistCurrent();
        editingList = entry;
        tempList.clear();
        @SuppressWarnings("unchecked")
        List<Object> existing = (List<Object>) entry.value();
        tempList.addAll(existing);
        scrollOffset = 0;
        rebuildWidgets();
    }

    @SuppressWarnings("unchecked")
    private void doneListEdit() {
        syncTempList();
        if (editingList != null)
            ((ConfigEntry<List<Object>>) editingList).setRaw(new ArrayList<>(tempList));
        editingList  = null;
        tempList.clear();
        scrollOffset = 0;
        rebuildWidgets();
    }

    private void cancelListEdit() {
        editingList  = null;
        tempList.clear();
        scrollOffset = 0;
        rebuildWidgets();
    }

    private void syncTempList() {
        for (ContentItem ci : contentItems) {
            if (ci instanceof ContentItem.ListElem le && le.field() instanceof EditBox box) {
                int idx = le.index();
                if (idx < tempList.size()) {
                    Object parsed = parseElem(box.getValue().trim());
                    if (parsed != null) tempList.set(idx, parsed);
                }
            }
        }
    }

    private String elemToStr(Object obj) {
        if (obj instanceof Item item) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            return rl != null ? rl.toString() : "minecraft:air";
        }
        return obj != null ? obj.toString() : "";
    }

    private Object defaultElem() {
        if (editingList == null) return "";
        Class<?> et = editingList.elementType();
        if (et == Integer.class) return 0;
        if (et == Double.class)  return 0.0;
        if (et == Boolean.class) return false;
        if (et == Item.class)    return net.minecraft.world.item.Items.AIR;
        return "";
    }

    private Object parseElem(String s) {
        if (editingList == null) return s;
        Class<?> et = editingList.elementType();
        try {
            if (et == Integer.class) return Integer.valueOf(s);
            if (et == Double.class)  return Double.valueOf(s);
            if (et == Boolean.class) return Boolean.valueOf(s);
            if (et == Item.class) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(s));
                return item != null ? item : net.minecraft.world.item.Items.AIR;
            }
            return s;
        } catch (NumberFormatException ignored) { return null; }
    }

    private void save() {
        persistCurrent();
        writeProperties();
        Minecraft.getInstance().setScreen(parent);
    }

    private List<ConfigFolder> folders() {
        return config.getContext().folders();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void writeProperties() {
        File dir = configDir();
        dir.mkdirs();
        Properties p = new Properties();
        for (ConfigEntry<?> e : config.getContext().allEntries())
            p.setProperty(e.id(), serializeEntry(e));
        try (FileWriter w = new FileWriter(new File(dir, modid + "-pigeon.properties"))) {
            p.store(w, modid + " pigeon config");
        } catch (Exception ex) {
            PigeonCore.LOGGER.error("[PigeConfig] save failed for '{}'", modid, ex);
        }
    }

    private static String serializeEntry(ConfigEntry<?> e) {
        if (e.type() == Item.class) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey((Item) e.value());
            return rl != null ? rl.toString() : "minecraft:air";
        }
        if (e.type() == List.class) {
            List<?> list = (List<?>) e.value();
            return list.stream().map(elem -> {
                if (elem instanceof Item item) {
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
                    return rl != null ? rl.toString() : "minecraft:air";
                }
                return String.valueOf(elem);
            }).collect(Collectors.joining("|"));
        }
        return String.valueOf(e.value());
    }

    /** Loads persisted values from disk. Called during {@code FMLClientSetupEvent}. */
    public static void load(PigeConfig config, String modid) {
        File file = new File(configDir(), modid + "-pigeon.properties");
        if (!file.exists()) return;
        Properties p = new Properties();
        try (FileReader r = new FileReader(file)) { p.load(r); }
        catch (Exception ex) { PigeonCore.LOGGER.warn("[PigeConfig] load failed for '{}'", modid, ex); return; }

        for (ConfigEntry<?> entry : config.getContext().allEntries()) {
            String raw = p.getProperty(entry.id());
            if (raw == null) continue;
            applyRaw(entry, raw);
        }
        PigeonCore.LOGGER.debug("[PigeConfig] loaded '{}' from {}", modid, file.getName());
    }

    @SuppressWarnings("rawtypes")
    private static void applyRaw(ConfigEntry entry, String raw) {
        try {
            if      (entry.type() == Boolean.class) entry.setRaw(Boolean.valueOf(raw));
            else if (entry.type() == Integer.class) entry.setRaw(Integer.valueOf(raw));
            else if (entry.type() == Double.class)  entry.setRaw(Double.valueOf(raw));
            else if (entry.type() == String.class)  entry.setRaw(raw);
            else if (entry.type() == Item.class) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(raw));
                if (item != null) entry.setRaw(item);
            } else if (entry.type() == List.class) {
                loadList(entry, raw);
            }
        } catch (NumberFormatException ignored) {}
    }

    @SuppressWarnings("rawtypes")
    private static void loadList(ConfigEntry entry, String raw) {
        Class<?> et = entry.elementType();
        if (et == null) return;
        if (raw.isEmpty()) { entry.setRaw(new ArrayList<>()); return; }
        List<Object> list = new ArrayList<>();
        for (String s : raw.split("\\|", -1)) {
            if (s.isEmpty()) continue;
            try {
                if      (et == Integer.class) list.add(Integer.valueOf(s));
                else if (et == Double.class)  list.add(Double.valueOf(s));
                else if (et == Boolean.class) list.add(Boolean.valueOf(s));
                else if (et == Item.class) {
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(s));
                    if (item != null) list.add(item);
                } else list.add(s);
            } catch (NumberFormatException ignored) {}
        }
        entry.setRaw(list);
    }

    private static File configDir() {
        return new File(Minecraft.getInstance().gameDirectory, "config");
    }

}
