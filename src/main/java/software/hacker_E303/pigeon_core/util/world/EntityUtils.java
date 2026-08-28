package software.hacker_E303.pigeon_core.util.world;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;

/**
 * Entity classification and distance helper methods.
 */
public class EntityUtils {

    private static final Map<String, TagKey<EntityType<?>>> ENTITY_TAGS = new HashMap<>();

    static {
        ENTITY_TAGS.put("projectiles", TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("minecraft:projectiles")));
    }

    /**
     * Checks whether the given entity matches a predefined entity tag.
     *
     * @param entity the entity to test
     * @param tag    the tag name to look up
     * @return {@code true} if the entity belongs to the requested tag
     */
    public static boolean isTaggedAs(Entity entity, String tag) {
        return ENTITY_TAGS.containsKey(tag) ? entity.getType().is(ENTITY_TAGS.get(tag)) : false;
    }

    /**
     * Calculates the Euclidean distance between two entities.
     *
     * @param ent1 the first entity
     * @param ent2 the second entity
     * @return the distance in blocks
     */
    public static double distBetween(Entity ent1, Entity ent2) {

        double deltaX = ent1.getX() - ent2.getX();
        double deltaY = ent1.getY() - ent2.getY();
        double deltaZ = ent1.getZ() - ent2.getZ();

        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    /**
     * Returns {@code true} if two entities are within the given distance limit.
     *
     * @param ent1     the first entity
     * @param ent2     the second entity
     * @param distLimit the maximum allowed distance
     * @return {@code true} if the entities are close enough
     */
    public static boolean isTooClose(Entity ent1, Entity ent2, double distLimit) {
        return distBetween(ent1, ent2) <= distLimit;
    }

    /**
     * Validates that a living entity is non-null and alive.
     *
     * @param living the entity to check
     * @return {@code true} if the entity is valid
     */
	public static boolean isValid(LivingEntity living) {
		return living != null && living.isAlive();
	}

    /**
     * Classifies an entity as an animal of the requested habitat types.
     *
     * @param entity the entity to test
     * @param land   {@code true} to accept land animals
     * @param water  {@code true} to accept water animals
     * @param flying {@code true} to accept flying mobs
     * @return {@code true} if the entity matches any of the requested animal types
     */
    public static boolean isAnimal(Entity entity, boolean land, boolean water, boolean flying) {
        if (entity == null) return false;

        boolean isLand  = false;
        boolean isWater = false;
        boolean isFlying = false;

        if (entity instanceof WaterAnimal)    isWater = true;
        else if (entity instanceof FlyingMob) isFlying = true;
        else if (entity instanceof Animal)    isLand = true;

        if (!isLand && !isWater && !isFlying) return false;
        return (land && isLand) || (water && isWater) || (flying && isFlying);
    }
}