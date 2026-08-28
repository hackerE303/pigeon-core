package software.hacker_E303.pigeon_core.common.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

/**
 * Builder context passed to the lambda inside
 * {@link ConfigContext#server}, {@link ConfigContext#client},
 * {@link ConfigContext#common}, or {@link ConfigContext#folder}.
 *
 * <p>Items (entries and sub-folders) are kept in declaration order.
 * Nested {@code folder()} calls produce a <strong>button</strong> in the GUI —
 * clicking it navigates into that folder (file-manager style), rather than showing
 * an inline section divider.</p>
 *
 * <h3>Entry API</h3>
 * <pre>{@code
 * f.add("show_hud",    Boolean.class, true,  null, null);  // checkbox
 * f.add("max_turrets", Integer.class, 5,     1,   20);     // slider (both bounds present)
 * f.add("damage_mult", Double.class,  1.0,  0.1, 10.0);   // slider
 * f.add("server_tag",  String.class, "Tag", null, null);   // EditBox
 * f.add("fuel_item",   Item.class, Items.COAL, null, null);// icon + RL EditBox
 * f.addList("fuel_items", Item.class, List.of(Items.COAL));// list editor
 * }</pre>
 *
 * <p>The description tooltip is seeded automatically as
 * {@code config.<modid>.<id>.description} in {@code en_us.json}.</p>
 *
 * <h3>Nested folders</h3>
 * <pre>{@code
 * f.folder("combat", sub -> {
 *     sub.add("armor_pen", Integer.class, 0, 0, 100);
 *     sub.folder("explosions", exp -> {
 *         exp.add("blast_radius", Double.class, 4.0, 0.5, 20.0);
 *     });
 * });
 * }</pre>
 */
public final class FolderContext {

    final List<FolderItem> items = new ArrayList<>();

    // ── Entry API ─────────────────────────────────────────────────────────────

    /**
     * Adds a typed config entry.
     *
     * @param id           programmatic identifier; also used to derive the display label
     *                     ({@code "max_turrets"} → {@code "Max Turrets"}) and to seed the
     *                     description tooltip key {@code config.<modid>.<id>.description}
     * @param type         value class — {@code Boolean.class}, {@code Integer.class},
     *                     {@code Double.class}, {@code String.class}, or
     *                     {@code net.minecraft.world.item.Item.class}
     * @param defaultValue the initial / reset value
     * @param min          lower bound for numeric types — activates a slider when both
     *                     {@code min} and {@code max} are non-null; {@code null} to use an EditBox
     * @param max          upper bound for numeric types — activates a slider when both
     *                     {@code min} and {@code max} are non-null; {@code null} to use an EditBox
     */
    public <T> ConfigEntry<T> add(String id,
                                   Class<T> type, T defaultValue,
                                   @Nullable T min, @Nullable T max) {
        ConfigEntry<T> e = new ConfigEntry<>(id, type, null, defaultValue, min, max);
        items.add(e);
        return e;
    }

    /**
     * Adds a list config entry.
     * The GUI shows the element count and an <em>Edit List…</em> button that opens
     * a dedicated list-editor screen.
     *
     * @param id           programmatic identifier
     * @param elementType  class of each element in the list
     *                     ({@code String.class}, {@code Integer.class},
     *                     {@code Double.class}, or {@code Item.class})
     * @param defaultValue initial list content — use {@code List.of(…)} or {@code null} for empty
     */
    @SuppressWarnings("unchecked")
    public <E> ConfigEntry<List<E>> addList(String id,
                                             Class<E> elementType,
                                             @Nullable List<E> defaultValue) {
        List<E> dv = defaultValue != null ? new ArrayList<>(defaultValue) : new ArrayList<>();
        ConfigEntry<List<E>> e =
                (ConfigEntry<List<E>>) (Object) new ConfigEntry<>(id, List.class, elementType, dv, null, null);
        items.add(e);
        return e;
    }

    // ── Sub-folders ───────────────────────────────────────────────────────────

    /**
     * Creates a named sub-folder at the current nesting level.
     * In the GUI this appears as a clickable folder button — pressing it navigates
     * into the folder (file-manager style) and a breadcrumb allows returning.
     * Empty sub-folders are silently dropped.
     */
    public FolderContext folder(String id, Consumer<FolderContext> builder) {
        FolderContext sub = new FolderContext();
        builder.accept(sub);
        if (!sub.items.isEmpty())
            items.add(new ConfigFolder(id, new ArrayList<>(sub.items)));
        return this;
    }
}
