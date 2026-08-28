package software.hacker_E303.pigeon_core.entity.common.spawn;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;

import java.util.Set;
import javax.annotation.Nonnull;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import software.hacker_E303.pigeon_core.entity.common.spawn.SpawnPlace.SpawnBuilder;

/**
 * Defines spawn rules for an entity type, including biomes, dimensions,
 * placement conditions, weight, and group counts.
 */
public final class SpawnDefinition {

        protected static final Set<String> NETHER_BIOMES = Set.of("nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas");
        protected static final Set<String> END_BIOMES = Set.of("the_end", "end_highlands", "end_midlands", "small_end_islands", "end_barrens");

        protected final TagKey<Biome> biome;
        protected final ResourceKey<Biome> biomeKey;
        protected final ResourceKey<Level> dimension;

        protected SpawnPlacements.Type           placementType  = SpawnPlacements.Type.ON_GROUND;
        protected Heightmap.Types                heightmapType  = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        protected SpawnPlacements.SpawnPredicate<Mob> predicate     = Mob::checkMobSpawnRules;
        protected int                         weight         = 10;
        protected int                         minCount       = 1;
        protected int                         maxCount       = 3;
        protected MobCategory                 category       = MobCategory.MONSTER;

        /**
         * Constructs a spawn definition for the given biome/dimension combination.
         *
         * @param biome      the biome tag, or {@code null}
         * @param biomeKey   the single biome key, or {@code null}
         * @param dimension  the dimension resource key
         */
        protected SpawnDefinition(TagKey<Biome> biome, ResourceKey<Biome> biomeKey, ResourceKey<Level> dimension) {
            this.biome     = biome;
            this.biomeKey  = biomeKey;
            this.dimension = dimension;
        }

        /**
         * Creates a spawn definition for either a single biome or a biome tag.
         * <p>
         * If {@code biome} starts with {@code '#'} it is interpreted as a biome
         * <b>tag</b> (e.g. {@code "#minecraft:is_overworld"}); otherwise it is a
         * single concrete <b>biome</b> (e.g. {@code "plains"} or
         * {@code "minecraft:plains"}). A missing namespace defaults to
         * {@code minecraft}.
         *
         * @param biome a biome id or biome tag
         * @return a new {@link SpawnBuilder}
         */
        public static SpawnBuilder create(@Nonnull String biome) {
            boolean isTag = biome.charAt(0) == '#';
            if (isTag) biome = biome.substring(1);
            if (!biome.contains(":")) biome = "minecraft:" + biome;

            ResourceLocation location = new ResourceLocation(biome);

            String path = location.getPath();
            ResourceKey<Level> dimensionKey;

            if (NETHER_BIOMES.contains(path)) {
                dimensionKey = Level.NETHER;
            } else if (END_BIOMES.contains(path)) {
                dimensionKey = Level.END;
            } else {
                dimensionKey = Level.OVERWORLD; 
            }

            TagKey<Biome> biomeTag = isTag ? TagKey.create(Registries.BIOME, location) : null;
            ResourceKey<Biome> singleBiome = isTag ? null : ResourceKey.create(Registries.BIOME, location);

            return new SpawnBuilder(new SpawnDefinition(biomeTag, singleBiome, dimensionKey));
        }

        /**
         * @return the biome tag, or {@code null}
         */
        public TagKey<Biome> biome() {
            return this.biome;
        }

        /**
         * @return the single biome key, or {@code null}
         */
        public ResourceKey<Biome> biomeKey() {
            return this.biomeKey;
        }

        /**
         * @return the dimension resource key
         */
        public ResourceKey<Level> dimension() { 
            return this.dimension; 
        }
        
        /**
         * @return the spawn placement type
         */
        public SpawnPlacements.Type placementType() {
            return this.placementType;
        }

        /**
         * @return the required heightmap type
         */
        public Heightmap.Types heightmapType() { 
            return this.heightmapType; 
        }

        /**
         * @return the spawn predicate
         */
        public SpawnPlacements.SpawnPredicate<Mob> predicate() {
            return this.predicate;
        }

        /**
         * @return the spawn weight
         */
        public int weight() {
            return this.weight;
        }

        /**
         * @return the minimum group size
         */
        public int min() {
            return this.minCount;
        }

        /**
         * @return the maximum group size
         */
        public int max() {
            return this.maxCount; 
        }

        /**
         * @return the mob category
         */
        public MobCategory category() {
            return this.category;
        }    
    }
