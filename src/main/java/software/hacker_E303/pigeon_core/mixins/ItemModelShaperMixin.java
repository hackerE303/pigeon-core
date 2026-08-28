package software.hacker_E303.pigeon_core.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import software.hacker_E303.pigeon_core.client.item.EItemModelHandler;
import software.hacker_E303.pigeon_core.item.EItem;

/**
 * Redirects item-model resolution for {@link EItem}
 * stacks to {@link EItemModelHandler}, so the rendered texture comes from
 * {@code EItem#getTextureLocation(ItemStack)} instead of the static JSON model.
 *
 * <environment_details>
 *     <env>CLIENT</env>
 * </environment_details>
 */
@Mixin(ItemModelShaper.class)
public abstract class ItemModelShaperMixin {

    @Inject(method = "getItemModel(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/resources/model/BakedModel;", at = @At("RETURN"), cancellable = true)
    private void onGetItemModel(ItemStack stack, CallbackInfoReturnable<BakedModel> cir) {
        cir.setReturnValue(EItemModelHandler.resolve(stack, cir.getReturnValue()));
    }
}
