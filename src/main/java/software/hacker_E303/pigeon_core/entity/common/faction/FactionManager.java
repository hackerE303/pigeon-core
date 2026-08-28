package software.hacker_E303.pigeon_core.entity.common.faction;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.entity.EntityType;

/**
 * Central registry for all named {@link Faction} instances.
 */
public final class FactionManager {

    private static final Map<String, Faction> REGISTRY = new HashMap<>();

    /**
     * Retrieves or creates a faction by name, adding the provided entity types as members.
     *
     * @param name     the faction name
     * @param entities the entity types to add as members
     * @return the existing or newly created {@link Faction}
     */
    public static Faction getOrCreate(String name, EntityType<?>... entities) {

        if (!REGISTRY.containsKey(name)) REGISTRY.put(name, new Faction(name, entities));
        else REGISTRY.get(name).add(entities);

        return REGISTRY.get(name);
    }

    /**
     * @param name the faction name
     * @return the {@link Faction} with the given name, or {@code null} if not registered
     */
    public static Faction getFactionFrom(String name) {
        return REGISTRY.get(name);
    }
}
