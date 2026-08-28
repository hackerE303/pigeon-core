package software.hacker_E303.pigeon_core.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * Magazine-style item that displays current ammo in its tooltip and uses a fixed durability bar.
 */
public abstract class EMagazine extends EItem {

    public EMagazine(Rarity rarity, int durability) {
        super(Rarity.COMMON, durability);
    }

    @Override
    public void addTooltip(ItemStack stack, List<Component> tooltip) {

        tooltip.add(Component.literal("Current Ammo: " + 
            (stack.getMaxDamage() - stack.getDamageValue())).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public final int getBarColor(ItemStack stack) {
        return 0xFFFF00;
    }

    @Override
    public final boolean isEnchantable(ItemStack stack) {
        return false;
    }
}