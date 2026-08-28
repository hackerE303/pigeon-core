package software.hacker_E303.pigeon_core.entity.common.faction;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;

/**
 * Interface for entities that can belong to a faction.
 */
public interface IFaction {
    
    /**
     * Returns the faction this entity belongs to, or {@code null} if none.
     *
     * @return the entity faction, or {@code null}
     */
    @Nullable
    default public Faction getFaction() {
        return null;
    }

    /**
     * @return {@code true} if this entity belongs to a faction
     */
    default boolean hasFaction() {
        return this.getFaction() != null;
    }

    /**
     * Determines whether the given entity is considered a friend.
     *
     * @param entity the entity to check
     * @return {@code true} if the entity is a friend
     */
    default boolean isFriend(@Nullable Entity entity) {
        return entity != null && this.hasFaction() && this.getFaction().isMember(entity);
    }
}
