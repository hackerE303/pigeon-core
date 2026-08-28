package software.hacker_E303.pigeon_core.item.common;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Provides an optional item drop result when an {@link EItem} is consumed.
 */
public interface IEatResult {

    default Item getResult() {
        return Items.AIR;
    }
}