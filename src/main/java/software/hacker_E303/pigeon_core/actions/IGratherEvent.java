package software.hacker_E303.pigeon_core.actions;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Marks entities or items that can gather or collect resources.
 */
public interface IGratherEvent {

    /**
     * Called when the entity or item should gather resources.
     *
     * @param stack  the item stack involved
     * @param level  the current level
     * @param player the player performing the action
     * @return {@code true} if gathering should proceed
     */
    default boolean gatherEvent(ItemStack stack, Level level, Player player) {
        return true;
    }
}