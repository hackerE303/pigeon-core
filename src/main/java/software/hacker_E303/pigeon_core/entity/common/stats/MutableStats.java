package software.hacker_E303.pigeon_core.entity.common.stats;

import software.hacker_E303.pigeon_core.PigeonCore;
import software.hacker_E303.pigeon_core.entity.common.BoundingBox;

/**
 * Base class for mutable stats backed by an {@link InitStats} template.
 */
public abstract class MutableStats {

    protected final InitStats stats;
    protected final BoundingBox box;

    /**
     * Constructs mutable stats for the given modid and key.
     *
     * @param modid the mod id
     * @param key   the stats identifier
     */
    protected MutableStats(String modid, String key) {
        this.stats = PigeonCore.getEntityStats(modid, key);
        this.box = this.stats.getBoundingBox().copy();
    }

    /**
     * @return the copied {@link BoundingBox} for this entity
     */
    public BoundingBox getBoundingBox() {
        return box;
    }
}
