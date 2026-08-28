package software.hacker_E303.pigeon_core.item.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Parallel to {@link IItemTexture} but for the item model instead of its texture.
 * Lets an item choose, per-stack, which model JSON to use (or resolve it at runtime),
 * so the framework can bypass the static {@code models/item/<id>.json} and drive the
 * model from code (e.g. swapping between a JSON model and a Java/built-in one, or
 * picking the model per stack).
 */
public interface IItemModel {

    String getModel(ItemStack stack);
    void setModel(ItemStack stack, String name);

    Path getModelPath(ItemStack stack);
    ResourceLocation getModelLocation(ItemStack stack);
}
