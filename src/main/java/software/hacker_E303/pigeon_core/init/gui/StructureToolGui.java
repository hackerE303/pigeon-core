package software.hacker_E303.pigeon_core.init.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import software.hacker_E303.pigeon_core.common.PigeGui;
import software.hacker_E303.pigeon_core.common.gui.GuiContext;
import software.hacker_E303.pigeon_core.common.gui.LayoutBounds;
import software.hacker_E303.pigeon_core.init.item.StructureTool;
import software.hacker_E303.pigeon_core.main.AutoRegister;
import software.hacker_E303.pigeon_core.util.BetterData;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;
import software.hacker_E303.pigeon_core.util.world.BuildUtils;
import software.hacker_E303.pigeon_core.util.world.PlayerUtils;
import software.hacker_E303.pigeon_core.util.world.StructureReplaceJob;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * GUI for the structure placement tool: lists the blocks found in the selected area
 * (with per-type remove / replace) and a picker to choose a replacement block from
 * the whole game registry (vanilla + mods).
 */
@AutoRegister("structure_tool_gui")
public class StructureToolGui extends PigeGui {

    private static final Path BUTTONS = Path.create("textures/guis/buttons/", ".png");

    private static final Location BACKGROUND = Location.create(Path.TEXTURE.GUI, "structure_tool_gui");
    private static final Location BUTTON = Location.create(BUTTONS, "button");

    private static final String MODE_KEY           = "structure_tool_mode";
    private static final String PAGE_KEY           = "structure_tool_page";
    private static final String SEARCH_KEY         = "structure_tool_search";
    private static final String REPLACE_SOURCE_KEY = "structure_tool_replace_source";

    private static final String MODE_LIST   = "list";
    private static final String MODE_PICKER = "picker";

    private static final int PAGE_SIZE   = 6;
    private static final int ROW_HEIGHT  = 14;
    private static final int MAX_SEARCH_LENGTH = 32;

    /**
     * Snapshot of a block-count scan for one tool instance, keyed by its persistent UUID.
     * Invalidated only on explicit events (corner change, area re-render with a stale
     * corner pair, or job completion) — never rebuilt every frame, since every render
     * call also decides how many/which buttons exist and their order must stay stable
     * between the frame a click was drawn on and the tick the server processes it.
     */
    private static final class CachedScan {
        final long corner1;
        final long corner2;
        final List<Map.Entry<Block, Integer>> sortedEntries;

        CachedScan(long corner1, long corner2, List<Map.Entry<Block, Integer>> sortedEntries) {
            this.corner1 = corner1;
            this.corner2 = corner2;
            this.sortedEntries = sortedEntries;
        }
    }

    private static final Map<UUID, CachedScan> SCAN_CACHE = new ConcurrentHashMap<>();
    private static volatile List<Block> ALL_BLOCKS;

    // ── renderInterface ───────────────────────────────────────────────────────

