package software.hacker_E303.pigeon_core.util;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Convenience accessors for typed NBT data and Forge capabilities on
 * {@link ItemStack} and {@link Entity} instances.
 */
@SuppressWarnings({"UnnecessaryBoxing", "unchecked", "null"})
public class BetterData {

    /**
     * Reads typed data from an {@link ItemStack} using the default value to infer the type.
     *
     * @param stack        the item stack to query
     * @param key          the NBT key
     * @param defaultValue the default value returned if the key is missing
     * @param <V>          the value type
     * @return the stored value, or {@code defaultValue}
     */
    public static <V> V getData(ItemStack stack, String key, @Nonnull V defaultValue) {
        return getDataValue(getProvider(stack), key, defaultValue);
    }

    /**
     * Writes typed data to an {@link ItemStack}.
     *
     * @param stack the item stack to update
     * @param key   the NBT key
     * @param value the value to store
     */
    public static void setData(ItemStack stack, String key, @Nonnull Object value) {   

        setDataValue(getProvider(stack), key, value);
    }

    /**
     * Removes a key from an {@link ItemStack}'s persistent NBT.
     *
     * @param stack the item stack
     * @param key   the NBT key to remove
     */
    public static void removeData(ItemStack stack, String key) {
        getProvider(stack).remove(key);
    }

    /**
     * Reads typed data from an {@link Entity} using the default value to infer the type.
     *
     * @param entity       the entity to query
     * @param key          the NBT key
     * @param defaultValue the default value returned if the key is missing
     * @param <V>          the value type
     * @return the stored value, or {@code defaultValue}
     */
    public static <V> V getData(Entity entity, String key, @Nonnull V defaultValue) {
        return getDataValue(getProvider(entity), key, defaultValue);
    }

    /**
     * Writes typed data to an {@link Entity}'s persistent NBT.
     *
     * @param entity the entity to update
     * @param key    the NBT key
     * @param value  the value to store
     */
    public static void setData(Entity entity, String key, @Nonnull Object value) {   

        setDataValue(getProvider(entity), key, value);
    }

    /**
     * Removes a key from an {@link Entity}'s persistent NBT.
     *
     * @param entity the entity
     * @param key    the NBT key to remove
     */
    public static void removeData(Entity entity, String key) {
        getProvider(entity).remove(key);
    }

    /**
     * Writes a value to a {@link CompoundTag} using the value's runtime type to
     * choose the correct NBT method.
     *
     * @param nbt   the target tag
     * @param key   the key to write
     * @param value the value to store
     * @return {@code true} if the value type was handled
     */
    private static boolean setDataValue(CompoundTag nbt, String key, Object value) {

        if (value instanceof Integer v) {
            nbt.putInt(key, v);
            return true;
        }
        if (value instanceof Double v) {
            nbt.putDouble(key, v);
            return true;
        }
        if (value instanceof Float v) {
            nbt.putFloat(key, v);
            return true;
        }
        if (value instanceof Long v) {
            nbt.putLong(key, v);
            return true;
        }
        if (value instanceof Short v) {
            nbt.putShort(key, v);
            return true;
        }
        if (value instanceof Boolean v) {
            nbt.putBoolean(key, v);
            return true;
        }
        if (value instanceof String v) {
            nbt.putString(key, v);
            return true;
        }
        if (value instanceof Tag v) {
            nbt.put(key, v);
            return true;
        }
        return false;
    }

