package software.hacker_E303.pigeon_core.entity.common.stats;

import javax.annotation.Nonnull;

/**
 * Provides access to an entity's {@link MutableStats}.
 */
public interface IStats {

    /**
     * @return the entity's mutable stats; must not be null
     */
    @Nonnull
    MutableStats getStats();
}
