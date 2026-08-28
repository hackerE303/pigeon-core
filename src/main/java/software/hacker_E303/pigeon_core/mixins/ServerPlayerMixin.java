package software.hacker_E303.pigeon_core.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.hacker_E303.pigeon_core.actions.IGratherEvent;

/**
 * Prevents a refused {@link IGratherEvent} item from being dropped on the
 * server.
 *
 * <p>{@link ServerPlayer#drop(boolean)} is the entry point for intentional
 * drops (Q key). It removes the stack from the inventory before creating the
 * entity, so cancelling here keeps the item safely in the inventory.</p>
 *
 * <environment_details>
 *     <env>DEDICATED_SERVER</env>
 * </environment_details>
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void onDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {

        ServerPlayer player = (ServerPlayer) (Object) this;
        ItemStack selected = player.getInventory().getSelected();

        if (selected.isEmpty() || !selected.onDroppedByPlayer(player)) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }
        if (!(selected.getItem() instanceof IGratherEvent grather)) return;

        Level level = player.level();
        if (!grather.gatherEvent(selected.copy(), level, player)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}