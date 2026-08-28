package software.hacker_E303.pigeon_core.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import software.hacker_E303.pigeon_core.common.config.ConfigContext;
import software.hacker_E303.pigeon_core.common.config.ConfigEntry;
import software.hacker_E303.pigeon_core.common.config.ConfigFolder;
import software.hacker_E303.pigeon_core.common.config.FolderItem;
import software.hacker_E303.pigeon_core.util.BetterTexts;

/**
 * Base class for mod configuration.
 *
 * <p>Annotate a subclass with {@link software.hacker_E303.pigeon_core.main.AutoRegister}
 * so the framework discovers it, calls {@link #build} once at startup, and registers
 * the Forge "Config" button automatically.</p>
 *
 * <pre>{@code
 * @AutoRegister("my_config")
 * public class MyConfig extends PigeConfig {
 *     @Override
 *     public void build(ConfigContext ctx) {
 *         ctx.server(f -> {
 *             f.add("max_count", Integer.class, 10, 1, 100);
 *             f.folder("advanced", sub -> {
 *                 sub.add("scale", Double.class, 1.0, 0.1, 5.0);
 *             });
 *         });
 *         ctx.client(f -> { f.add("show_hud", Boolean.class, true, null, null); });
 *     }
 * }
 * }</pre>
 *
 * Values are read via {@link #get(String, Class)} or {@link #getValues()}.
 * Missing lang keys (labels + description slots) are auto-seeded in dev mode.
 */
public abstract class PigeConfig {

    // ── Instance ──────────────────────────────────────────────────────────────

    private ConfigContext context;

    /**
     * Declare all config entries here via the provided {@link ConfigContext}.
     * Called once by the framework — do not invoke manually.
     */
    public abstract void build(ConfigContext ctx);

    /** Called by {@link software.hacker_E303.pigeon_core.RegisterFactory}. */
    public final void init() {
        context = new ConfigContext();
        build(context);
    }

    /** Flat {@code id → currentValue} snapshot across every folder and sub-folder. */
    public Map<String, Object> getValues() {
        return context.toMap();
    }

    /**
     * Returns the current value for {@code id} cast to {@code type},
     * or {@code null} if the id is not found or the type does not match.
     */
    @Nullable
    public <T> T get(String id, Class<T> type) {
        for (ConfigEntry<?> e : context.allEntries()) {
            if (e.id().equals(id) && type.isInstance(e.value()))
                return type.cast(e.value());
        }
        return null;
    }

    public ConfigContext getContext() { return context; }

    // ── Lang key generation ───────────────────────────────────────────────────

    /**
     * Collects every translation key this config needs together with a sensible
     * English default. Used by the framework to seed {@code en_us.json}.
     *
     * <p>Only <em>mod-specific</em> keys are generated here. Framework-level
     * strings (the {@code Save}/{@code Done}/{@code Cancel} buttons, the
     * {@code Min}/{@code Max} tooltips, and the built-in {@code Server}/{@code Client}/{@code Common}
     * folder labels) are shipped from {@code pigeon_core}'s own {@code en_us.json}
     * and are <strong>not</strong> duplicated into the consuming mod's file.</p>
     *
     * Keys generated:
     * <ul>
     *   <li>{@code config.<modid>.title}              → title of the config screen</li>
     *   <li>{@code config.<modid>.folder.<id>}        → folder button name (custom folders only;
     *       built-in {@code server}/{@code client}/{@code common} tabs are skipped —
     *       their labels come from {@code config.pigeon_core.folder.*})</li>
     *   <li>{@code config.<modid>.<id>}               → entry label (auto-derived)</li>
     *   <li>{@code config.<modid>.<id>.description}   → tooltip description (empty — fill in your lang file)</li>
     * </ul>
     */
    public Map<String, String> generateLangKeys(String modid) {
        Map<String, String> keys = new LinkedHashMap<>();
        keys.put("config." + modid + ".title", BetterTexts.titleCase(modid) + " Configuration");
        for (ConfigFolder tab : context.folders()) {
            // Built-in folders (server/client/common) use framework-provided labels
            // from pigeon_core's lang file (config.pigeon_core.folder.<id>), so only
            // the folder-label key is skipped — entry keys inside are still generated.
            if (!isBuiltInFolder(tab.id()))
                keys.put("config." + modid + ".folder." + tab.id(), tab.defaultLabel());
            collectLangKeys(tab.items(), modid, keys);
        }
        return keys;
    }

    /**
     * Names of the built-in config tabs created by {@link ConfigContext#server},
     * {@link ConfigContext#client}, and {@link ConfigContext#common}. Their display
     * labels are shipped from {@code pigeon_core} so they don't need to be
     * regenerated into every consuming mod's lang file.
     */
    public static boolean isBuiltInFolder(String id) {
        return "server".equals(id) || "client".equals(id) || "common".equals(id);
    }

    private static void collectLangKeys(List<FolderItem> items, String modid, Map<String, String> keys) {
        for (FolderItem item : items) {
            if (item instanceof ConfigEntry<?> e) {
                keys.put("config." + modid + "." + e.id(),                    e.defaultLabel());
                keys.put("config." + modid + "." + e.id() + ".description",   "");
            } else if (item instanceof ConfigFolder f) {
                // Skip built-in folder labels (server/client/common) — they come from pigeon_core.
                if (!isBuiltInFolder(f.id()))
                    keys.put("config." + modid + ".folder." + f.id(), f.defaultLabel());
                collectLangKeys(f.items(), modid, keys);
            }
        }
    }
}