    /**
     * Reads a value from a {@link CompoundTag} using the default value's type to
     * infer the correct getter.
     *
     * @param nbt           the source tag
     * @param key           the key to read
     * @param defaultValue  the default value used to determine the type
     * @param <V>           the value type
     * @return the stored value, or {@code defaultValue} if missing
     */
    public static <V> V getDataValue(CompoundTag nbt, String key, V defaultValue) {
    
        if (!nbt.contains(key)) return defaultValue;

        if (defaultValue instanceof Boolean)
            return (V) Boolean.valueOf(nbt.getBoolean(key));

        if (defaultValue instanceof Integer)
            return (V) Integer.valueOf(nbt.getInt(key));

        if (defaultValue instanceof Double)
            return (V) Double.valueOf(nbt.getDouble(key));

        if (defaultValue instanceof Float)
            return (V) Float.valueOf(nbt.getFloat(key));

        if (defaultValue instanceof Long)
            return (V) Long.valueOf(nbt.getLong(key));

        if (defaultValue instanceof Short)
            return (V) Short.valueOf(nbt.getShort(key));

        if (defaultValue instanceof String)
            return (V) nbt.getString(key);

        if (defaultValue instanceof Tag)
            return (V) nbt.get(key);

        return defaultValue;
    }

    /**
     * Executes an action against a capability on an {@link ItemStack}, if present.
     *
     * @param <C>    the capability type
     * @param cap    the capability to query
     * @param stack  the item stack
     * @param action the action to perform on the capability instance
     */
    public static <C> void setCap(Capability<C> cap, ItemStack stack, Consumer<C> action) {
        C instance = stack.getCapability(cap).orElse(null);
        if (instance != null) action.accept(instance);
    }

    /**
     * Reads a value from a capability on an {@link ItemStack}, if present.
     *
     * @param <C>      the capability type
     * @param <V>      the return type
     * @param cap      the capability to query
     * @param stack    the item stack
     * @param function the mapping function to apply
     * @return the mapped value, or {@code null} if the capability is absent
     */
    public static <C, V> V getCap(Capability<C> cap, ItemStack stack, Function<C, V> function) {
        C instance = stack.getCapability(cap).orElse(null);
        if (instance != null) return function.apply(instance);
        return null;
    }

    /**
     * Executes an action against a capability on an {@link Entity}, if present.
     *
     * @param <C>    the capability type
     * @param cap    the capability to query
     * @param entity the entity
     * @param action the action to perform on the capability instance
     */
    public static <C> void setCap(Capability<C> cap, Entity entity, Consumer<C> action) {
        C instance = entity.getCapability(cap).orElse(null);
        if (instance != null) action.accept(instance);
    }

    /**
     * Reads a value from a capability on an {@link Entity}, if present.
     *
     * @param <C>      the capability type
     * @param <V>      the return type
     * @param cap      the capability to query
     * @param entity   the entity
     * @param function the mapping function to apply
     * @return the mapped value, or {@code null} if the capability is absent
     */
    public static <C, V> V getCap(Capability<C> cap, Entity entity, Function<C, V> function) {
        C instance = entity.getCapability(cap).orElse(null);
        if (instance != null) return function.apply(instance);
        return null;
    }

    /**
     * Checks whether an {@link ItemStack} contains the given NBT key.
     *
     * @param stack the item stack
     * @param key   the NBT key
     * @return {@code true} if the key exists
     */
    public static boolean hasData(ItemStack stack, String key) {

        CompoundTag tag = getProvider(stack);
        return tag.contains(key);
    }

    /**
     * Checks whether an {@link Entity} contains the given NBT key.
     *
     * @param entity the entity
     * @param key    the NBT key
     * @return {@code true} if the key exists
     */
    public static boolean hasData(Entity entity, String key) {

        CompoundTag tag = getProvider(entity);
        return tag.contains(key);
    }

    /**
     * Returns the persistent {@link CompoundTag} provider for an {@link ItemStack}.
     *
     * @param stack the item stack
     * @return the item's tag, creating it if necessary
     */
    public static CompoundTag getProvider(ItemStack stack) {
        return stack.getOrCreateTag();
    }

    /**
     * Returns the persistent {@link CompoundTag} provider for an {@link Entity}.
     *
     * @param entity the entity
     * @return the entity's persistent data tag
     */
    public static CompoundTag getProvider(Entity entity) {
        return entity.getPersistentData();
    }
}