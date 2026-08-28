package software.hacker_E303.pigeon_core.mixins;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ItemLike;
import software.hacker_E303.pigeon_core.item.util.SlotInitializer;
import software.hacker_E303.pigeon_core.item.EItem;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Writes essential NBT ({@code Texture}/{@code Model}) to every {@link ItemStack}
 * as soon as it is created, without delays.
 *
 * <p>In 1.20.1 all public constructors delegate to the single base constructor
 * {@code (ItemLike, int, CompoundTag)}: we inject there only.</p>
 *
 * <p>{@link ItemStack#copy()} builds the copy and <em>then</em> overwrites its tag
 * with the original's tag; therefore we wrap {@code copy()} to suppress writing
 * in the internal constructor (the copy already inherits the tag).</p>
 *
 * <p>Note: the logical event {@link EItem#instantiatingEvent(ItemStack, Level)}
 * is called by {@link SlotInitializer#initializeFromConstructor(ItemStack)} (same
 * class). In the {@code ItemStack} constructor, no {@link net.minecraft.world.level.Level}
 * is available, so {@code null} is passed.</p>
 *
 * <environment_details>
 *     <env>CLIENT</env>
 *     <env>DEDICATED_SERVER</env>
 * </environment_details>
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/level/ItemLike;ILnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void pigeon_core$onConstruct(ItemLike item, int count, CompoundTag tag, CallbackInfo ci) {
        SlotInitializer.initializeFromConstructor((ItemStack) (Object) this);
    }

    @Inject(method = "copy", at = @At("HEAD"))
    private void pigeon_core$onCopyHead(CallbackInfoReturnable<ItemStack> cir) {
        SlotInitializer.enterCopy();
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void pigeon_core$onCopyReturn(CallbackInfoReturnable<ItemStack> cir) {
        SlotInitializer.exitCopy();
    }
}