package software.hacker_E303.pigeon_core.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.hacker_E303.pigeon_core.actions.IGratherEvent;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

/**
 * Client-side gate for the creative inventory tab. The creative tab is
 * client-predictive: it writes items into the carried/item picker state (and
 * directly into inventory slots) without ever consulting the server, so the
 * server-side gates cannot stop it — the item only vanishes once the client
 * re-syncs (e.g. when a container is opened).
 *
 * <p>To avoid that desync flicker, we cancel the creative click (and the
 * carried write) when the item is a refused {@link IGratherEvent}. The decision
 * is made client-side via {@code gatherEvent} with the client {@code Level};
 * {@code EItem#gatherEvent} guards its server-only work behind
 * {@code !level.isClientSide()}, so this is safe.</p>
 *
 * <environment_details>
 *     <env>CLIENT</env>
 * </environment_details>
 */
@OnlyIn(Dist.CLIENT)
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void onSlotClicked(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {

        if (slot == null) return;
        ItemStack stack = slot.getItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof IGratherEvent grather)) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (!grather.gatherEvent(stack.copy(), player.level(), player)) {
            ci.cancel();
        }
    }
}
