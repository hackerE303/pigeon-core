package software.hacker_E303.pigeon_core.entity.common.stats;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.entity.MobCategory;
import software.hacker_E303.pigeon_core.entity.common.BoundingBox;

/**
 * Initial stats template used during entity registration.
 * <p>
 * Provides a fluent builder API via {@link Selector} for constructing
 * {@link InitStats} instances with entity attributes, bounding box,
 * category, and spawn behavior.
 */
@SuppressWarnings("unchecked")
public final class InitStats {

    private static final Map<String, Double> BASE_VALUES = new HashMap<>();

    static {
        BASE_VALUES.put("health",                20.0);
        BASE_VALUES.put("range.follow",          64.0);
        
        BASE_VALUES.put("armor",                 0.0);
        BASE_VALUES.put("armor.toughness",       0.0);
        BASE_VALUES.put("knockback.resistence",  0.0);

        BASE_VALUES.put("speed",                 2.5);
        BASE_VALUES.put("speed.flying",          0.0);

        BASE_VALUES.put("attack.speed",          4.0);
        BASE_VALUES.put("attack.damage",         3.0);
        BASE_VALUES.put("knockback.attack",      0.0);

        BASE_VALUES.put("shoot_range",           3.0);
        BASE_VALUES.put("power_duration",        0.0);
    }

    private final Map<String, Double> values = new HashMap<>(BASE_VALUES);
    private final String id;

    private BoundingBox boundingBox = BoundingBox.create();
    private MobCategory category = MobCategory.MISC;

    private boolean fireImmune = false;

    private int updateInterval = 3;
    private double trackingRange = 120.0;

    private InitStats(String id) {
        this.id = id;
    }

    /**
     * @return the subject id for this stats template
     */
    public String getSubjectId() {
        return this.id;
    }

    /**
     * @return the {@link BoundingBox} used by the entity
     */
    public BoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    /**
     * @return the mob category for spawning
     */
    public MobCategory getCategory() {
        return this.category;
    }

    /**
     * @return {@code true} if the entity is immune to fire
     */
    public boolean isFireImmune() {
        return this.fireImmune;
    }

    /**
     * @return the entity update interval in ticks
     */
    public int getUpdateInterval() {
        return this.updateInterval;
    }

    /**
     * @return the entity tracking range in blocks
     */
    public double getTrackingRange() {
        return this.trackingRange;
    }

    /**
     * @return an unmodifiable view of attribute base values
     */
    public Map<String, Double> getAttributes() {
        return this.values;
    }

    protected InitStats set(String attribute, double value) {
        this.values.put(attribute, value);
        return this;
    }

    protected double get(String attribute) {
        return this.values.getOrDefault(attribute, 0.0);
    }

    private static InitStats createFor(String id) {
        return new InitStats(id);
    }

    /**
     * Creates a new stats selector for the given id.
     *
     * @param id the stats identifier
     * @return a new {@link Selector}
     */
    public static Selector create(String id) {
        return new Selector(id);
    }

    /**
     * Entry point for building {@link InitStats} instances.
     */
    public static final class Selector {

        private final String id;

        private Selector(String id) {
            this.id = id;
        }

        /**
         * Creates an entity stats builder.
         *
         * @return a new {@link EntityBuilder}
         */
        public EntityBuilder entity() {
            return new EntityBuilder(id);
        }

        /**
         * Creates a living entity stats builder.
         *
         * @return a new {@link LivingBuilder}
         */
        public LivingBuilder living() {
            return new LivingBuilder(id);
        }

        /**
         * Creates a mob stats builder.
         *
         * @return a new {@link MobBuilder}
         */
        public MobBuilder mob() {
            return new MobBuilder(id);
        }

        /**
         * Creates a turret stats builder.
         *
         * @return a new {@link TurretBuilder}
         */
        public TurretBuilder turret() {
            return new TurretBuilder(id);
        }

        /**
         * Creates a vehicle stats builder.
         *
         * @return a new {@link VehicleBuilder}
         */
        public VehicleBuilder vehicle() {
            return new VehicleBuilder(id);
        }
    }

    /**
     * Base builder for generic entity stats.
     */
    public static class EntityBuilder extends StatsBuilder<EntityBuilder> {

        protected EntityBuilder(String id) {
            super(id);
        }
    }

    /**
     * Builder for living entity stats.
     */
    public static class LivingBuilder extends EntityBuilder {
        
        protected LivingBuilder(String id) {
            super(id);
        }

        /**
         * Sets the health attribute value.
         *
         * @param value the health value
         * @return this builder
         */
        public LivingBuilder health(double value) {
            stats.set("health", value);
            return this;
        }

        /**
         * Sets the armor attribute value.
         *
         * @param value the armor value
         * @return this builder
         */
        public LivingBuilder armor(double value) {
            stats.set("armor", value);
            return this;
        }

        /**
         * Sets the movement speed attribute value.
         *
         * @param value the speed value
         * @return this builder
         */
        public LivingBuilder speed(double value) {
            stats.set("speed.movement", value);
            return this;
        }

        /**
         * Sets a custom attribute value.
         *
         * @param attribute the attribute key
         * @param value     the value
         * @return this builder
         */
        public LivingBuilder set(String attribute, double value) {
            stats.set(attribute, value);
            return this;
        }
        
        /**
         * Sets the entity update interval.
         *
         * @param value the update interval in ticks
         * @return this builder
         */
        @Override
        public LivingBuilder updateInterval(int value) {
            super.updateInterval(value);
            return this;
        }

        /**
         * Sets the entity tracking range.
         *
         * @param value the tracking range in blocks
         * @return this builder
         */
        @Override
        public LivingBuilder trackingRange(double value) {
            super.trackingRange(value);
            return this;
        }

