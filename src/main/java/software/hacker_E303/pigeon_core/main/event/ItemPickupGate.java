package software.hacker_E303.pigeon_core.main.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.hacker_E303.pigeon_core.actions.IGratherEvent;

/**
 * Blocks pickup of a refused {@link IGratherEvent} item at the source: the item
 * entity is never added to the inventory, so the change-detector tick never sees
 * it and no refuse/drop loop can form. This is the only gate for ground items;
 * slot-to-slot moves are blocked by {@code AbstractContainerMenuMixin}.
 */
@Mod.EventBusSubscriber(modid = "pigeon_core", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ItemPickupGate {

    /**
     * Cancels pickup of a refused {@link IGratherEvent} item so the inventory
     * change detector never sees it.
     *
     * @param event the pickup event
     */
    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (event.isCanceled()) return;

        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) return;

        ItemStack stack = event.getItem().getItem();
        if (!(stack.getItem() instanceof IGratherEvent grather)) return;

        if (!grather.gatherEvent(stack.copy(), level, player)) {
            event.setCanceled(true);
        }
    }
}


