package software.hacker_E303.pigeon_core.common.config;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public final class ConfigEntry<T> implements FolderItem {

    private final String     id;
    private final Class<?>   rawType;      // Boolean, Integer, Double, String, Item, List
    @Nullable
    private final Class<?>   elementType;  // for List<E>: E.class
    private final T          defaultValue;
    private T                value;
    @Nullable private final T min;
    @Nullable private final T max;

    ConfigEntry(String id, Class<?> rawType, @Nullable Class<?> elementType,
                T defaultValue, @Nullable T min, @Nullable T max) {
        this.id           = id;
        this.rawType      = rawType;
        this.elementType  = elementType;
        this.defaultValue = defaultValue;
        this.value        = defaultValue;
        this.min          = min;
        this.max          = max;
    }

    public String    id()          { return id; }
    public Class<?>  type()        { return rawType; }
    @Nullable
    public Class<?>  elementType() { return elementType; }
    public T         defaultValue(){ return defaultValue; }
    public T         value()       { return value; }
    @Nullable public T min()       { return min; }
    @Nullable public T max()       { return max; }

    public void set(T v)  { this.value = v; }
    public void reset()   { this.value = defaultValue; }

    /** Auto-derived display label for the lang template: {@code "max_turrets"} → {@code "Max Turrets"}. */
    public String defaultLabel() {
        return Arrays.stream(id.split("_")).filter(w -> !w.isEmpty())
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    @SuppressWarnings("unchecked")
    public void setRaw(Object raw) {
        if (raw == null) return;
        try { this.value = (T) raw; } catch (ClassCastException ignored) {}
    }
}
