package software.hacker_E303.pigeon_core.util.locator;

import net.minecraft.resources.ResourceLocation;

/**
 * Resolves a full resource location string from a {@link Path} and resource name.
 */
public final class Location {

    private final Path path;
    private final String resource;

    /**
     * Creates a new {@link Location}.
     *
     * @param path   the base {@link Path}
     * @param obj    the resource name
     */
    private Location(Path path, String obj) {
        this.path = path;
        this.resource  = obj;
    }

    /**
     * Returns the directory portion of this location (base + relative path).
     *
     * @return the combined directory path
     */
    public String getPath() {
        return this.path.path0 + this.path.path1;
    }

    /**
     * Returns the resource object name without path or suffix.
     *
     * @return the resource name
     */
    public String getObject() {
        return this.resource;
    }

    /**
     * Returns the file suffix from the underlying {@link Path}.
     *
     * @return the suffix string
     */
    public String getSuffix() {
        return this.path.suffix;
    }

    /**
     * Returns the full resource location string.
     *
     * @return the concatenated path, resource, and suffix
     */
    @Override
    public String toString() {
        return this.path.path0 + this.path.path1 + this.resource + this.path.suffix;
    }

    /**
     * Converts this location into a {@link ResourceLocation} for the given mod id.
     *
     * @param modid the namespace to use
     * @return a new {@link ResourceLocation}
     */
    public ResourceLocation from(String modid) {
        return new ResourceLocation(modid, this.toString());
    }

    /**
     * Creates a new {@link Location} from a {@link Path} and resource name.
     *
     * @param path     the base path
     * @param resource the resource name
     * @return the new {@link Location}
     */
    public static Location create(Path path, String resource) {
        return new Location(path, resource);
    }
}