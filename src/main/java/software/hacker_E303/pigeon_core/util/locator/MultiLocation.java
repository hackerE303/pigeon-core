package software.hacker_E303.pigeon_core.util.locator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Groups multiple resource names under a single {@link Path} and lazily
 * resolves each to a {@link Location}.
 */
public final class MultiLocation {

    private final Path path;
    private final List<String> resources = new ArrayList<>();

    /**
     * Creates a new {@link MultiLocation} bound to the given path and resources.
     *
     * @param path      the shared base path
     * @param resources the initial resource names
     */
    private MultiLocation(Path path, String... resources) {
        this.path = path;
        if (resources.length > 0) {
            this.resources.addAll(Arrays.asList(resources));
        }
    }

    /**
     * Builds a {@link Location} for the given resource under this path.
     *
     * @param resource the resource name
     * @return the resolved {@link Location}
     */
    public Location get(String resource) {
        return Location.create(this.path, resource);
    }

    /**
     * Adds a resource name to this group.
     *
     * @param resource the resource name to add
     * @return this {@link MultiLocation} for chaining
     */
    public MultiLocation put(String resource) {
        this.resources.add(resource);
        return this;
    }

    /**
     * Removes a resource name from this group.
     *
     * @param resource the resource name to remove
     * @return this {@link MultiLocation} for chaining
     */
    public MultiLocation remove(String resource) {
        this.resources.remove(resource);
        return this;
    }

    /**
     * Iterates over every resource in this group, applying the given action.
     *
     * @param action the consumer to invoke for each resolved {@link Location}
     */
    public void forEach(Consumer<Location> action) {
        
        for (String resource : this.resources) {
            action.accept(Location.create(this.path, resource));
        }
    }

    /**
     * Creates a new {@link MultiLocation} from the given path and resources.
     *
     * @param path      the shared base path
     * @param resources the resource names to include
     * @return the new {@link MultiLocation} instance
     */
    public static MultiLocation create(Path path, String... resources) {
        return new MultiLocation(path, resources);
    }
}