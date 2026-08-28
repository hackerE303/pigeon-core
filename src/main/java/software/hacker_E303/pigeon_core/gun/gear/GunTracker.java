package software.hacker_E303.pigeon_core.gun.gear;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.hacker_E303.pigeon_core.geo.item.gun.EGun;

/**
 * Client-side tracker for managing gun state and holder information.
 * Provides functionality to track if the client is holding a gun and manages
 * a cache of gun instances to their current holders.
 */
@OnlyIn(Dist.CLIENT)
public class GunTracker {

	/** Cache to track which entity is holding each gun instance */
	private static final Map<Long, Entity> gunHolders = new ConcurrentHashMap<>();

	// Gun Holder Cache Methods

	/**
	 * Records that an entity is holding a specific gun instance.
	 *
	 * @param stack The gun ItemStack
	 * @param entity The entity holding the gun
	 */
	public static void setGunHolder(ItemStack stack, Entity entity) {

		if (!entity.level().isClientSide) return;
		EGun.process(stack, gun -> {

			long instanceId = gun.getGeckoId();
			gunHolders.put(instanceId, entity);
		});
	}

    /**
     * Returns the entity currently holding a specific gun instance.
     *
     * @param stack the gun ItemStack
     * @return the entity holding the gun, or null if not held
     */
    public static Entity getGunHolder(ItemStack stack) {

		if (EGun.from(stack) == null) return null;
		return EGun.process(stack, gun -> {

			long instanceId = gun.getGeckoId();
			return gunHolders.get(instanceId);
		}, null);
	}

	/**
	 * Checks if the given ItemStack is being held by the current player instance.
	 *
	 * @param stack The ItemStack to check
	 * @return true if the stack is being held by the current player, false otherwise
	 */
	public static boolean isHeldByInstancePlayer(ItemStack stack) {
		Player currentPlayer = Minecraft.getInstance().player;
		Entity holder = getGunHolder(stack);
		return holder == currentPlayer;
	}

	/**
	 * Clears the holder information for a specific gun instance.
	 *
	 * @param stack The gun ItemStack
	 */
	public static void removeGunHolder(ItemStack stack) {

		EGun.process(stack, gun -> {

			long instanceId = gun.getGeckoId();
			gunHolders.remove(instanceId);
		});
	}

	/**
	 * Clears all gun holder data from the cache.
	 */
	public static void clearCache() {
		gunHolders.clear();
	}
}