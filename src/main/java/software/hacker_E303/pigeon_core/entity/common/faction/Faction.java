package software.hacker_E303.pigeon_core.entity.common.faction;

import java.util.Arrays;
import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Represents a named faction that groups entity types and checks membership.
 */
public final record Faction(String name, List<EntityType<?>> members) {

    /**
     * Constructs a faction from a varargs list of entity types.
     *
     * @param name     the faction name
     * @param members  the member entity types
     */
    protected Faction(String name, EntityType<?>... members) {
        this(name, Arrays.asList(members));
    }

    /**
     * Adds additional entity types to this faction.
     *
     * @param members the entity types to add
     */
    protected void add(EntityType<?>... members) {
        this.members.addAll(Arrays.asList(members));
    }

    /**
     * Checks whether the given entity is a member of this faction.
     *
     * @param entity the entity to check
     * @return {@code true} if the entity is a member
     */
    protected boolean isMember(Entity entity) {

        if (entity == null) return false;

        if (entity instanceof IFaction factionable)
            return factionable.hasFaction() && factionable.getFaction().name().equals(this.name);

        return members.contains(entity.getType());
    }

    /**
     * Undead faction containing common undead entity types.
     */
    public static final Faction UNDEAD = FactionManager.getOrCreate("undead",

        EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITHER, EntityType.WITHER_SKELETON,
        EntityType.HUSK, EntityType.DROWNED, EntityType.ZOGLIN, EntityType.PHANTOM, EntityType.ZOMBIFIED_PIGLIN,
        EntityType.ZOMBIE_HORSE, EntityType.SKELETON_HORSE, EntityType.VEX, EntityType.GIANT
    );

    /**
     * Raider faction containing common raider entity types.
     */
    public static final Faction RAIDER = FactionManager.getOrCreate("raider",

        EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER,
        EntityType.VEX, EntityType.WITCH, EntityType.RAVAGER
    );

    /**
     * Faction for turret entities.
     */
    public static final Faction TURRETS = FactionManager.getOrCreate("turrets");

    /**
     * Faction for hostile turret entities.
     */
    public static final Faction HOSTILE_TURRETS = FactionManager.getOrCreate("hostile_turrets");

    /**
     * Faction for vehicle entities.
     */
    public static final Faction VEHICLES = FactionManager.getOrCreate("vehicles");
}
