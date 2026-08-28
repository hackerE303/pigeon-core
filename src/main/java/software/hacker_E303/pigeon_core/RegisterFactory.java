package software.hacker_E303.pigeon_core;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.inventory.MenuType;
import java.util.function.Consumer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.resource.PathPackResources;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.forgespi.language.ModFileScanData.ClassData;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import software.hacker_E303.pigeon_core.client.entity.renderer.BulletRenderer;
import software.hacker_E303.pigeon_core.client.entity.renderer.AnimatableEMobRenderer;
import software.hacker_E303.pigeon_core.client.entity.renderer.GeoEMobRenderer;
import software.hacker_E303.pigeon_core.geo.entity.GeoEMob;
import software.hacker_E303.pigeon_core.client.gui.PigeAutoScreen;
import software.hacker_E303.pigeon_core.client.gui.PigeConfigScreen;
import software.hacker_E303.pigeon_core.common.Generic;
import software.hacker_E303.pigeon_core.common.PigeConfig;
import software.hacker_E303.pigeon_core.common.PigeGui;
import software.hacker_E303.pigeon_core.common.Tab;
import software.hacker_E303.pigeon_core.common.gui.PigeAutoContainer;
import software.hacker_E303.pigeon_core.entity.EBullet;
import software.hacker_E303.pigeon_core.entity.animation.AnimatableEMob;
import software.hacker_E303.pigeon_core.entity.common.spawn.SpawnPlace;
import software.hacker_E303.pigeon_core.entity.common.spawn.SpawnDefinition;
import software.hacker_E303.pigeon_core.entity.common.stats.LivingStats;
import software.hacker_E303.pigeon_core.entity.common.stats.InitStats;
import software.hacker_E303.pigeon_core.main.AutoRegister;
import software.hacker_E303.pigeon_core.main.EResources;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Central factory responsible for scanning, registering, and wiring together
 * all framework-provided resources (items, entities, sounds, attributes,
 * GUIs, configs, spawn rules) for a single modid.
 */
@SuppressWarnings({"unchecked", "UseSpecificCatch"})
public final class RegisterFactory {

    private final String MOD_ID;

    private final DeferredRegister<Item> ITEMS;
    private final DeferredRegister<SoundEvent> SOUNDS;
    private final DeferredRegister<Attribute> ATTRIBUTES;
    private final DeferredRegister<EntityType<?>> ENTITY_TYPES;
    private final DeferredRegister<CreativeModeTab> CREATIVE_TABS;

    private final Map<String, Tab> TABS = new HashMap<>();

    private final Map<String, InitStats> ENTITY_STATS = new HashMap<>();
    private final Map<String, InitStats> ENTITY_STATS_BY_ID = new HashMap<>();
    private final Map<String, Class<? extends Entity>> ENTITY_CLASSES = new HashMap<>();

    private final Map<ModelLayerLocation, Supplier<LayerDefinition>> PENDING_LAYERS = new HashMap<>();
    private final Map<String, Supplier<AttributeSupplier.Builder>> PENDING_ATTRIBUTES = new HashMap<>();

    private final Map<String, RegistryObject<Item>> REGISTERED_ITEMS = new HashMap<>();
    private final Map<String, RegistryObject<SoundEvent>> REGISTERED_SOUNDS = new HashMap<>();
    private final Map<String, RegistryObject<Attribute>> REGISTERED_ATTRIBUTES = new HashMap<>();
    private final Map<String, RegistryObject<EntityType<? extends Entity>>> REGISTERED_ENTITIES = new HashMap<>();

    // Spawn registrations
    private final Map<String, SpawnPlace.SpawnContext> ENTITY_SPAWNS = new HashMap<>();

    // Placement info (SpawnPlacements.Type / heightmap / predicate) per entity id,
    // kept alive after common setup so it can be applied via SpawnPlacementRegisterEvent.
    private final Map<String, SpawnPlace.SpawnBuilder> SPAWN_PLACEMENTS = new HashMap<>();

    // GUI / MenuType registrations
    private final DeferredRegister<MenuType<?>> MENUS;
    private final Map<String, RegistryObject<MenuType<?>>> REGISTERED_MENUS = new HashMap<>();
    private final Map<String, PigeGui> GUI_INSTANCES = new HashMap<>();
    private final Map<String, Class<?>> PENDING_SCREENS = new HashMap<>();

    // Config registration
    private PigeConfig registeredConfig = null;
    private final Map<String, PigeConfig> CONFIGS = new HashMap<>();

    private final List<Class<?>> CLASSES_TO_INIT = new ArrayList<>();