        /**
         * Sets the entity bounding box.
         *
         * @param box the {@link BoundingBox}
         * @return this builder
         */
        @Override
        public LivingBuilder boundingBox(BoundingBox box) {
            super.boundingBox(box);
            return this;
        }

        /**
         * Sets the entity bounding box from dimensions.
         *
         * @param width   the width
         * @param height  the height
         * @param scale   the scale
         * @param shadow  the shadow scale
         * @return this builder
         */
        @Override
        public LivingBuilder boundingBox(double width, double height, double scale, double shadow) {
            super.boundingBox(width, height, scale, shadow);
            return this;
        }

        /**
         * Sets the mob category.
         *
         * @param category the mob category
         * @return this builder
         */
        public LivingBuilder category(MobCategory category) {
            stats.category = category;
            return this;
        }

        /**
         * Marks the entity as immune to fire.
         *
         * @return this builder
         */
        public LivingBuilder fireImmune() {
            stats.fireImmune = true;
            return this;
        }
    }

    /**
     * Builder for mob-specific stats.
     */
    public static class MobBuilder extends LivingBuilder {
        
        protected MobBuilder(String id) {
            super(id);
        }

        /**
         * Sets the attack damage attribute value.
         *
         * @param value the damage value
         * @return this builder
         */
        public MobBuilder damage(double value) {
            stats.set("attack.damage", value);
            return this;
        }

        /**
         * Sets the entity update interval.
         *
         * @param value the update interval in ticks
         * @return this builder
         */
        @Override
        public MobBuilder updateInterval(int value) {
            super.updateInterval(value);
            return this;
        }

        /**
         * Sets the entity tracking range.
         *
         * @param value the tracking range in blocks
         * @return this builder
         */
        @Override
        public MobBuilder trackingRange(double value) {
            super.trackingRange(value);
            return this;
        }

        /**
         * Sets the entity bounding box.
         *
         * @param box the {@link BoundingBox}
         * @return this builder
         */
        @Override
        public MobBuilder boundingBox(BoundingBox box) {
            super.boundingBox(box);
            return this;
        }

        /**
         * Sets the entity bounding box from dimensions.
         *
         * @param width   the width
         * @param height  the height
         * @param scale   the scale
         * @param shadow  the shadow scale
         * @return this builder
         */
        @Override
        public MobBuilder boundingBox(double width, double height, double scale, double shadow) {
            super.boundingBox(width, height, scale, shadow);
            return this;
        }

        /**
         * Sets a custom attribute value.
         *
         * @param attribute the attribute key
         * @param value     the value
         * @return this builder
         */
        @Override
        public MobBuilder set(String attribute, double value) {
            super.set(attribute, value);
            return this;
        }
    }

    /**
     * Builder for turret-specific stats.
     */
    public static class TurretBuilder extends StatsBuilder<TurretBuilder> {

        /**
         * Constructs a turret builder with sensible defaults.
         *
         * @param id the stats identifier
         */
        protected TurretBuilder(String id) {
            super(id);

            stats.category = MobCategory.MONSTER;
            stats.updateInterval = 1;
            this.shootRange(20.0);
            this.powerDuration(2400l);
        }

        /**
         * Sets the health attribute value.
         *
         * @param value the health value
         * @return this builder
         */
        public TurretBuilder health(double value) {
            stats.set("health", value);
            return this;
        }

        /**
         * Sets the attack damage attribute value.
         *
         * @param value the damage value
         * @return this builder
         */
        public TurretBuilder damage(double value) {
            stats.set("attack.damage", value);
            return this;
        }

        /**
         * Sets the shoot range attribute value.
         *
         * @param value the shoot range value
         * @return this builder
         */
        public TurretBuilder shootRange(double value) {
            stats.set("shoot_range", value);
            return this;
        }

        /**
         * Sets the power duration attribute value.
         *
         * @param value the power duration in ticks
         * @return this builder
         */
        public TurretBuilder powerDuration(long value) {
            stats.set("power_duration", value);
            return this;
        }
    }

    /**
     * Generic stats builder base class.
     */
    public static class StatsBuilder<T> {

        protected final InitStats stats;

        protected StatsBuilder(String id) {
            this.stats = InitStats.createFor(id);
        }

        /**
         * Sets the entity update interval.
         *
         * @param value the update interval in ticks
         * @return this builder
         */
        public T updateInterval(int value) {
            stats.updateInterval = value;
            return (T) this;
        }

        /**
         * Sets the entity tracking range.
         *
         * @param value the tracking range in blocks
         * @return this builder
         */
        public T trackingRange(double value) {
            stats.trackingRange = value;
            return (T) this;
        }

        /**
         * Sets the entity bounding box.
         *
         * @param box the {@link BoundingBox}
         * @return this builder
         */
        public T boundingBox(BoundingBox box) {
            stats.boundingBox = box;
            return (T) this;
        }

        /**
         * Sets the entity bounding box from dimensions.
         *
         * @param width   the width
         * @param height  the height
         * @param scale   the scale
         * @param shadow  the shadow scale
         * @return this builder
         */
        public T boundingBox(double width, double height, double scale, double shadow) {
            stats.boundingBox = BoundingBox.create(width, height, scale, shadow);
            return (T) this;
        }

        /**
         * Builds the {@link InitStats} instance.
         *
         * @return the built {@link InitStats}
         */
        public final InitStats build() {
            return this.stats;
        }
    }

    /**
     * Builder for vehicle stats.
     */
    public static class VehicleBuilder extends EntityBuilder {
        
        protected VehicleBuilder(String id) {
            super(id);
        }
    }
}
