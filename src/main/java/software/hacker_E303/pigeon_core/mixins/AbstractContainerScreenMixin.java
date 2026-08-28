package software.hacker_E303.pigeon_core.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.hacker_E303.pigeon_core.actions.IGratherEvent;

/**
 * Client-side counterpart of {@code AbstractContainerMenuMixin}. Cancelling the
 * server-side {@code clicked} still left a one-frame "bounce": the client is
 * predictive and animates the move before the server rejects it and re-syncs.
 * By cancelling {@code slotClicked} here (before the predictive move and the
 * network packet), the action is simply ignored — no bounce, no flicker.
 *
 * <environment_details>
 *     <env>CLIENT</env>
 * </environment_details>
 */
@OnlyIn(Dist.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void onSlotClicked(Slot slot, int slotId, int mouseButton, net.minecraft.world.inventory.ClickType type, CallbackInfo ci) {

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (slot != null && isRefused(slot.getItem(), player)) {
            ci.cancel();
            return;
        }

        // Block placing/swapping a refused stack that is currently carried.
        ItemStack carried = player.containerMenu.getCarried();
        if (isRefused(carried, player)) {
            ci.cancel();
        }
    }

    private static boolean isRefused(ItemStack stack, LocalPlayer player) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IGratherEvent grather)) return false;
        Level level = player.level();
        return !grather.gatherEvent(stack.copy(), level, player);
    }
}