    @Override
    public void renderInterface(GuiContext ctx, Player player) {
        ctx.renderImage(BACKGROUND, LayoutBounds.create(0, 0), () -> true);

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof StructureTool)) return;

        BlockPos corner1 = StructureTool.getFirstCorner(stack);
        BlockPos corner2 = StructureTool.getSecondCorner(stack);
        // Never generate/persist a UUID here: only StructureTool's client-side corner
        // selection is allowed to create one (and sync it to the server). Doing it here
        // too would let the server mint its own, diverging from the client's and
        // silently breaking the UUID-keyed scan cache below.
        UUID existingUUID = StructureTool.getExistingUUID(stack);
        UUID toolUUID = existingUUID != null ? existingUUID : UUID.randomUUID();
        String mode = BetterData.getData(stack, MODE_KEY, MODE_LIST);

        if (MODE_PICKER.equals(mode)) {
            renderPickerPage(ctx, player, stack, toolUUID, corner1, corner2);
        } else {
            renderListPage(ctx, player, stack, toolUUID, corner1, corner2);
        }
    }

    // ── List page ─────────────────────────────────────────────────────────────

    private void renderListPage(GuiContext ctx, Player player, ItemStack stack, UUID toolUUID,
                                 @Nullable BlockPos corner1, @Nullable BlockPos corner2) {
        boolean hasArea = corner1 != null && corner2 != null;

        List<Map.Entry<Block, Integer>> entries = hasArea
                ? getOrRefreshScan(toolUUID, player.level(), corner1, corner2).sortedEntries
                : List.of();

        int maxPage = Math.max(0, (entries.size() - 1) / PAGE_SIZE);
        int page = Math.min(BetterData.getData(stack, PAGE_KEY, 0), maxPage);
        final int clampedPage = page;
        final BlockPos c1 = corner1;
        final BlockPos c2 = corner2;

        ctx.renderText("gui.pigeon_core.structure_tool_gui.title_list", LayoutBounds.create(8, 6), () -> true);
        ctx.renderText("gui.pigeon_core.structure_tool_gui.no_area", LayoutBounds.create(8, 20), () -> !hasArea);

        for (int i = 0; i < PAGE_SIZE; i++) {
            final int rowIndex = clampedPage * PAGE_SIZE + i;
            final int rowY = 20 + i * ROW_HEIGHT;
            BooleanSupplier rowVisible = () -> rowIndex < entries.size();

            ctx.renderItem(
                    rowIndex < entries.size() ? new ItemStack(entries.get(rowIndex).getKey()) : ItemStack.EMPTY,
                    LayoutBounds.create(11, rowY + 2, 0.8f),
                    action -> action.spin(45f), rowVisible);

            ctx.renderLiteralText(
                    rowIndex < entries.size() ? rowLabel(entries.get(rowIndex)) : "",
                    LayoutBounds.create(26, rowY + 3), rowVisible);

            ctx.renderButton("gui.pigeon_core.structure_tool_gui.remove", BUTTON,
                    LayoutBounds.create(150, rowY, 55, 13),
                    action -> {
                        if (rowIndex >= entries.size()) return;
                        Block target = entries.get(rowIndex).getKey();
                        if (action.isClientSide()) return;
                        if (player.level() instanceof ServerLevel serverLevel) {
                            invalidateScan(toolUUID);
                            StructureReplaceJob.enqueue(serverLevel, c1, c2, target, Blocks.AIR, player,
                                    () -> invalidateScan(toolUUID));
                        }
                    }, rowVisible);

            ctx.renderButton("gui.pigeon_core.structure_tool_gui.replace", BUTTON,
                    LayoutBounds.create(210, rowY, 65, 13),
                    action -> {
                        if (rowIndex >= entries.size()) return;
                        Block source = entries.get(rowIndex).getKey();
                        ItemStack live = player.getMainHandItem();
                        BetterData.setData(live, REPLACE_SOURCE_KEY, ForgeRegistries.BLOCKS.getKey(source).toString());
                        BetterData.setData(live, MODE_KEY, MODE_PICKER);
                        BetterData.setData(live, SEARCH_KEY, "");
                        BetterData.setData(live, PAGE_KEY, 0);
                    }, rowVisible);
        }

        ctx.renderButton("gui.pigeon_core.structure_tool_gui.prev_page", BUTTON,
                LayoutBounds.create(8, 108, 50, 13),
                action -> BetterData.setData(player.getMainHandItem(), PAGE_KEY, Math.max(0, clampedPage - 1)),
                () -> clampedPage > 0);

        ctx.renderButton("gui.pigeon_core.structure_tool_gui.next_page", BUTTON,
                LayoutBounds.create(262, 108, 50, 13),
                action -> BetterData.setData(player.getMainHandItem(), PAGE_KEY, Math.min(maxPage, clampedPage + 1)),
                () -> clampedPage < maxPage);

        ctx.renderButton("common.pigeon_core.save", BUTTON, LayoutBounds.create(220, 180, 100, 20), action -> {
            if (action.isClientSide()) return;
            PlayerUtils.with("pigeon_core", player, context -> context.debugMessage("structure_tool.save"));
            BuildUtils.saveStructure(player.level(), "structure", c1, c2);
            action.closeInterface();
        }, () -> hasArea);
    }

    // ── Picker page ───────────────────────────────────────────────────────────

    private void renderPickerPage(GuiContext ctx, Player player, ItemStack stack, UUID toolUUID,
                                   @Nullable BlockPos corner1, @Nullable BlockPos corner2) {
        String search = BetterData.getData(stack, SEARCH_KEY, "");
        String sourceId = BetterData.getData(stack, REPLACE_SOURCE_KEY, "");

        List<Block> filtered = allBlocksSorted().stream()
                .filter(b -> matchesSearch(b, search))
                .toList();

        int maxPage = Math.max(0, (filtered.size() - 1) / PAGE_SIZE);
        int page = Math.min(BetterData.getData(stack, PAGE_KEY, 0), maxPage);
        final int clampedPage = page;
        final BlockPos c1 = corner1;
        final BlockPos c2 = corner2;

        ctx.renderText("gui.pigeon_core.structure_tool_gui.title_picker", LayoutBounds.create(8, 6), () -> true);

        String placeholder = Component.translatable("gui.pigeon_core.structure_tool_gui.search_placeholder").getString();
        String searchDisplay = search.isEmpty() ? "§8" + placeholder : "§7" + search;
        ctx.renderLiteralText(searchDisplay, LayoutBounds.create(10, 20), () -> true);

        for (int i = 0; i < PAGE_SIZE; i++) {
            final int rowIndex = clampedPage * PAGE_SIZE + i;
            final int rowY = 36 + i * ROW_HEIGHT;
            BooleanSupplier rowVisible = () -> rowIndex < filtered.size();

            ctx.renderItem(
                    rowIndex < filtered.size() ? new ItemStack(filtered.get(rowIndex)) : ItemStack.EMPTY,
                    LayoutBounds.create(11, rowY + 2, 0.8f),
                    action -> action.spin(45f), rowVisible);

            ctx.renderLiteralText(
                    rowIndex < filtered.size() ? truncate(blockDisplayName(filtered.get(rowIndex)), 24) : "",
                    LayoutBounds.create(26, rowY + 3), rowVisible);

            ctx.renderButton("gui.pigeon_core.structure_tool_gui.select", BUTTON,
                    LayoutBounds.create(255, rowY, 55, 13),
                    action -> {
                        if (rowIndex >= filtered.size()) return;
                        Block target = filtered.get(rowIndex);

                        if (!action.isClientSide()) {
                            Block source = resolveBlock(sourceId);
                            if (source == null) {
                                PlayerUtils.with("pigeon_core", player, c ->
                                        c.debugMessage("structure_tool.replace_missing_source"));
                            } else if (c1 != null && c2 != null && player.level() instanceof ServerLevel serverLevel) {
                                invalidateScan(toolUUID);
                                StructureReplaceJob.enqueue(serverLevel, c1, c2, source, target, player,
                                        () -> invalidateScan(toolUUID));
                            }
                        }

                        ItemStack live = player.getMainHandItem();
                        BetterData.setData(live, MODE_KEY, MODE_LIST);
                        BetterData.setData(live, REPLACE_SOURCE_KEY, "");
                        BetterData.setData(live, PAGE_KEY, 0);
                    }, rowVisible);
        }

        ctx.renderButton("gui.pigeon_core.structure_tool_gui.prev_page", BUTTON,
                LayoutBounds.create(8, 124, 50, 13),
                action -> BetterData.setData(player.getMainHandItem(), PAGE_KEY, Math.max(0, clampedPage - 1)),
                () -> clampedPage > 0);

        ctx.renderButton("gui.pigeon_core.structure_tool_gui.next_page", BUTTON,
                LayoutBounds.create(262, 124, 50, 13),
                action -> BetterData.setData(player.getMainHandItem(), PAGE_KEY, Math.min(maxPage, clampedPage + 1)),
                () -> clampedPage < maxPage);

        ctx.renderButton("common.pigeon_core.cancel", BUTTON, LayoutBounds.create(120, 124, 60, 13), action -> {
            ItemStack live = player.getMainHandItem();
            BetterData.setData(live, MODE_KEY, MODE_LIST);
            BetterData.setData(live, REPLACE_SOURCE_KEY, "");
            BetterData.setData(live, PAGE_KEY, 0);
        }, () -> true);
    }

    // ── Search input (client types, server owns the persisted text) ─────────────

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean onCharTyped(char c) {
        return !isPickerModeClient();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isPickerModeClient()) return true;
        return keyCode != 259; // GLFW_KEY_BACKSPACE
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean isPickerModeClient() {
        net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return false;
        return MODE_PICKER.equals(BetterData.getData(player.getMainHandItem(), MODE_KEY, MODE_LIST));
    }

    @Override
    public void handleCharInput(char c, int keyCode, @Nullable Entity sender, Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!MODE_PICKER.equals(BetterData.getData(stack, MODE_KEY, MODE_LIST))) return;

        String search = BetterData.getData(stack, SEARCH_KEY, "");
        if (c != 0) {
            if (search.length() < MAX_SEARCH_LENGTH) search = search + c;
        } else if (keyCode == 259) { // Backspace
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
        }
        BetterData.setData(stack, SEARCH_KEY, search);
        BetterData.setData(stack, PAGE_KEY, 0);
    }

    // ── Scan cache ────────────────────────────────────────────────────────────

    private static CachedScan getOrRefreshScan(UUID toolUUID, net.minecraft.world.level.Level level,
                                                BlockPos corner1, BlockPos corner2) {
        long l1 = corner1.asLong();
        long l2 = corner2.asLong();
        CachedScan cached = SCAN_CACHE.get(toolUUID);
        if (cached != null && cached.corner1 == l1 && cached.corner2 == l2) return cached;

        Map<Block, Integer> counts = BuildUtils.countBlocks(level, corner1, corner2);
        List<Map.Entry<Block, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort(Comparator.comparing(e -> ForgeRegistries.BLOCKS.getKey(e.getKey()).toString()));

        CachedScan fresh = new CachedScan(l1, l2, sorted);
        SCAN_CACHE.put(toolUUID, fresh);
        return fresh;
    }

    private static void invalidateScan(UUID toolUUID) {
        SCAN_CACHE.remove(toolUUID);
    }

    // ── All-blocks registry (for the picker) ─────────────────────────────────

    private static List<Block> allBlocksSorted() {
        List<Block> local = ALL_BLOCKS;
        if (local == null) {
            synchronized (StructureToolGui.class) {
                local = ALL_BLOCKS;
                if (local == null) {
                    local = ForgeRegistries.BLOCKS.getValues().stream()
                            .filter(b -> b != Blocks.AIR)
                            .sorted(Comparator.comparing(b -> ForgeRegistries.BLOCKS.getKey(b).toString()))
                            .toList();
                    ALL_BLOCKS = local;
                }
            }
        }
        return local;
    }

    private static boolean matchesSearch(Block block, String search) {
        if (search.isEmpty()) return true;
        String id = ForgeRegistries.BLOCKS.getKey(block).toString();
        return id.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    @Nullable
    private static Block resolveBlock(String registryId) {
        if (registryId == null || registryId.isEmpty()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(registryId);
        if (rl == null) return null;
        Block block = ForgeRegistries.BLOCKS.getValue(rl);
        return (block == null || block == Blocks.AIR) ? null : block;
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    private static String rowLabel(Map.Entry<Block, Integer> entry) {
        return truncate(blockDisplayName(entry.getKey()), 20) + " x" + entry.getValue();
    }

    private static String blockDisplayName(Block block) {
        return Component.translatable(block.getDescriptionId()).getString();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Background getBackground(LayoutBounds bounds) {
        return Background.create(LayoutBounds.create(-1, 10, 320, 200), false);
    }
}
