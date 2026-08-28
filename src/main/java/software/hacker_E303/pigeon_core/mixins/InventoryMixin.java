package software.hacker_E303.pigeon_core.mixins;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.hacker_E303.pigeon_core.item.util.SlotInitializer;
import software.hacker_E303.pigeon_core.actions.IGratherEvent;
import software.hacker_E303.pigeon_core.main.event.PlayerDeathPreserve;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Handles refused {@link IGratherEvent} items on death (death-drop gate) and
 * initializes {@link EItem} stacks as soon as they enter an inventory slot.
 *
 * <environment_details>
 *     <env>CLIENT</env>
 *     <env>DEDICATED_SERVER</env>
 * </environment_details>
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Accessor("player")
    abstract Player getPlayer();

    @Inject(method = "setItem", at = @At("HEAD"))
    private void pigeon_core$onSlotSet(int slot, ItemStack stack, CallbackInfo ci) {
        Inventory self = (Inventory) (Object) this;

        // Only empty -> filled transition: the slot was empty and now receives an item.
        ItemStack previous = self.getItem(slot);
        if (!previous.isEmpty()) return;
        if (stack == null || stack.isEmpty()) return;

        Player player = getPlayer();
        Level level = player.level();
        SlotInitializer.onEnterSlotWithLevel(stack, player, level);
    }

    // Inventory#add(int, ItemStack) tries to MERGE into an existing compatible
    // stack (bumping its count in place) before ever calling setItem — and that
    // merge check compares NBT tags directly. A stack already sitting in the
    // inventory has already been through onEnterSlotWithLevel (5 essential/
    // bookkeeping tags); a freshly obtained one (e.g. picked from the creative
    // tab) hasn't yet (4 tags) at the moment this merge check runs. The mismatch
    // silently fails the merge, and setItem's own hook above then only catches up
    // afterwards — too late, the stacks are already split. Normalizing here, at
    // add()'s HEAD, runs before that merge check so both stacks compare equal.
    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"))
    private void pigeon_core$onAdd(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack == null || stack.isEmpty()) return;

        Player player = getPlayer();
        Level level = player.level();
        SlotInitializer.onEnterSlotWithLevel(stack, player, level);
    }

    @Inject(method = "dropAll", at = @At("HEAD"), cancellable = true)
    private void pigeon_core$onDropAll(CallbackInfo ci) {
        Inventory self = (Inventory) (Object) this;
        Player player = getPlayer();
        Level level = player.level();
        if (level.isClientSide()) return;

        int size = self.getContainerSize();

        boolean anyRefused = false;
        for (int i = 0; i < size; i++) {
            ItemStack stack = self.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IGratherEvent grather) {
                if (!grather.gatherEvent(stack.copy(), level, player)) {
                    anyRefused = true;
                    break;
                }
            }
        }
        if (!anyRefused) return;

        ci.cancel();
        for (int i = 0; i < size; i++) {
            ItemStack stack = self.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof IGratherEvent grather
                    && !grather.gatherEvent(stack.copy(), level, player)) {
                PlayerDeathPreserve.preserve(player.getUUID(), stack);
                continue;
            }
            player.drop(stack, true, false);
            self.setItem(i, ItemStack.EMPTY);
        }
    }
}