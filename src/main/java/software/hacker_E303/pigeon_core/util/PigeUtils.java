package software.hacker_E303.pigeon_core.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class PigeUtils {

    private static final String SPAWN_EGG_SUFFIX = "_spawn_egg";

    private PigeUtils() {}

    /**
     * Derives the framework item-atlas sprite id ({@code <modid>:items/<pigeid>})
     * for a spawn egg, where {@code pigeid} is the egg's registry id with any
     * trailing {@code _spawn_egg} stripped. Turret eggs (id already ends in
     * {@code turret}) and mob eggs ({@code <id>_spawn_egg}) both resolve
     * correctly. Returns null when the egg is not registered.
     */
    public static ResourceLocation getEggSpriteId(ItemStack eggStack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(eggStack.getItem());
        if (id == null) return null;

        String pigeid = id.getPath();
        if (pigeid.endsWith(SPAWN_EGG_SUFFIX))
            pigeid = pigeid.substring(0, pigeid.length() - SPAWN_EGG_SUFFIX.length());

        return new ResourceLocation(id.getNamespace(), "items/" + pigeid);
    }

    /**
     * Returns the framework pigeid (entity id with any trailing {@code _spawn_egg}
     * stripped) for a spawn egg, or null if the item is not registered.
     */
    public static String getEggPigeid(ItemStack eggStack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(eggStack.getItem());
        if (id == null) return null;

        String pigeid = id.getPath();
        if (pigeid.endsWith(SPAWN_EGG_SUFFIX))
            pigeid = pigeid.substring(0, pigeid.length() - SPAWN_EGG_SUFFIX.length());

        return pigeid;
    }
}