    private RegisterFactory(String modid) {
        this.MOD_ID = modid;

        this.ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, modid);
        this.ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, modid);
        this.CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, modid);
        this.SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, modid);
        this.ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, modid);
        this.MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, modid);
    }

    /**
     * Creates a new register factory for the given modid.
     *
     * @param modid the mod id
     * @return a new {@link RegisterFactory} instance
     */
    protected static RegisterFactory create(String modid) {
        return new RegisterFactory(modid);
    }

    /**
     * Initializes deferred registers, scans annotated classes, and registers
     * resources, entities, items, GUIs, configs, and spawn rules.
     *
     * @param bus the mod event bus
     */
    protected void init(IEventBus bus) {

        ITEMS.register(bus);
        ENTITY_TYPES.register(bus);
        CREATIVE_TABS.register(bus);
        SOUNDS.register(bus);
        ATTRIBUTES.register(bus);
        MENUS.register(bus);
        bus.register(this);

        IModInfo modInfo = ModList.get().getModContainerById(MOD_ID).get().getModInfo();
        ModFileScanData scanData = modInfo.getOwningFile().getFile().getScanResult();

        List<ClassData> allClasses = scanData.getClasses().stream()
            .filter(classData -> !classData.clazz().getClassName().contains(".mixins.") && !classData.clazz().getClassName().contains(".mixin."))
            .filter(classData -> FMLEnvironment.dist != Dist.DEDICATED_SERVER || 
                (!classData.clazz().getClassName().toLowerCase().contains("renderer") &&
                 !classData.clazz().getClassName().toLowerCase().contains("model") &&
                 !classData.clazz().getClassName().toLowerCase().contains("layer")))
            .toList();

        // First pass: register all Resources (tabs, sounds, entity stats, attributes)
        for (ClassData classData : allClasses) {
            try {
                String className = classData.clazz().getClassName();
                Class<?> clazz = Class.forName(className, false, RegisterFactory.class.getClassLoader());

                if (EResources.class.isAssignableFrom(clazz) && clazz != EResources.class) {
                    try {
                        EResources resources = (EResources) clazz.getDeclaredConstructor().newInstance();
                        registerResources(resources);
                    } catch (Exception e) {
                        printError(clazz, e, 0);
                    }
                }
            } catch (Exception e) {
                printError(null, e, 0);
            }
        }

        // Second pass: register all @AutoRegister entities and items
        for (ClassData classData : allClasses) {
            try {
                String className = classData.clazz().getClassName();
                Class<?> clazz = Class.forName(className, false, RegisterFactory.class.getClassLoader());

                if (FMLEnvironment.dist.isClient()) {
                    if (EntityModel.class.isAssignableFrom(clazz)) {
                        registerLayers(clazz);
                        continue;
                    }
                }
                if (clazz.isAnnotationPresent(AutoRegister.class)) {
                    AutoRegister ann = clazz.getAnnotation(AutoRegister.class);

                    if (Entity.class.isAssignableFrom(clazz)) {
                        registerEntities(clazz, ann);
                        if (Mob.class.isAssignableFrom(clazz)) registerEggs(ann);

                    } else if (Item.class.isAssignableFrom(clazz)) {
                        registerItems(clazz, ann);

                    } else if (PigeGui.class.isAssignableFrom(clazz)) {
                        registerGuis(clazz, ann);

                    } else if (PigeConfig.class.isAssignableFrom(clazz)
                               && registeredConfig == null) {
                        registerConfig(clazz);
                    }

                    try {
                        clazz.getDeclaredMethod("init");
                        CLASSES_TO_INIT.add(clazz);
                    } catch (NoSuchMethodException ignored) {
                    }
                }
            } catch (Exception e) {
                printError(null, e, 0);
            }
        }

        // Third pass: generate biome-spawn data files.
        // In 1.20.1 'forge:biome_modifier' is a datapack registry, so spawns
        // must be provided as JSON (loaded when a world/datapacks load). We write
        // them to disk first, then expose the folder via a runtime resource pack
        // so they are picked up on the FIRST world load (no rebuild/re-launch).
        // They are also seeded into src/main/resources so the built jar carries
        // them for production.
        generateBiomeSpawnFiles();
        generateItemModels();
    }

    /**
     * Writes {@code forge:add_spawns} biome modifier JSON files, one per spawn
     * definition, into two locations:
     * <ul>
     *   <li>{@code config/<modid>/generated/data/<modid>/forge/biome_modifier/}
     *       — exposed at runtime via a generated resource pack (first-run works);</li>
     *   <li>{@code src/main/resources/data/<modid>/forge/biome_modifier/}
     *       — baked into the built jar for production.</li>
     * </ul>
     */
    private void generateBiomeSpawnFiles() {
        if (ENTITY_SPAWNS.isEmpty()) return;

        // Dev: write into the runtime-pack folder (config/<modid>/generated/...).
        // Prod: bake into src/main/resources so the built jar carries them.
        // Exactly one source is used to avoid duplicate biome-modifier ids.
        String base = FMLEnvironment.production
            ? "../src/main/resources/data/" + MOD_ID
            : "config/" + MOD_ID + "/generated/data/" + MOD_ID;
        java.io.File dir = new java.io.File(base + "/forge/biome_modifier");

        ENTITY_SPAWNS.forEach((id, ctx) -> {
            List<SpawnPlace.SpawnBuilder> builders = ctx.getBuilders();
            for (int i = 0; i < builders.size(); i++) {
                SpawnDefinition rule = builders.get(i).build();

                String biomes;
                if (rule.biome() != null) {
                    biomes = "#" + rule.biome().location();       // tag → "#namespace:path"
                } else if (rule.biomeKey() != null) {
                    biomes = rule.biomeKey().location().toString(); // single biome id
                } else {
                    continue;
                }

                com.google.gson.JsonObject spawner = new com.google.gson.JsonObject();
                spawner.addProperty("type", MOD_ID + ":" + id);
                spawner.addProperty("weight", rule.weight());
                spawner.addProperty("minCount", rule.min());
                spawner.addProperty("maxCount", rule.max());

                com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                json.addProperty("type", "forge:add_spawns");
                json.addProperty("biomes", biomes);
                json.add("spawners", spawner);

                writeJsonIfChanged(new java.io.File(dir, id + "_spawn_" + i + ".json"), json);
            }
        });

        // pack.mcmeta is required for the runtime-pack folder to be a valid pack.
        if (!FMLEnvironment.production) {
            com.google.gson.JsonObject pack = new com.google.gson.JsonObject();
            com.google.gson.JsonObject packInfo = new com.google.gson.JsonObject();
            packInfo.addProperty("pack_format", 15);
            packInfo.addProperty("description", MOD_ID + " generated data");
            pack.add("pack", packInfo);
            writeJsonIfChanged(new java.io.File("config/" + MOD_ID + "/generated/pack.mcmeta"), pack);
        }
    }

    /**
     * Writes a minimal {@code item/generated} model JSON for every registered item
     * (excluding spawn eggs), one per item id, into two locations:
     * <ul>
     *   <li>{@code config/<modid>/generated/assets/<modid>/models/item/<id>.json}
     *       — exposed at runtime via the generated resource pack (first-run works);</li>
     *   <li>{@code src/main/resources/assets/<modid>/models/item/<id>.json}
     *       — baked into the built jar for production.</li>
     * </ul>
     * This removes the "Unable to load model" warnings for items that have no
     * hand-authored model JSON.
     */
    private void generateItemModels() {
        if (REGISTERED_ITEMS.isEmpty()) return;

        // Same single-source rule as generateBiomeSpawnFiles: dev → runtime pack,
        // prod → src/main/resources. Only item ids (no spawn eggs).
        String base = FMLEnvironment.production
            ? "../src/main/resources/assets/" + MOD_ID
            : "config/" + MOD_ID + "/generated/assets/" + MOD_ID;
        java.io.File dir = new java.io.File(base + "/models/item");

        REGISTERED_ITEMS.forEach((id, registry) -> {
            if (id.endsWith("_spawn_egg")) return; // handled by Forge's spawn egg model

            com.google.gson.JsonObject model = new com.google.gson.JsonObject();
            model.addProperty("parent", "item/generated");
            com.google.gson.JsonObject textures = new com.google.gson.JsonObject();
            // layer0 must be a full texture path (with "textures/") so Minecraft
            // resolves it from assets/<modid>/textures/... — a bare "modid:items/id"
            // would be looked up under assets/<modid>/items/ and never found.
            String layer0 = itemTextureExists(base, id)
                ? MOD_ID + ":textures/items/" + id
                : MOD_ID + ":textures/misc/none";
            textures.addProperty("layer0", layer0);
            model.add("textures", textures);

            writeJsonIfChanged(new java.io.File(dir, id + ".json"), model);
        });
    }

    /** Returns true if a hand-authored texture PNG exists for the given item id. */
    private boolean itemTextureExists(String assetsBase, String id) {
        for (String root : new String[] { assetsBase, "config/" + MOD_ID + "/generated/assets/" + MOD_ID }) {
            if (new java.io.File(root + "/textures/item/" + id + ".png").isFile()) return true;
        }
        return false;
    }

    /**
     * Writes a {@link com.google.gson.JsonObject} to a file only if its content
     * differs from the current file content.
     *
     * @param file the destination file
     * @param json the JSON content to write
     */
    private void writeJsonIfChanged(java.io.File file, com.google.gson.JsonObject json) {
        try {
            String content = new com.google.gson.GsonBuilder()
                .setPrettyPrinting().create().toJson(json);

            if (file.exists()) {
                String existing = new String(
                    java.nio.file.Files.readAllBytes(file.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
                if (existing.equals(content)) return; // unchanged, leave file alone
            } else if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            java.nio.file.Files.write(file.toPath(),
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            PigeonCore.LOGGER.info("[Spawns] Wrote biome modifier {}", file.getName());
        } catch (Exception e) {
            PigeonCore.LOGGER.warn("[Spawns] Failed to write biome modifier {}", file.getName(), e);
        }
    }

    /**
     * Registers tabs, sounds, entity stats, attributes, and spawn contexts
     * from an {@link EResources} instance.
     *
     * @param resources the resources container to register
     */
    private void registerResources(EResources resources) {
        try {
            String modid = this.MOD_ID;

            for (Tab tab : resources.getTabs()) {
                if (tab == null) continue;

                tab.setModid(modid);
                TABS.put(tab.getId(), tab);
                registerPigeid(tab.getId());

                if (tab.isCreative()) {
                    MutableComponent component = Component.translatable("itemGroup." + MOD_ID + "." + tab.getId());

                    CREATIVE_TABS.register(tab.getId(), () -> CreativeModeTab.builder().title(component)
                        .icon(() -> new ItemStack(getItem(tab.getIcon()))).displayItems((parameters, data) -> tab.forEach(item -> data.accept(item))).build());
                }
            }

            for (Location location : resources.getSounds()) {

                if (location == null) continue;
                String registryName = location.getObject();
                ResourceLocation soundId = new ResourceLocation(modid, registryName);
                RegistryObject<SoundEvent> registry = SOUNDS.register(registryName, () -> SoundEvent.createVariableRangeEvent(soundId));
                
                REGISTERED_SOUNDS.put(registryName, registry);
                registerPigeid(registryName);
            }
            
            for (InitStats stats : resources.getEntityStats()) {
                if (stats == null) continue;
                ENTITY_STATS.put(stats.getSubjectId(), stats);
            }

            for (Generic generic : resources.getAttributes()) {
                if (generic == null) continue;

                String registryName = generic.get("id", String.class);
                Supplier<Attribute> attributeSupplier = generic.getOrDefault("attribute", () -> null);
                RegistryObject<Attribute> registry = ATTRIBUTES.register(registryName, attributeSupplier);
                REGISTERED_ATTRIBUTES.put(registryName, registry);
                registerPigeid(registryName);
            }

            for (SpawnPlace.SpawnContext ctx : resources.getEntitySpawns()) {
                if (ctx == null) continue;
                ENTITY_SPAWNS.put(ctx.getId(), ctx);
                registerPigeid(ctx.getId());
                // Keep the first rule's placement info for SpawnPlacementRegisterEvent.
                if (!ctx.getBuilders().isEmpty()) {
                    SPAWN_PLACEMENTS.put(ctx.getId(), ctx.getBuilders().get(0));
                }
            }

        } catch (Exception e) {
            printError(resources.getClass(), e, 6);
        }
    }

    /**
     * Registers an entity type, spawn egg, and associated attribute suppliers
     * for a class annotated with {@link AutoRegister}.
     *
     * @param clazz the entity class
     * @param ann   the auto-register annotation
     */
    private void registerEntities(Class<?> clazz, AutoRegister ann) {
        try {
            String id = ann.value();
            ENTITY_CLASSES.put(id, (Class<? extends Entity>) clazz);
        
            Constructor<?> ctor = clazz.getConstructor(EntityType.class, Level.class);
            ctor.setAccessible(true);

            EntityType.EntityFactory<Entity> factory = (type, level) -> {
                try { return (Entity) ctor.newInstance(type, level); }
                catch (Throwable t) { throw new RuntimeException(t); }
            };

            InitStats stats = ENTITY_STATS.get(id);
            if (stats != null) {
                ENTITY_STATS_BY_ID.put(id, stats);
                EntityType.Builder<Entity> builder = EntityType.Builder.of(factory, stats.getCategory())
                        .sized((float) stats.getBoundingBox().getWidth(), (float) stats.getBoundingBox().getHeight())
                        .clientTrackingRange((int) stats.getTrackingRange())
                        .updateInterval(stats.getUpdateInterval());

                if (stats.isFireImmune()) builder.fireImmune();
                RegistryObject<EntityType<? extends Entity>> registry = ENTITY_TYPES.register(id, () -> builder.build(id));

                REGISTERED_ENTITIES.put(id, registry);
                registerPigeid(id);

                if (LivingEntity.class.isAssignableFrom(clazz))
                    PENDING_ATTRIBUTES.put(id, () -> {
                        AttributeSupplier.Builder attrBuilder = Mob.createMobAttributes();
                        for (Map.Entry<String, Double> entry : stats.getAttributes().entrySet()) {

                            if (LivingStats.isVanillaAttribute(entry.getKey()))
                                attrBuilder.add(LivingStats.getVanillaAttribute(entry.getKey()), entry.getValue());
                            else {
                                RegistryObject<Attribute> attr = REGISTERED_ATTRIBUTES.get(entry.getKey());
                                if (attr != null && attr.isPresent())
                                    attrBuilder.add(attr.get(), entry.getValue());
                            }
                        }
                        return attrBuilder;
                    });
            } else {
                printError(clazz, null, 7);
            }
        } catch (Exception e) {
            printError(clazz, e, 2);
        }
    }

    /**
     * Registers a model layer definition for a class that exposes
     * {@code LAYER_LOCATION} and {@code createBodyLayer}.
     *
     * @param clazz the model/entity renderer class
     */
    private void registerLayers(Class<?> clazz) {
        try {
            var field = clazz.getDeclaredField("LAYER_LOCATION");
            field.setAccessible(true);

            ModelLayerLocation loc = (ModelLayerLocation) field.get(null);

            var method = clazz.getDeclaredMethod("createBodyLayer");
            method.setAccessible(true);

            PENDING_LAYERS.put(loc, () -> {
                try { 
                    return (LayerDefinition) method.invoke(null); 
                } catch (Exception e) { 
                    printError(clazz, e, 1);
                    return null; 
                }
            });
        } catch (NoSuchFieldException | NoSuchMethodException e) {
        } catch (IllegalAccessException e) {
            printError(clazz, e, 5); 

        } catch (Exception e) {
            printError(clazz, e, 5);
        }
    }

    /**
     * Registers a spawn egg item for an entity annotated with {@link AutoRegister}.
     *
     * @param ann the auto-register annotation for the entity
     */
    private void registerEggs(AutoRegister ann) {
        try {
            String id = ann.value() + (!ann.value().contains("turret") ? "_spawn_egg": "");
            RegistryObject<EntityType<? extends Entity>> entityType = REGISTERED_ENTITIES.get(ann.value());

            if (entityType != null) {
                RegistryObject<Item> eggRegistry = ITEMS.register(id, () -> new ForgeSpawnEggItem(
                    (Supplier<? extends EntityType<? extends Mob>>) (Object) entityType, -1, -1, new Item.Properties()));

                REGISTERED_ITEMS.put(id, eggRegistry);
                registerPigeid(ann.value());
                registerPigeid(id);
            }
        } catch (Exception e) {
            printError(null, e, 3);
        }
    }

    /**
     * Registers an item for a class annotated with {@link AutoRegister}.
     *
     * @param clazz the item class
     * @param ann   the auto-register annotation
     */
    private void registerItems(Class<?> clazz, AutoRegister ann) {
        try {
            Constructor<?> ctor;
            Object[] args;
            try {
                ctor = clazz.getConstructor(Item.Properties.class);
                args = new Object[]{new Item.Properties()};

            } catch (NoSuchMethodException e) {

                ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                args = new Object[]{};
            }
            final Constructor<?> finalCtor = ctor;
            final Object[] finalArgs = args;

            RegistryObject<Item> itemRegistry = ITEMS.register(ann.value(), () -> {

                try { return (Item) finalCtor.newInstance(finalArgs); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            REGISTERED_ITEMS.put(ann.value(), itemRegistry);
            registerPigeid(ann.value());
        } catch (Exception e) {
            printError(clazz, e, 4);
        }
    }

    /**
     * Registers a {@link PigeConfig} instance from a config class.
     *
     * @param clazz the config class
     */
    private void registerConfig(Class<?> clazz) {
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            software.hacker_E303.pigeon_core.common.PigeConfig config =
                (software.hacker_E303.pigeon_core.common.PigeConfig) ctor.newInstance();
            config.init();
            registeredConfig = config;
            CONFIGS.put(MOD_ID, config);
            seedLangFile(config);
            PigeonCore.LOGGER.debug("Registered PigeConfig for '{}' from class: {}", MOD_ID, clazz.getName());
        } catch (Exception e) {
            printError(clazz, e, 9);
        }
    }

    /**
     * In dev mode only: merges missing translation keys into
     * {@code src/main/resources/assets/<modid>/lang/en_us.json}.
     * Falls back to writing a template in the {@code config/} directory.
     */
    private void seedLangFile(software.hacker_E303.pigeon_core.common.PigeConfig config) {
        if (FMLEnvironment.production) return;

        java.util.Map<String, String> keys = config.generateLangKeys(MOD_ID);
        if (keys.isEmpty()) return;

        // Standard Forge dev working dir is {project}/run/ → lang file is one level up.
        java.io.File langFile = new java.io.File(
            "../src/main/resources/assets/" + MOD_ID + "/lang/en_us.json");

        if (!mergeLangFile(langFile, keys)) {
            // Fallback: template in config dir (always in game dir)
            java.io.File template = new java.io.File("config/" + MOD_ID + "-lang-template.json");
            writeLangTemplate(template, keys);
        }
    }

    /**
     * Merges missing translation keys into an existing lang JSON file.
     *
     * @param file the lang file to merge into
     * @param keys the translation keys to add
     * @return {@code true} if the file was updated, {@code false} on failure
     */
    private boolean mergeLangFile(java.io.File file, java.util.Map<String, String> keys) {
        try {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            String existingRaw = "{\n}";

            if (file.exists()) {
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                existingRaw = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(existingRaw);
                if (parsed.isJsonObject()) json = parsed.getAsJsonObject();
            } else {
                if (file.getParentFile() != null) file.getParentFile().mkdirs();
            }

            // Collect only truly missing keys
            java.util.List<java.util.Map.Entry<String, String>> missing = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, String> e : keys.entrySet()) {
                if (!json.has(e.getKey())) missing.add(e);
            }

            if (missing.isEmpty()) return true; // nothing to do — file stays untouched

            // Append missing entries via string surgery, preserving all existing formatting
            int lastBrace = existingRaw.lastIndexOf('}');
            if (lastBrace < 0) return false;

            String before = existingRaw.substring(0, lastBrace).stripTrailing();
            boolean needsComma = before.length() > 1 && before.charAt(before.length() - 1) != '{';

            StringBuilder sb = new StringBuilder(before);
            for (int i = 0; i < missing.size(); i++) {
                if (i == 0) {
                    if (needsComma) sb.append(',');
                    sb.append('\n'); // blank separator line between existing and new keys
                }
                java.util.Map.Entry<String, String> e = missing.get(i);
                sb.append('\n').append("  ")
                  .append('"').append(escapeJsonStr(e.getKey())).append("\": \"")
                  .append(escapeJsonStr(e.getValue())).append('"');
                if (i < missing.size() - 1) sb.append(',');
            }
            sb.append('\n').append('}');

            java.nio.file.Files.write(file.toPath(),
                sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            PigeonCore.LOGGER.info("[PigeConfig] {} lang key(s) added to {}", missing.size(), file.toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Escapes backslashes and double quotes for use inside JSON strings.
     *
     * @param s the input string
     * @return the escaped string
     */
    private static String escapeJsonStr(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Writes a lang JSON template file containing the provided translation keys.
     *
     * @param file the destination file
     * @param keys the translation keys to write
     */
    private void writeLangTemplate(java.io.File file, java.util.Map<String, String> keys) {
        try {
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            keys.forEach(json::addProperty);
            try (java.io.FileWriter w = new java.io.FileWriter(file)) {
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json, w);
            }
            PigeonCore.LOGGER.info("[PigeConfig] Lang template written to {}", file.toString());
        } catch (Exception e) {
            PigeonCore.LOGGER.warn("[PigeConfig] Failed to write lang template", e);
        }
    }

    /**
     * Registers a GUI, its container menu type, and its screen class
     * for a class annotated with {@link AutoRegister}.
     *
     * @param clazz the GUI class
     * @param ann   the auto-register annotation
     */
    private void registerGuis(Class<?> clazz, AutoRegister ann) {
        try {
            String id = ann.value();
            if (REGISTERED_MENUS.containsKey(id)) {
                PigeonCore.LOGGER.warn("GUI '{}' already registered, skipping: {}", id, clazz.getName());
                return;
            }

            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            PigeGui guiInstance = (PigeGui) ctor.newInstance();
            guiInstance.setId(id);
            guiInstance.setModid(MOD_ID);

            // IForgeMenuType supports a FriendlyByteBuf in the client-side factory,
            // letting PigeAutoContainer decode entity/position data sent by NetworkHooks.openScreen.
            RegistryObject<MenuType<?>>[] registryRef = new RegistryObject[1];
            registryRef[0] = MENUS.register(id, () -> IForgeMenuType.create(
                (windowId, inv, buf) -> new PigeAutoContainer(
                    (MenuType<?>) registryRef[0].get(), windowId, inv, buf)
            ));

            REGISTERED_MENUS.put(id, registryRef[0]);
            GUI_INSTANCES.put(id, guiInstance);
            registerPigeid(id);

            // Make the instance reachable via PigeGui.get() — MenuType is set later in FMLCommonSetupEvent.
            PigeGui.registerInstance(guiInstance);

            PENDING_SCREENS.put(id, clazz);

            PigeonCore.LOGGER.debug("Registered GUI '{}' from class: {}", id, clazz.getName());

        } catch (Exception e) {
            printError(clazz, e, 8);
        }
    }

    /**
     * Returns the registered {@link MenuType} for the given GUI id.
     *
     * @param id the GUI registry id
     * @return the menu type, or {@code null} if not registered
     */
    @Nullable
    protected MenuType<?> getMenuType(String id) {
        RegistryObject<MenuType<?>> registry = REGISTERED_MENUS.get(id);
        return registry != null ? registry.orElse(null) : null;
    }

    /**
     * Returns the registered {@link Tab} for the given id.
     *
     * @param id the tab registry id
     * @return the tab, or {@code null} if not registered
     */
    @Nullable
    protected Tab getTab(String id) {
        return TABS.containsKey(id) ? TABS.get(id) : null;
    }

    /**
     * Returns the registered {@link EntityType} for the given entity id.
     *
     * @param id the entity registry id
     * @return the entity type, or {@code null} if not registered
     */
    @Nullable
    protected EntityType<?> getEntityType(String id) {
        return REGISTERED_ENTITIES.containsKey(id) ? REGISTERED_ENTITIES.get(id).orElse(null) : null;
    }

    /**
     * Returns the registered {@link EntityType} for the given id, cast to the
     * provided entity class.
     *
     * @param id    the entity registry id
     * @param clazz the expected entity class
     * @return the entity type, or {@code null} if not registered
     */
    @Nullable
    protected <T extends Entity> EntityType<T> getEntityType(String id, Class<T> clazz) {
        if (!REGISTERED_ENTITIES.containsKey(id)) return null;

        var registryObject = REGISTERED_ENTITIES.get(id);
        if (registryObject.isPresent()) return (EntityType<T>) registryObject.get();

        return (EntityType<T>) ForgeRegistries.ENTITY_TYPES.getValue(Location.create(Path.NONE, id).from(MOD_ID));
    }
    
    /**
     * Returns the registered {@link Item} for the given id.
     *
     * @param id the item registry id
     * @return the item, or {@code null} if not registered
     */
    @Nullable
    protected Item getItem(String id) {
        RegistryObject<Item> registry = REGISTERED_ITEMS.get(id);
        return registry != null ? registry.orElse(null) : null;
    }

    /**
     * Returns the registered {@link SoundEvent} for the given id.
     *
     * @param id the sound registry id
     * @return the sound event, or {@code null} if not registered
     */
    @Nullable
    protected SoundEvent getSound(String id) {
        RegistryObject<SoundEvent> registry = REGISTERED_SOUNDS.get(id);
        return registry != null ? registry.orElse(null) : null;
    }

    /**
     * Returns the registered {@link Attribute} for the given id.
     *
     * @param id the attribute registry id
     * @return the attribute, or {@code null} if not registered
     */
    @Nullable
    protected Attribute getAttribute(String id) {
        RegistryObject<Attribute> registry = REGISTERED_ATTRIBUTES.get(id);
        return registry != null ? registry.orElse(null) : null;
    }

    /**
     * Returns {@link InitStats} for the given entity class.
     *
     * @param entityClass the entity class to look up
     * @return the entity stats, or {@code null}
     */
    @Nullable
    protected InitStats getEntityStats(Class<?> entityClass) {
        return null;
    }

    /**
     * Returns {@link InitStats} for the given entity registry id.
     *
     * @param id the entity registry id
     * @return the entity stats, or {@code null} if not found
     */
    @Nullable
    protected InitStats getEntityStats(String id) {
        return ENTITY_STATS_BY_ID.get(id);
    }

    /**
     * Returns the registered {@link PigeConfig} for the given modid.
     *
     * @param modid the mod id
     * @return the config, or {@code null} if not registered
     */
    @Nullable
    protected software.hacker_E303.pigeon_core.common.PigeConfig getConfig(String modid) {
        return CONFIGS.get(modid);
    }

    /**
     * Submits pending entity attribute suppliers during {@link EntityAttributeCreationEvent}.
     */
    @SubscribeEvent
    public void onAttributeCreation(EntityAttributeCreationEvent event) {

        PENDING_ATTRIBUTES.forEach((id, supplier) -> {
            RegistryObject<EntityType<? extends Entity>> registryObject = REGISTERED_ENTITIES.get(id);
        
            if (registryObject != null && registryObject.isPresent()) {

                EntityType<? extends LivingEntity> livingType = 
                    (EntityType<? extends LivingEntity>) (Object) registryObject.get();

                event.put(livingType, supplier.get().build());
            }
        });
        PENDING_ATTRIBUTES.clear();
    }

    /**
     * Registers pending model layer definitions during
     * {@link EntityRenderersEvent.RegisterLayerDefinitions}.
     */
    @SubscribeEvent
    public void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {

        PENDING_LAYERS.forEach((loc, supplier) -> {
            LayerDefinition definition = supplier.get();

            if (definition != null)
                event.registerLayerDefinition(loc, () -> definition);
        });
        PENDING_LAYERS.clear();
    }

    /**
     * Registers entity renderers for GeoEMob, AnimatableEMob, and EBullet entities
     * during {@link EntityRenderersEvent.RegisterRenderers}.
     */
    @SubscribeEvent
    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {

        REGISTERED_ENTITIES.forEach((id, registryObject) -> {
            Class<? extends Entity> entityClass = ENTITY_CLASSES.get(id);

            if (entityClass == null) return;
            EntityType<? extends Entity> type = registryObject.get();
            
            if (GeoEMob.class.isAssignableFrom(entityClass)) {

                EntityType<GeoEMob> geoMobType = (EntityType<GeoEMob>) (Object) type;
                event.registerEntityRenderer(geoMobType, GeoEMobRenderer::new);

            } else if (AnimatableEMob.class.isAssignableFrom(entityClass)) {

                EntityType<AnimatableEMob> PigeMobType = (EntityType<AnimatableEMob>) (Object) type;
                event.registerEntityRenderer(PigeMobType, AnimatableEMobRenderer::new);

            } else if (EBullet.class.isAssignableFrom(entityClass)) {
            
                EntityType<EBullet> bulletMobType = (EntityType<EBullet>) (Object) type;
                event.registerEntityRenderer(bulletMobType, BulletRenderer::new);
            }
        });
        ENTITY_CLASSES.clear();
    }

    /**
     * Registers config and GUI screens during {@link FMLClientSetupEvent}.
     */
    @SubscribeEvent
    public void onRegisterScreens(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {

        // Register Forge config screen for any discovered PigeConfig subclass.
        if (registeredConfig != null) {
            PigeConfig cfg = registeredConfig;
            PigeConfigScreen.load(cfg, MOD_ID);
            ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                    (mc, parent) -> new PigeConfigScreen(parent, cfg, MOD_ID)));
            PigeonCore.LOGGER.debug("Registered config screen for '{}'", MOD_ID);
        }

        event.enqueueWork(() -> {
            PENDING_SCREENS.forEach((id, guiClass) -> {
                try {
                    RegistryObject<MenuType<?>> menuRegistry = REGISTERED_MENUS.get(id);
                    if (menuRegistry == null || !menuRegistry.isPresent()) {
                        PigeonCore.LOGGER.warn("Cannot register screen for GUI '{}': MenuType not found", id);
                        return;
                    }

                    MenuType<PigeAutoContainer> menuType =
                        (MenuType<PigeAutoContainer>) menuRegistry.get();

                    // All PigeGui subclasses use PigeAutoScreen — no separate Screen class needed.
                    MenuScreens.register(menuType, PigeAutoScreen::new);
                    PigeonCore.LOGGER.debug("Registered PigeAutoScreen for GUI '{}'", id);

                } catch (Exception e) {
                    PigeonCore.LOGGER.error("Failed to register screen for GUI '{}'", id, e);
                }
            });
            PENDING_SCREENS.clear();
        });
    }

    /**
     * Initializes GUI menu types and invokes static {@code init()} methods on
     * annotated classes during {@link FMLCommonSetupEvent}.
     */
    @SubscribeEvent
    public void init(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Resolve MenuType for every registered PigeGui now that all registries are ready.
            GUI_INSTANCES.forEach((id, gui) -> {
                RegistryObject<MenuType<?>> registry = REGISTERED_MENUS.get(id);
                if (registry != null && registry.isPresent())
                    gui.setMenuType(registry.get());
            });

            CLASSES_TO_INIT.forEach(clazz -> {
                try {
                    var method = clazz.getDeclaredMethod("init");
                    method.setAccessible(true);
                    method.invoke(null);
                } catch (Exception e) {
                }
            });
            CLASSES_TO_INIT.clear();
        });
    }

    /**
     * Registers spawn placements (how/where the mob may spawn) with Forge.
     * Uses {@link SpawnPlacementRegisterEvent}, the correct Forge 1.20.1 API: it is
     * fired once on the mod bus and MERGES placements, so it never throws the
     * "Duplicate registration" IllegalStateException that the deprecated
     * {@link net.minecraft.world.entity.SpawnPlacements#register} would.
     * <p>
     * The biome spawn <em>lists</em> (which biomes the mob appears in) are provided
     * separately via the generated {@code forge:add_spawns} biome-modifier datapack.
     */
    @SubscribeEvent
    @SuppressWarnings("rawtypes")
    public void onSpawnPlacementRegister(SpawnPlacementRegisterEvent event) {
        SPAWN_PLACEMENTS.forEach((id, builder) -> {
            RegistryObject<EntityType<? extends Entity>> registry = REGISTERED_ENTITIES.get(id);
            if (registry == null || !registry.isPresent()) return;

            SpawnDefinition rule = builder.build();
            // SpawnPlacementRegisterEvent requires a non-null placement + heightmap for a NEW entry.

            EntityType<? extends Entity> type = (EntityType) registry.get();
            event.register(
                type,
                rule.placementType(),
                rule.heightmapType(),
                (SpawnPlacements.SpawnPredicate) rule.predicate(),
                SpawnPlacementRegisterEvent.Operation.REPLACE
            );
        });
    }

    /**
     * Exposes the generated biome-modifier JSON (written to
     * {@code config/<modid>/generated/}) to Minecraft's pack system so spawns are
     * applied on the FIRST world load, without a rebuild. Registered for both
     * client resources and server data pack types.
     */
    @SubscribeEvent
    public void onAddPackFinders(AddPackFindersEvent event) {
        // Always (re)generate the JSON files BEFORE the pack is mounted, so the
        // ResourceManager reload that follows this event can actually see them.
        // Previously the files were written only at the end of init() — which can
        // race with AddPackFindersEvent and produce "Unable to load model" / no
        // spawns on the first run.
        generateBiomeSpawnFiles();
        generateItemModels();

        java.io.File folder = new java.io.File("config/" + MOD_ID + "/generated");
        if (!folder.isDirectory()) return;

        // pack.mcmeta is mandatory for the folder to be a valid pack. Write it
        // unconditionally (not only when spawns exist) so readMetaAndCreate never
        // returns null and crashes PackRepository.
        if (!FMLEnvironment.production) {
            com.google.gson.JsonObject pack = new com.google.gson.JsonObject();
            com.google.gson.JsonObject packInfo = new com.google.gson.JsonObject();
            packInfo.addProperty("pack_format", 15);
            packInfo.addProperty("description", MOD_ID + " generated data");
            pack.add("pack", packInfo);
            writeJsonIfChanged(new java.io.File(folder, "pack.mcmeta"), pack);
        }

        java.nio.file.Path packRoot = folder.toPath();
        boolean isClient = event.getPackType() == PackType.CLIENT_RESOURCES;
        PigeonCore.LOGGER.info("[Packs] Registering generated pack for {} (isClient={})", event.getPackType(), isClient);
        event.addRepositorySource((Consumer<Pack> sink) -> {
            String name = MOD_ID + "_generated_" + (isClient ? "assets" : "data");
            PathPackResources pack = new PathPackResources(name, isClient, packRoot);
            Pack.ResourcesSupplier supplier = (String id) -> pack;
            Pack packInfo = Pack.readMetaAndCreate(
                name,
                Component.literal(name),
                true,
                supplier,
                event.getPackType(),
                Pack.Position.BOTTOM,
                PackSource.BUILT_IN);
            sink.accept(packInfo);
        });
    }

    /**
     * Logs a formatted registry error message.
     *
     * @param clazz     the class where the error occurred, or {@code null}
     * @param exception the exception that caused the error, or {@code null}
     * @param error     the numeric error code determining the message
     */
    private void printError(Class<?> clazz, final Exception exception, final int error) {

        String name = (clazz != null) ? clazz.getName() : "Unknown Class";
        String message = switch(error) {

            case 0 -> "Failed to load class: " + name;
            case 1 -> "Failed to extract attributes from class: " + name;
            case 2 -> "Entity auto-registration failed for: " + name;
            case 3 -> "Failed to register spawn egg for: " + name;
            case 4 -> "Item auto-registration failed for: " + name;
            case 5 -> "Layer/Model registration failed for: " + name;
            case 6 -> "Resources (tabs/sounds/etc.) registration failed for: " + name;
            case 7 -> "Skipped entity (no Stats registered): " + name;
            case 8 -> "GUI auto-registration failed for: " + name;
            case 9 -> "Config auto-registration failed for: " + name;
            default -> "Unknown error in registries for: " + name;
        };
        PigeonCore.LOGGER.error(message, exception);
    }

    /**
     * Adds a pigeon id to the global registered set.
     *
     * @param pigeid the id to register
     */
    public static void registerPigeid(String pigeid) {
        PigeonCore.REGISTERED_PIGEIDS.add(pigeid);
    }
}
