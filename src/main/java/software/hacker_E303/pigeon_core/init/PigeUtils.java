package software.hacker_E303.pigeon_core.init;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import software.hacker_E303.pigeon_core.PigeonCore;
import software.hacker_E303.pigeon_core.main.AutoRegister;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Shared utility methods for mod-id resolution, mod-installation checks,
 * and runtime resource-warning messages.
 */
public final class PigeUtils {

    private static final ResourceLocation UNREGISTERED_KEY = Location.create(Path.NONE, "air").from("minecraft");

    /**
     * Resolves the mod id from an {@link Item} registry key.
     *
     * @param item the item to inspect
     * @return the item's registry namespace, or this mod's id if unregistered
     */
    private static String modidFromItem(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (isUnregistered(key)) return PigeonCore.getModid();
        return key.getNamespace();
    }

    /**
     * Resolves the mod id from a {@link Block} registry key.
     *
     * @param block the block to inspect
     * @return the block's registry namespace, or this mod's id if unregistered
     */
    private static String modidFromBlock(Block block) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        if (isUnregistered(key)) return PigeonCore.getModid();
        return key.getNamespace();
    }

    /**
     * Resolves the mod id from a {@link SoundEvent} registry key.
     *
     * @param sound the sound event to inspect
     * @return the sound's registry namespace
     */
    private static String modidFromSound(SoundEvent sound) {
        return ForgeRegistries.SOUND_EVENTS.getKey(sound).getNamespace();
    }

    /**
     * Resolves the mod id from an {@link EntityType} registry key.
     *
     * @param type the entity type to inspect
     * @return the entity type's registry namespace, or this mod's id if unregistered
     */
    private static String modidFromEntityType(EntityType<?> type) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (isUnregistered(key)) return PigeonCore.getModid();
        return key.getNamespace();
    }

    /**
     * Checks whether a registry key is {@code null} or the built-in {@code minecraft:air}
     * placeholder used for unregistered entries.
     *
     * @param key the registry key to test
     * @return {@code true} if the key is missing or the air placeholder
     */
    private static boolean isUnregistered(ResourceLocation key) {
        return key == null || key.equals(UNREGISTERED_KEY);
    }

    /**
     * Resolves the mod id from a heterogeneous object.
     *
     * <p>Supported types:</p>
     * <ul>
     *   <li>{@link String} — returned as-is</li>
     *   <li>{@link Item} / {@link ItemStack} — resolved via {@link #modidFromItem(Item)}</li>
     *   <li>{@link Block} — resolved via {@link #modidFromBlock(Block)}</li>
     *   <li>{@link SoundEvent} — resolved via {@link #modidFromSound(SoundEvent)}</li>
     *   <li>{@link Entity} / {@link EntityType} — resolved via {@link #modidFromEntityType(EntityType)}</li>
     * </ul>
     *
     * @param obj the object to inspect; may be {@code null}
     * @return the resolved mod id, or {@code null} if {@code obj} is {@code null}
     */
    public static String modidFrom(Object obj) {

        if (obj == null) return null;
        String modId = "";

        if (obj instanceof String string)
            modId = string;
        
        else if (obj instanceof Item item)
            modId = modidFromItem(item);

        else if (obj instanceof ItemStack stack)
            modId = modidFromItem(stack.getItem());

        else if (obj instanceof Block block)
            modId = modidFromBlock(block);

        else if (obj instanceof SoundEvent sound)
            modId = modidFromSound(sound);

        else if (obj instanceof Entity entity)
            modId = modidFromEntityType(entity.getType());

        else if (obj instanceof EntityType type)
            modId = modidFromEntityType(type);

        return modId;
    }

    /**
     * Checks whether a mod is loaded in the current runtime.
     *
     * @param modId the mod id to check
     * @return {@code true} if the mod is present in {@link ModList}
     */
    public static boolean isInstalledMod(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /**
     * Resolves the pigeon id from a class or instance annotated with {@link AutoRegister}.
     *
     * @param obj the class or instance to inspect
     * @return the {@code @AutoRegister} value, or {@code "none"} if not annotated
     */
    public static String pigeidFrom(Object obj) {

        Class<?> clazz;
        if (obj instanceof Class<?> base) clazz = base;
        else clazz = obj.getClass();

        if (clazz == null) return "null";

        AutoRegister ann = clazz.getAnnotation(AutoRegister.class);
        if (ann != null) return ann.value();

        return "none";
    }

    /**
     * Sends a missing-resource warning to a specific player.
     *
     * @param player   the player who should receive the message
     * @param location the missing resource location
     */
    public static void missingResourceWarning(Player player, ResourceLocation location) {
        player.sendSystemMessage(textureWarning(location));
    }

    /**
     * Broadcasts a missing-resource warning to all players on the server.
     *
     * @param level    the server level
     * @param location the missing resource location
     */
    public static void missingResourceWarning(Level level, ResourceLocation location) {

        if (level.isClientSide()) return;
        level.getServer().getPlayerList().broadcastSystemMessage(textureWarning(location), false);
    }

    /**
     * Builds the chat component used for missing-texture warnings.
     *
     * @param location the missing texture resource location
     * @return a formatted warning component
     */
    private static MutableComponent textureWarning(ResourceLocation location) {

        String path = location.toString();
        String obj  = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.')) + ".png";

        return Component.translatable("§e[Pigeon Core] ")
            .append(Component.translatable("debug.pigeon_core.errors.missing_texture", "§c" + obj, "§c" + path.replace(obj, "")).withStyle(ChatFormatting.GRAY));
    }
}