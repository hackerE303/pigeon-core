package software.hacker_E303.pigeon_core.common.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A named group of {@link FolderItem}s — either {@link ConfigEntry} leaves or nested
 * {@link ConfigFolder} sub-groups — in declaration order.
 *
 * <p>Implements {@link FolderItem} so it can itself appear inside another folder,
 * enabling unlimited nesting depth.</p>
 */
public final class ConfigFolder implements FolderItem {

    private final String           id;
    private final List<FolderItem> items;   // entries + sub-folders in declaration order

    ConfigFolder(String id, List<FolderItem> items) {
        this.id    = id;
        this.items = List.copyOf(items);
    }

    public String           id()    { return id; }
    public List<FolderItem> items() { return items; }

    /**
     * Human-readable English label auto-derived from the id, used as the
     * default value when seeding the lang file.
     * {@code "my_server"} → {@code "My Server"}.
     */
    public String defaultLabel() {
        return Arrays.stream(id.split("_"))
                .filter(w -> !w.isEmpty())
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /** All {@link ConfigEntry} leaves reachable from this folder (recursive, depth-first). */
    public List<ConfigEntry<?>> allEntries() {
        List<ConfigEntry<?>> out = new ArrayList<>();
        collectEntries(items, out);
        return out;
    }

    static void collectEntries(List<FolderItem> items, List<ConfigEntry<?>> out) {
        for (FolderItem item : items) {
            if (item instanceof ConfigEntry<?> e) out.add(e);
            else if (item instanceof ConfigFolder f) collectEntries(f.items, out);
        }
    }
}
