package software.hacker_E303.pigeon_core.actions;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import software.hacker_E303.pigeon_core.PigeonCore;

/**
 * Handles shooting logic for gun-like items and entities.
 */
public interface IShoot {

    public static final Attribute SHOOT_RANGE = PigeonCore.getAttribute("pigeon_core", "shoot_range");

    /**
     * Handles the shooting action.
     *
     * @param stack  the item stack; may be {@code null}
     * @param entity the entity performing the action; may be {@code null}
     * @return the resulting {@link ShootAction}
     */
    public ShootAction handleShoot(@Nullable ItemStack stack, @Nullable Entity entity);

    /**
     * Encapsulates the result of a shooting action.
     */
    public static class ShootAction {

        private final float speed;
        private final float inaccuracy;
        private final float power;
        private final int count;
        private final int delay;
        private final int cooldown;

        private ShootAction(float speed, float inaccuracy, float power, int count, int delay, int cooldown) {
            this.speed = speed;
            this.inaccuracy = inaccuracy;
            this.power = power;
            this.count = count;
            this.delay = delay;
            this.cooldown = cooldown;
        }

        /**
         * Returns the projectile speed.
         *
         * @return the speed value
         */
        public float speed() {
            return speed;
        }

        /**
         * Returns the projectile inaccuracy.
         *
         * @return the inaccuracy value
         */
        public float inaccuracy() {
            return inaccuracy;
        }

        /**
         * Returns the projectile power.
         *
         * @return the power value
         */
        public float power() {
            return power;
        }

        /**
         * Returns the number of projectiles fired.
         *
         * @return the count value
         */
        public int count() {
            return count;
        }

        /**
         * Returns the firing delay in ticks.
         *
         * @return the delay value
         */
        public int delay() {
            return delay;
        }


        /**
         * Returns the cooldown duration in ticks.
         *
         * @return the cooldown value
         */
        public int cooldown() {
            return cooldown;
        }

        /**
         * Creates a new shoot action.
         *
         * @param speed      the projectile speed
         * @param inaccuracy the projectile inaccuracy
         * @param power      the projectile power
         * @param count      the number of projectiles
         * @param delay      the firing delay in ticks
         * @param cooldown   the cooldown duration in ticks
         * @return a new {@link ShootAction}
         */
        public static ShootAction create(float speed, float inaccuracy, float power, int count, int delay, int cooldown) {
            return new ShootAction(speed, inaccuracy, power, count, delay, cooldown);
        }
    }
}