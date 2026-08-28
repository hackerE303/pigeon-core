package software.hacker_E303.pigeon_core.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.hacker_E303.pigeon_core.actions.IGratherEvent;

/**
 * Client-side counterpart of {@link ServerPlayerMixin}. Cancels intentional
 * drops (Q key) when the held item refuses the {@link IGratherEvent}.
 *
 * <environment_details>
 *     <env>CLIENT</env>
 * </environment_details>
 */
@OnlyIn(Dist.CLIENT)
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void onDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {

        LocalPlayer player = (LocalPlayer) (Object) this;
        ItemStack selected = player.getInventory().getSelected();
        
        if (selected.isEmpty()) return;
        if (!(selected.getItem() instanceof IGratherEvent grather)) return;

        Level level = player.level();
        if (!grather.gatherEvent(selected.copy(), level, player)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}