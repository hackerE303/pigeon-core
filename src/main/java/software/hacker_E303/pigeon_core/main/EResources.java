package software.hacker_E303.pigeon_core.main;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import software.hacker_E303.pigeon_core.common.Generic;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import software.hacker_E303.pigeon_core.common.Tab;
import software.hacker_E303.pigeon_core.entity.common.spawn.SpawnPlace;
import software.hacker_E303.pigeon_core.entity.common.stats.InitStats;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.MultiLocation;

/**
 * Abstract base for registering mod resources: creative tabs, sounds,
 * entity stats, spawns, and attributes.
 */
public abstract class EResources {

    /**
     * Returns the registered creative-mode tabs.
     *
     * @return an unmodifiable list of {@link Tab} instances
     */
    public final List<Tab> getTabs() {
        TabContext ctx = new TabContext();
        this.registerTabs(ctx);
        return ctx.TABS;
    }

    /**
     * Returns the registered sound locations.
     *
     * @return an unmodifiable list of {@link Location} instances
     */
    public final List<Location> getSounds() {
        SoundContext ctx = new SoundContext();
        this.registerSounds(ctx);
        return ctx.LOCATIONS;
    }

    /**
     * Returns the registered entity stats definitions.
     *
     * @return an unmodifiable list of {@link InitStats} instances
     */
    public final List<InitStats> getEntityStats() {
        EntityStatsContext ctx = new EntityStatsContext();
        this.registerEntityStats(ctx);
        return ctx.STATS;
    }

    /**
     * Returns the registered entity spawn contexts.
     *
     * @return an unmodifiable list of {@link SpawnPlace.SpawnContext} instances
     */
    public final List<SpawnPlace.SpawnContext> getEntitySpawns() {
        EntitySpawnsContext ctx = new EntitySpawnsContext();
        this.registerEntitySpawns(ctx);
        return ctx.PLACES;
    }

    /**
     * Returns the registered entity attributes.
     *
     * @return an unmodifiable list of {@link Generic} attribute definitions
     */
    public final List<Generic> getAttributes() {
        AttributeContext ctx = new AttributeContext();
        this.registerAttributes(ctx);
        return ctx.ATTRIBUTES;
    }

    /**
     * Registers creative-mode tabs.
     *
     * @param ctx the tab context
     */
    protected abstract void registerTabs(TabContext ctx);

    /**
     * Registers mod sound events grouped by category.
     *
     * @param ctx the sound context
     */
    protected abstract void registerSounds(SoundContext ctx);

    /**
     * Registers entity stats definitions.
     *
     * @param ctx the entity stats context
     */
    protected abstract void registerEntityStats(EntityStatsContext ctx);

    /**
     * Registers natural spawn definitions.
     *
     * @param ctx the entity spawns context
     */
    protected abstract void registerEntitySpawns(EntitySpawnsContext ctx);

    /**
     * Registers entity attributes.
     *
     * @param ctx the attribute context
     */
    protected abstract void registerAttributes(AttributeContext ctx);

    /**
     * Context for collecting {@link Tab} registrations.
     */
    protected final class TabContext {

        private final List<Tab> TABS = new ArrayList<>();

        /**
         * Adds a tab to the registration list.
         *
         * @param tab the tab to add
         * @return this context for chaining
         */
        public TabContext add(@Nonnull Tab tab) {
            this.TABS.add(tab);
            return this;
        }
    }

    /**
     * Context for collecting {@link Location} sound registrations.
     */
    protected final class SoundContext {

        private final List<Location> LOCATIONS = new ArrayList<>();

        /**
         * Adds a single sound location.
         *
         * @param location the location to add
         * @return this context for chaining
         */
        public SoundContext add(@Nonnull Location location) {
            this.LOCATIONS.add(location);
            return this;
        }

        /**
         * Adds multiple sound locations.
         *
         * @param locations the locations to add
         * @return this context for chaining
         */
        public SoundContext add(@Nonnull MultiLocation locations) {
            locations.forEach((loc) -> this.LOCATIONS.add(loc));
            return this;
        }
    }

    /**
     * Context for collecting {@link InitStats} registrations.
     */
    protected final class EntityStatsContext {

        private final List<InitStats> STATS = new ArrayList<>();

        /**
         * Adds an entity stats definition.
         *
         * @param stats the stats builder to add
         * @return this context for chaining
         */
        public EntityStatsContext add(@Nonnull InitStats.StatsBuilder<?> stats) {
            this.STATS.add(stats.build());
            return this;
        }
    }

    /**
     * Context for collecting {@link SpawnPlace.SpawnContext} registrations.
     */
    protected final class EntitySpawnsContext {

        private final List<SpawnPlace.SpawnContext> PLACES = new ArrayList<>();

        /**
         * Adds a spawn context.
         *
         * @param ctx the spawn context to add
         * @return this context for chaining
         */
        public EntitySpawnsContext add(@Nonnull SpawnPlace.SpawnContext ctx) {
            this.PLACES.add(ctx);
            return this;
        }
    }

    /**
     * Context for collecting attribute registrations.
     */
    protected final class AttributeContext {

        private final List<Generic> ATTRIBUTES = new ArrayList<>();

        /**
         * Registers a ranged attribute under the given id.
         *
         * @param id    the attribute identifier
         * @param value the default value
         * @param min   the minimum value
         * @param max   the maximum value
         * @return this context for chaining
         */
        public AttributeContext add(String id, double value, double min, double max) {
            this.ATTRIBUTES.add(Generic.create((ctx) -> {
                ctx.add("id", id)
                    .add("attribute", (Supplier<Attribute>) () -> new RangedAttribute("attribute.name." + id, value, min, max).setSyncable(true));
            }));
            return this;
        }
    }
}