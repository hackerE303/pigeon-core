package software.hacker_E303.pigeon_core.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.hacker_E303.pigeon_core.actions.IGratherEvent;

/**
 * Blocks moving a refused {@link IGratherEvent} item out of its slot.
 *
 * <p>The requirement is not "the item may not be in the inventory" but "the item
 * may not be moved out of the slot it currently sits in" — from a chest, the
 * player inventory, or anywhere else. So we intercept {@code clicked} at the
 * head and, if the clicked (source) slot holds a refused item, or the currently
 * carried stack is a refused item, we cancel the whole click before anything is
 * mutated. No rollback, no slot rewriting — the item simply stays where it is.</p>
 *
 * <environment_details>
 *     <env>CLIENT</env>
 *     <env>DEDICATED_SERVER</env>
 * </environment_details>
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void onClickedHead(int slotIndex, int button, ClickType type, Player player, CallbackInfo ci) {

        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        // The slot being acted on (negative index means "outside the menu").

        if (slotIndex >= 0 && slotIndex < self.slots.size()) {
            ItemStack inSlot = self.getSlot(slotIndex).getItem();
            if (isRefused(inSlot, player)) {
                ci.cancel();
                return;
            }
        }

        // The stack currently held in the cursor: don't let a refused item be
        // placed/swapped either.
        if (isRefused(self.getCarried(), player)) {
            ci.cancel();
        }
    }

    private static boolean isRefused(ItemStack stack, Player player) {

        if (stack.isEmpty() || !(stack.getItem() instanceof IGratherEvent grather)) return false;
        Level level = player.level();
        
        if (level.isClientSide()) return false; // decision is server-side
        return !grather.gatherEvent(stack.copy(), level, player);
    }
}
