package software.hacker_E303.pigeon_core.item.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import software.hacker_E303.pigeon_core.item.EItem;
import software.hacker_E303.pigeon_core.main.event.network.RouterUtils;
import software.hacker_E303.pigeon_core.util.locator.Location;

/**
 * Warns (via the same {@code CheckTexturePacket}/{@code ResourceWarningPacket}
 * round-trip {@link software.hacker_E303.pigeon_core.entity.EMob} uses for its
 * own texture) the first time a player receives an {@link EItem} of a given
 * pigeid whose {@code Texture} or {@code Model} points at a resource that
 * doesn't actually exist.
 *
 * <p>The check is per (player, pigeid), not per stack: picking up a stack of
 * 64 only triggers one round-trip. A missing {@code Model} is NOT reported
 * when its name equals the item's own pigeid — that's the framework's default
 * (see {@code SlotInitializer#writeEssentialData}), and simply falls back to
 * the vanilla generated icon when no matching {@code models/items/<pigeid>.json}
 * exists, which is expected, not a content bug.</p>
 */
public final class ItemResourceChecker {

    private ItemResourceChecker() {}

    private static final Map<UUID, Set<String>> CHECKED = new HashMap<>();

    /**
     * Runs the missing-resource check for {@code stack} against {@code player},
     * exactly once per (player, pigeid).
     *
     * @param player the player who just received the stack
     * @param stack  the stack (must wrap an {@link EItem})
     */
    public static void checkOnce(ServerPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof EItem eItem)) return;

        String pigeid = eItem.pigeid();
        Set<String> seen = CHECKED.computeIfAbsent(player.getUUID(), id -> new HashSet<>());
        if (!seen.add(pigeid)) return;

        String modid = eItem.modid();

        String textureName = eItem.getTexture(stack).replace("texture_", "");
        ResourceLocation textureFile = Location.create(eItem.getTexturePath(stack), textureName).from(modid);
        RouterUtils.Debug.ensureResource(player, textureFile, true);

        String modelName = eItem.getModel(stack);
        if (!"none".equals(modelName) && !modelName.equals(pigeid)) {
            String modelFileName = modelName.replace("model_", "");
            ResourceLocation modelFile = Location.create(eItem.getModelPath(stack), modelFileName).from(modid);
            RouterUtils.Debug.ensureResource(player, modelFile, true);
        }
    }
}
