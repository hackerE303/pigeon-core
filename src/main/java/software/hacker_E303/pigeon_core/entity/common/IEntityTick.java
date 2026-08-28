package software.hacker_E303.pigeon_core.entity.common;

/**
 * Hook for entities that need per-tick update logic.
 */
public interface IEntityTick {
    
    /**
     * Called every tick.
     *
     * @return {@code true} to continue ticking, {@code false} to stop
     */
    default boolean tickEvent() {
        return true;
    }
}
