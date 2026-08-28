package software.hacker_E303.pigeon_core.entity.common.spawn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Static factory and namespace for spawn configuration.
 * <p>
 * Usage in {@code EResources.registerEntitySpawns()}:
 * <pre>{@code
 * ctx.add(SpawnPlace.create("my_mob")
 *     .define(
 *         // single biome (no leading '#')
 *         SpawnDefinition.create("plains")
 *             .type(SpawnPlacements.Type.ON_GROUND)
 *             .weight(15).min(2).max(5).category(MobCategory.CREATURE),
 *         // biome tag (leading '#')
 *         SpawnDefinition.create("#minecraft:is_forest")
 *             .weight(10).min(1).max(3)
 *     )
 * );
 * }</pre>
 */
public final class SpawnPlace {

    private SpawnPlace() { }

    /**
     * Creates a new {@link SpawnContext} for an entity identified by the given id.
     */
    public static SpawnContext create(@Nonnull String id) {
        return new SpawnContext(id);
    }

    public static final class SpawnBuilder {

        private final SpawnDefinition rule;

        protected SpawnBuilder(SpawnDefinition rule) {
            this.rule = rule;
        }

        public SpawnBuilder type(SpawnPlacements.Type type) {
            rule.placementType = type; return this;
        }

        public SpawnBuilder heightmap(Heightmap.Types type) {
            rule.heightmapType = type; return this;
        }

        public SpawnBuilder predicate(SpawnPlacements.SpawnPredicate<Mob> predicate) {
            rule.predicate = predicate; return this;
        }

        public SpawnBuilder weight(int value) {
            rule.weight = value; return this;
        }

        public SpawnBuilder min(int count) {
            rule.minCount = count; return this;
        }
        public SpawnBuilder max(int count) {
            rule.maxCount = count; return this;
        }

        public SpawnBuilder category(MobCategory category) {
            rule.category = category; return this;
        }
        
        public SpawnDefinition build() {
            return rule;
        }
    }

    public static final class SpawnContext {

        private final String id;
        private final List<SpawnBuilder> builders = new ArrayList<>();

        private SpawnContext(String id) {
            this.id = id;
        }

        /**
         * Adds one or more {@link SpawnBuilder}s at once and returns
         * this context for fluent chaining.
         */
        public SpawnContext define(@Nonnull SpawnBuilder... builders) {
            this.builders.addAll(Arrays.asList(builders));
            return this;
        }

        /** The entity identifier (matches {@code @AutoRegister} value). */
        public String getId() {
            return id;
        }

        /** Returns all rules that have been added. */
        public List<SpawnBuilder> getBuilders() {
            return builders;
        }
    }
}