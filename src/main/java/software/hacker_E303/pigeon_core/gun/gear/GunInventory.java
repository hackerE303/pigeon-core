package software.hacker_E303.pigeon_core.gun.gear;

import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import software.hacker_E303.pigeon_core.geo.item.gun.EAttachment;
import software.hacker_E303.pigeon_core.geo.item.gun.EGun;
import software.hacker_E303.pigeon_core.util.BetterData;

/**
 * Item stack handler for gun attachments.
 */
public class GunInventory extends ItemStackHandler {

    private final ItemStack gunStack;

    /**
     * Creates a new gun inventory and loads persisted attachments.
     * 
     * @param stack the gun item stack
     */
    public GunInventory(ItemStack stack) {

        super(EAttachment.Type.values().length);
        this.gunStack = stack;

        if (stack == null) return;
        CompoundTag nbt = stack.getTag();
        if (nbt != null && nbt.contains("gun_attachments_" + EGun.ATTACHMENTS, Tag.TAG_COMPOUND))
            this.deserializeNBT(nbt.getCompound("gun_attachments_" + EGun.ATTACHMENTS));
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {

        if (stack.getItem() instanceof EAttachment attachment) {

            EAttachment.Type slotType = EAttachment.Type.values()[slot];
            return attachment.getType() == slotType;
        }
        return false;
    }

    @Override
    protected void onContentsChanged(int slot) {

        CompoundTag nbt = BetterData.getProvider(gunStack);
        nbt.put("gun_attachments_" + EGun.ATTACHMENTS, this.serializeNBT());
    }
}