package software.hacker_E303.pigeon_core.mixins;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.hacker_E303.pigeon_core.actions.IGratherEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts both the generic and death-drop variants to return refused
 * {@link IGratherEvent} items back into the player's inventory instead of
 * spawning an {@link ItemEntity}.
 *
 * <environment_details>
 *     <env>CLIENT</env>
 *     <env>DEDICATED_SERVER</env>
 * </environment_details>
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void pigeon_core$onDropGeneric(ItemStack stack, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {

        if (stack == null || stack.isEmpty()) return;
        if (!(stack.getItem() instanceof IGratherEvent grather)) return;

        Player player = (Player) (Object) this;
        Level level = player.level();
        if (level.isClientSide()) return;

        if (!grather.gatherEvent(stack.copy(), level, player)) {
            cir.setReturnValue(null);
            cir.cancel();
            player.getInventory().add(stack);
        }
    }

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void pigeon_core$onDropDeath(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {

        if (stack == null || stack.isEmpty()) return;
        if (!(stack.getItem() instanceof IGratherEvent grather)) return;

        Player player = (Player) (Object) this;
        Level level = player.level();
        if (!grather.gatherEvent(stack.copy(), level, player)) {
            cir.setReturnValue(null);
            cir.cancel();
            player.getInventory().add(stack);
        }
    }
}