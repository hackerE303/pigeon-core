package software.hacker_E303.pigeon_core.actions;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Handles reloading logic for gun-like items and entities.
 */
public interface IReload {

    /**
     * Handles the reload action.
     *
     * @param stack  the item stack; may be {@code null}
     * @param entity the entity performing the action; may be {@code null}
     * @return the resulting {@link ReloadAction}
     */
    public ReloadAction handleReload(@Nullable ItemStack stack, @Nullable Entity entity);

    /**
     * Encapsulates the result of a reload action.
     */
    public static class ReloadAction {

        private final int cooldown;

        private ReloadAction(int cooldown) {
            this.cooldown = cooldown;
        }

        /**
         * Returns the reload cooldown in ticks.
         *
         * @return the cooldown value
         */
        public int cooldown() {
            return cooldown;
        }

        /**
         * Creates a new reload action.
         *
         * @param cooldown the reload cooldown in ticks
         * @return a new {@link ReloadAction}
         */
        public static ReloadAction create(int cooldown) {
            return new ReloadAction(cooldown);
        }
    }
}