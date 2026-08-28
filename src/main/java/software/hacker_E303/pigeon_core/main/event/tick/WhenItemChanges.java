package software.hacker_E303.pigeon_core.main.event.tick;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.hacker_E303.pigeon_core.actions.IGratherEvent;

/**
 * Notifies the owning {@link IGratherEvent} item via
 * {@link IGratherEvent#gatherEvent} whenever an inventory slot changes.
 *
 * <p>The change is detected purely by comparing each slot's previous and current
 * stack every server tick — it does NOT read or compare NBT. A change is
 * recorded only when the item type and/or count differ from the last snapshot for
 * that slot.</p>
 *
 * <p>The actual gate that prevents a refused item from being moved out of its
 * slot lives in {@code AbstractContainerMenuMixin} (server-side click cancel) plus
 * {@code ItemPickupGate} (ground pickup) and {@code CreativeModeInventoryScreenMixin}
 * (creative tab). This class is only a change notifier; it never removes items
 * from the inventory, because a refused item is allowed to stay where it is —
 * it just must not be moved.</p>
 *
 * <p>Per-slot previous stacks are remembered per player (keyed by UUID) over the
 * full inventory reported by {@code Inventory.getContainerSize()} (main + armor
 * + offhand). This runs only on the logical server.</p>
 */
@Mod.EventBusSubscriber(modid = "pigeon_core", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WhenItemChanges {

    /** Per-player snapshot of the last-seen stack for every tracked slot index. */
    private static final Map<UUID, ItemStack[]> LAST = new ConcurrentHashMap<>();

    /**
     * Number of inventory slots tracked per player. Inventory.getContainerSize()
     * already includes the offhand slot (36 main + 4 armor + 1 offhand = 41), so we
     * simply read whatever the inventory reports rather than hard-coding the layout.
     */
    private static final int INVENTORY_SLOTS = 41; // 36 main + 4 armor + 1 offhand

    /**
     * Snapshots the current inventory and notifies {@link IGratherEvent} items
     * of any slot change.
     *
     * @param event the server tick event
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            ServerLevel level = player.serverLevel();

            ItemStack[] previous = LAST.computeIfAbsent(player.getUUID(),
                k -> new ItemStack[INVENTORY_SLOTS]);

            var inv = player.getInventory();

            int slots = Math.min(INVENTORY_SLOTS, inv.getContainerSize());
            for (int i = 0; i < slots; i++) {
                ItemStack current = inv.getItem(i);

                ItemStack before = previous[i];
                if (before == null) before = ItemStack.EMPTY;

                if (sameStack(before, current)) continue;

                // A slot changed: notify the item (if any) of the new state.
                if (!current.isEmpty() && current.getItem() instanceof IGratherEvent grather) {
                    grather.gatherEvent(current.copy(), level, player);
                }

                previous[i] = current.copy();
            }
        }
    }

    /**
     * Two stacks are considered "the same slot content" only when they are both
     * empty, or both non-empty with equal item type AND equal count. We do NOT
     * compare NBT — a data change on the same item+count is intentionally not
     * treated as an item change here.
     */
    private static boolean sameStack(ItemStack a, ItemStack b) {
        boolean aEmpty = a.isEmpty();
        boolean bEmpty = b.isEmpty();
        if (aEmpty && bEmpty) return true;
        if (aEmpty != bEmpty) return false;
        return a.getItem() == b.getItem() && a.getCount() == b.getCount();
    }

    /**
     * Drops a player's inventory snapshot (e.g. on disconnect) to avoid memory leaks.
     *
     * @param playerId the player's UUID
     */
    public static void forget(UUID playerId) {
        LAST.remove(playerId);
    }
}
