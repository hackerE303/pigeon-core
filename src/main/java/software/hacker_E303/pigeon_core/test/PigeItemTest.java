package software.hacker_E303.pigeon_core.test;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.hacker_E303.pigeon_core.item.EItem;
import software.hacker_E303.pigeon_core.main.AutoRegister;
import software.hacker_E303.pigeon_core.util.world.PlayerUtils;

/**
 * Test item registered as {@code test_item}.
 */
@AutoRegister("test_item")
public class PigeItemTest extends EItem {

    /**
     * Handles item use events.
     *
     * @param stack the used item stack
     * @param level the level
     * @param player the using player
     * @return the interaction result
     */
    @Override
    public Interaction useEvent(ItemStack stack, Level level, Player player) {
        // Debug: on use, print the stack's current texture id via PlayerUtils.
        PlayerUtils.with(this.modid, player, ctx ->
            ctx.debugMessage("debug.test_item.model", this.getModel(stack)));
        //this.setTexture(stack, "advanced_iron");
        this.setModel(stack, "test");
        return Interaction.SUCCESS;
    }
}