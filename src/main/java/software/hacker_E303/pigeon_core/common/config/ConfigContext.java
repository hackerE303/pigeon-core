package software.hacker_E303.pigeon_core.common.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Top-level context received by {@link software.hacker_E303.pigeon_core.common.PigeConfig#build}.
 *
 * <h3>Built-in tabs</h3>
 * <pre>{@code
 * ctx.server(f -> { f.add("max_turrets", Integer.class, 5, 1, 20); });
 * ctx.client(f -> { f.add("show_hud", Boolean.class, true, null, null); });
 * ctx.common(f -> { f.add("server_tag", String.class, "default", null, null); });
 * }</pre>
 *
 * <h3>Custom tabs</h3>
 * <pre>{@code
 * ctx.folder("weapons", f -> {
 *     f.add("gun_damage", Double.class, 1.0, 0.1, 10.0);
 *     f.folder("special", sub -> {
 *         sub.add("rockets_enabled", Boolean.class, false, null, null);
 *     });
 * });
 * }</pre>
 *
 * Each top-level folder becomes a <strong>tab</strong> in the config screen.
 * Nested {@code folder()} calls appear as folder buttons within that tab.
 * Empty folders (no entries or sub-folders) are silently dropped.
 */
public final class ConfigContext {

    private final List<ConfigFolder> tabs = new ArrayList<>();

    // ── Built-in convenience methods ──────────────────────────────────────────

    /** Adds (or merges into) the built-in <em>Server</em> tab. */
    public ConfigContext server(Consumer<FolderContext> builder) {
        return folder("server", builder);
    }

    /** Adds (or merges into) the built-in <em>Client</em> tab. */
    public ConfigContext client(Consumer<FolderContext> builder) {
        return folder("client", builder);
    }

    /** Adds (or merges into) the built-in <em>Common</em> tab. */
    public ConfigContext common(Consumer<FolderContext> builder) {
        return folder("common", builder);
    }

    // ── Custom folders ────────────────────────────────────────────────────────

    /**
     * Adds a top-level tab with a custom id.
     * Can also be called with an existing id to append more items to that tab.
     */
    public ConfigContext folder(String id, Consumer<FolderContext> builder) {
        FolderContext f = new FolderContext();
        builder.accept(f);
        if (!f.items.isEmpty()) {
            for (ConfigFolder existing : tabs) {
                if (existing.id().equals(id)) {
                    List<FolderItem> merged = new ArrayList<>(existing.items());
                    merged.addAll(f.items);
                    tabs.set(tabs.indexOf(existing), new ConfigFolder(id, merged));
                    return this;
                }
            }
            tabs.add(new ConfigFolder(id, new ArrayList<>(f.items)));
        }
        return this;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Top-level folders; each becomes a tab in the config screen. */
    public List<ConfigFolder> folders() {
        return Collections.unmodifiableList(tabs);
    }

    /** All {@link ConfigEntry} leaves across every tab, depth-first. */
    public List<ConfigEntry<?>> allEntries() {
        List<ConfigEntry<?>> all = new ArrayList<>();
        for (ConfigFolder tab : tabs) all.addAll(tab.allEntries());
        return all;
    }

    /** Flat {@code id → currentValue} snapshot. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        for (ConfigEntry<?> e : allEntries()) map.put(e.id(), e.value());
        return map;
    }
}
