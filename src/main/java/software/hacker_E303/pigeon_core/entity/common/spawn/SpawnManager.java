package software.hacker_E303.pigeon_core.entity.common.spawn;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import software.hacker_E303.pigeon_core.entity.common.spawn.SpawnPlace.SpawnBuilder;

/**
 * Accumulates spawn configurations and registers them with Forge
 * during {@code FMLCommonSetupEvent}.
 * <p>
 * This class is package-private; it is used by {@code RegisterFactory}.
 */
public final class SpawnManager {

    private static final Map<String, List<SpawnBuilder>> SPAWN_DATA = new LinkedHashMap<>();

    /** Not instantiable. */
    private SpawnManager() { }

    /**
     * Stores a list of {@link SpawnBuilder}s for the given entity id.
     * Called from {@code RegisterFactory} during common setup.
     */
    public static void addSpawns(@Nonnull String entityId, @Nonnull List<SpawnBuilder> rules) {
        SPAWN_DATA.put(entityId, rules);
    }

    /**
     * Iterates over all registered spawn data.
     */
    public static void forEach(@Nonnull BiConsumer<String, List<SpawnBuilder>> action) {
        SPAWN_DATA.forEach(action);
    }

    /** Returns {@code true} if any spawn data has been registered. */
    public static boolean hasEntries() {
        return !SPAWN_DATA.isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Forge registration helpers (called from RegisterFactory)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Registers all spawn placements with {@link SpawnPlacements}.
     * Must be called during {@code FMLCommonSetupEvent}.
     */
    public static void registerPlacements(@Nonnull Map<String, EntityType<?>> entityTypes) {
        forEach((id, builders) -> {
            EntityType<?> type = entityTypes.get(id);
            if (type == null || builders.isEmpty()) return;

            // SpawnPlacements allows only ONE placement per entity type, so register
            // it once using the first rule (all biome entries share the same type).
            tryRegister(type, builders.get(0));
        });
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    private static <T extends Mob> void tryRegister(EntityType<?> type, SpawnBuilder builder) {
        // 1. Safely cast the EntityType to the captured generic type T
        EntityType<T> targetType = (EntityType<T>) type;
        SpawnDefinition rule = builder.build();
        
        // 2. Safely cast the predicate to match the exact same T
        SpawnPlacements.SpawnPredicate<T> predicate = 
            (SpawnPlacements.SpawnPredicate<T>) rule.predicate();
        
        // 3. The compiler now accepts this because both arguments use the exact same 'T'
        SpawnPlacements.register(
            targetType,
            rule.placementType(),
            rule.heightmapType(),
            predicate
        );
    }

    /**
     * Returns an unmodifiable view of the spawn data (entity id → spawn rules).
     * Useful for future integration with biome modifiers or data generation.
     */
    public static Map<String, List<SpawnBuilder>> getSpawnData() {
        return Map.copyOf(SPAWN_DATA);
    }
}