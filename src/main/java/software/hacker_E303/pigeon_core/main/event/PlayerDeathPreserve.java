package software.hacker_E303.pigeon_core.main.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps refused {@link software.hacker_E303.pigeon_core.actions.IGratherEvent}
 * items across death.
 *
 * <p>On death, {@code InventoryMixin} intercepts
 * {@code Inventory.dropAll()} (the death drop path) and, instead of dropping or
 * deleting refused stacks, stashes a copy here via {@link #preserve} and clears
 * the dead player's slot. On respawn the new {@code ServerPlayer} is created
 * with a fresh (empty) inventory — vanilla only copies the old inventory back
 * when the {@code keepInventory} game rule is on — so the refused stacks would
 * otherwise be lost. This handler restores them into the new player's inventory
 * when {@link PlayerEvent.PlayerRespawnEvent} fires.</p>
 *
 * <p>Note: player death/respawn does NOT go through {@code Entity.clone}, so
 * {@code PlayerEvent.Clone} is never posted for players. {@code PlayerRespawnEvent}
 * is the correct hook.</p>
 */
@Mod.EventBusSubscriber(modid = "pigeon_core", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerDeathPreserve {

    /** UUID (preserved across respawn) -> refused stacks to restore. */
    private static final Map<UUID, List<ItemStack>> PENDING = new ConcurrentHashMap<>();

    /**
     * Stashes a refused stack so it can be restored after respawn.
     *
     * @param playerUuid the player's UUID (preserved across respawn)
     * @param stack      the refused ItemStack to preserve
     */
    public static void preserve(UUID playerUuid, ItemStack stack) {
        PENDING.computeIfAbsent(playerUuid, k -> new ArrayList<>()).add(stack.copy());
    }

    /**
     * Restores refused stacks into the respawned player's inventory.
     *
     * @param event the respawn event
     */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        UUID uuid = player.getUUID();
        List<ItemStack> stacks = PENDING.remove(uuid);
        if (stacks == null || stacks.isEmpty()) return;

        // With keepInventory on, vanilla already restored the dead inventory
        // (the refused stack is still in its slot), so re-adding from PENDING
        // would duplicate it. Just discard the pending copy.
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;

        Inventory inv = player.getInventory();
        int size = inv.getContainerSize();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            if (!inv.add(stack)) {
                // No room in the main inventory: place into the first free slot.
                int free = firstFreeSlot(inv, size);
                if (free >= 0) inv.setItem(free, stack);
                // else: genuinely no space — drop is skipped (item is lost),
                // which matches vanilla behaviour when the inventory overflows.
            }
        }
    }

    /**
     * Returns the index of the first empty slot, or -1 if the inventory is full.
     *
     * @param inv  the inventory to scan
     * @param size number of slots to scan
     * @return the first free slot index, or -1
     */
    private static int firstFreeSlot(Inventory inv, int size) {
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i).isEmpty()) return i;
        }
        return -1;
    }
}
