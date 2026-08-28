package software.hacker_E303.pigeon_core.client.gun.animation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Manages animation caches for gun instances in the Pige Tech Weapons mod.
 * Provides static methods to get, create, remove, and clear animation caches
 * for individual gun instances identified by their unique instance IDs.
 * Uses a ConcurrentHashMap for thread-safe operations.
 */
@OnlyIn(Dist.CLIENT)
public class AnimationManager {

	public static final float TIME_STEP    = 1f / 60f;
	public static final float SOUND_FACTOR = 1.9f;
    
	public static final float BASE_WEIGHT  = 1.2f;
	public static final float BASE_LAMBDA  = 4.2f;

    public static AnimationCache CLIENT_CACHE = new AnimationCache();

    public static boolean IS_HOLDING_STARTED = false;

    public static boolean IS_CLIENT_GUN_VISIBLE = false;
	/** Flag indicating if the client player is currently holding a gun */
    public static boolean IS_CLIENT_HOLDING_GUN = false;

	public static boolean IS_CLIENT_AIMING  = false;
	public static boolean IS_CLIENT_RUNNING = false;

	public static int GUN_VISIBILITY_SINCH_TIME = 0;

    /** Concurrent map to store animation cache per gun instance */
    private static final Map<Long, AnimationCache.SparkData> sparks = new ConcurrentHashMap<>();

     /**
     * Gets or creates an animation cache for a gun item stack.
     * @param instanceId The gun instance ID to get the cache for
     * @return The animation cache for the gun instance
     */
    public static AnimationCache.SparkData getSparkForInstance(long instanceId) {
        return sparks.computeIfAbsent(instanceId, data -> new AnimationCache.SparkData());
    }

    /**
     * Clears all animation cache entries.
     */
    public static void clearSparkCache() {
        sparks.clear();
    }

    public static void resetValues() {
        CLIENT_CACHE = new AnimationCache();

        IS_CLIENT_GUN_VISIBLE = false;
        GUN_VISIBILITY_SINCH_TIME = 3;
    }
}