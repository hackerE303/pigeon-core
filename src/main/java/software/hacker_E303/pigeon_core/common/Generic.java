package software.hacker_E303.pigeon_core.common;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Typed key-value container used to pass arbitrary registration data.
 */
@SuppressWarnings("unchecked")
public final class Generic {

    private final Map<String, Object> objects = new HashMap<>();

    private Generic() {
    }

    public <T> T get(String object, Class<T> result) {
        return (T) this.objects.getOrDefault(object, null);
    }

    public <T> T getOrDefault(String object, T defautValue) {
        return (T) this.objects.getOrDefault(object, defautValue);
    }

    public void forEach(Consumer<Object> action) {
        objects.forEach((name, object) -> action.accept(object));
    }

    public <T> void forEach(Class<T> type, Consumer<T> action) {
        objects.values().stream()
            .filter(type::isInstance)
            .map(type::cast)
            .forEach(action);
    }
    
    public static Generic create(Consumer<GenericContext> action) {

        Generic generic = new Generic();
        GenericContext ctx = new GenericContext(generic);
        action.accept(ctx);
        return generic;
    }

    public static final class GenericContext {

        private final Generic generic;

        private GenericContext(Generic generic) {
            this.generic = generic;
        }

        public GenericContext add(String key, Object object) {
            this.generic.objects.put(key, object);
            return this;
        }
    }
}