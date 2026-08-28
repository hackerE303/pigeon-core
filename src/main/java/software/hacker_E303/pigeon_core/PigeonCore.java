package software.hacker_E303.pigeon_core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.hacker_E303.pigeon_core.common.PigeConfig;
import software.hacker_E303.pigeon_core.common.Settings;
import software.hacker_E303.pigeon_core.common.Tab;
import software.hacker_E303.pigeon_core.entity.common.stats.InitStats;

/**
 * Main mod entry point for the {@code pigeon_core} framework.
 * <p>
 * Initializes the mod and provides global access to registered resources
 * (items, entities, sounds, attributes, menus, configs) across all
 * framework-enabled mods.
 */
@Mod("pigeon_core")
public final class PigeonCore {

    public static final String MOD_ID = "pigeon_core";
    public static final Logger LOGGER = LogManager.getLogger(PigeonCore.class);

    private static boolean initialized = false;

    private static final Map<String, RegisterFactory> REGISTERIES = new HashMap<>();
    private static final Map<String, Settings> SETTINGS = new HashMap<>();

    protected static final Set<String> REGISTERED_PIGEIDS = new HashSet<>();

    /**
     * Constructs the mod instance and performs one-time initialization.
     */
    public PigeonCore() {

        if (!initialized) {
            PigeonCore.initialized = true;
            MinecraftForge.EVENT_BUS.register(this);

            IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
            PigeonCore.init(bus, null, "pigeon_core");
        }
    }

    /**
     * Initializes the framework for the given modid.
     *
     * @param bus     the mod event bus to register deferred registers and event handlers
     * @param settings optional mod settings; {@link Settings#DEFAULT} is used when null
     * @param modid   the mod id under which resources will be registered
     */
    public static void init(IEventBus bus, @Nullable Settings settings, String modid) {

        if (REGISTERIES.containsKey(modid)) return;
        RegisterFactory engine = RegisterFactory.create(modid);
        REGISTERIES.put(modid, engine);
        SETTINGS.put(modid, settings != null ? Settings.init(settings, modid) : Settings.DEFAULT);
        engine.init(bus);
    }

    /**
     * @param modid the mod id
     * @param key   the tab identifier
     * @return the registered {@link Tab}, or {@code null} if not found
     */
    @Nullable
    public static Tab getTab(String modid, String key) {
        RegisterFactory engine = REGISTERIES.get(modid);
        return engine != null ? engine.getTab(key) : null;
    }

    /**
     * @param modid the mod id
     * @param key   the item identifier
     * @return the registered {@link net.minecraft.world.item.Item}, or {@code null} if not found
     */
    @Nullable
    public static Item getItem(String modid, String key) {
        RegisterFactory engine = REGISTERIES.get(modid);
        return engine != null ? engine.getItem(key) : null;
    }

    /**
     * @param modid the mod id
     * @param key   the entity type identifier
     * @return the registered {@link EntityType}, or {@code null} if not found
     */
    @Nullable
    public static EntityType<?> getEntityType(String modid, String key) {
        RegisterFactory engine = REGISTERIES.get(modid);
        return engine != null ? engine.getEntityType(key) : null;
    }

    /**
     * @param modid the mod id
     * @param key   the entity stats identifier
     * @return the registered {@link InitStats}, or {@code null} if not found
     */
    @Nullable
    public static InitStats getEntityStats(String modid, String key) {
        RegisterFactory engine = REGISTERIES.get(modid);
        return engine != null ? engine.getEntityStats(key) : null;
    }

    /**
     * @param modid the mod id
     * @param key   the sound identifier
     * @return the registered {@link SoundEvent}, or {@code null} if not found
     */
    @Nullable
    public static SoundEvent getSound(String modid, String key) {
        RegisterFactory engine = REGISTERIES.get(modid);
        return engine != null ? engine.getSound(key) : null;
    }

    /**
     * @param modid the mod id
     * @param key   the attribute identifier
     * @return the registered {@link Attribute}, or {@code null} if not found
     */
    @Nullable
    public static Attribute getAttribute(String modid, String key) {
        RegisterFactory engine = REGISTERIES.get(modid);
        return engine != null ? engine.getAttribute(key) : null;
    }

    /**
     * @param modid the mod id
     * @param key   the menu type identifier
     * @return the registered {@link MenuType}, or {@code null} if not found
     */
    @Nullable
    public static MenuType<?> getMenuType(String modid, String key) {
        RegisterFactory engine = REGISTERIES.get(modid);
        return engine != null ? engine.getMenuType(key) : null;
    }

    /**
     * @param modid the mod id
     * @return the registered {@link PigeConfig}, or {@code null} if not found
     */
    @Nullable
    public static PigeConfig getConfig(String modid) {
        RegisterFactory engine = REGISTERIES.get(modid);
        return engine != null ? engine.getConfig(modid) : null;
    }

    /**
     * @param modid the mod id
     * @return the {@link Settings} for the mod, or {@link Settings#DEFAULT} if unregistered
     */
    @Nullable
    public static Settings settingsFrom(String modid) {
        Settings settings = SETTINGS.get(modid);

        if (settings == null) return Settings.DEFAULT;
        return settings;
    }

    /**
     * The modid of the (first) registered framework instance. Items/entities built
     * through {@link RegisterFactory} are registered under this id. It is used as a
     * fallback when {@code ForgeRegistries.ITEMS.getKey(item)} is not yet available
     * (e.g. while the item instance is still being constructed, before registration),
     * which would otherwise resolve to the {@code minecraft} namespace.
     */
    public static String getModid() {
        if (!REGISTERIES.isEmpty()) return REGISTERIES.keySet().iterator().next();
        return MOD_ID;
    }

    /**
     * @param pigeid the registered pigeid
     * @return {@code true} if the pigeid has been registered
     */
    public static boolean hasRegistered(String pigeid) {
        return REGISTERED_PIGEIDS.contains(pigeid);
    }

    /**
     * All modids that have initialized the framework. Used by the atlas sprite
     * injector to know which {@code textures/items/} folders to scan at stitch
     * time, so item sprites from external mods are registered into the
     * {@code minecraft:items} atlas without them shipping an atlas-info JSON.
     */
    public static Set<String> getRegisteredModids() {
        return REGISTERIES.keySet();
    }

}
