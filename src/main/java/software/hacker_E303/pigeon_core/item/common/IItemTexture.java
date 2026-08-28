package software.hacker_E303.pigeon_core.item.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Allows an item to declare, per-stack, which texture to display.
 */
public interface IItemTexture {
    
    String getTexture(ItemStack stack);
    void setTexture(ItemStack stack, String name);

    Path getTexturePath(ItemStack stack);
    ResourceLocation getTextureLocation(ItemStack stack);
}