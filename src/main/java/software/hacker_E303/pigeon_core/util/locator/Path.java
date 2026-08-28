package software.hacker_E303.pigeon_core.util.locator;

/**
 * Immutable resource path holder used to construct {@link Location} and
 * {@link MultiLocation} instances for sounds, textures, models, and animations.
 */
public class Path {

    protected final String path0;
    protected final String path1;
    protected final String suffix;

    private Path(String path0, String path1, String suffix) {
        this.path0  = path0;
        this.path1  = path1;
        this.suffix = suffix;
    }

    public static final Path NONE = new Path("", "", "");

    public static class SOUND {
        private static final String BASE = "sounds/";

        public static final Path MISC      = new Path(BASE, "misc/", "");
        public static final Path GUNS      = new Path(BASE, "guns/", "");
        public static final Path TURRETS   = new Path(BASE, "turrets/", "");
    }   

    public static class TEXTURE {
        private static final String BASE = "textures/";
        private static final String SUFFIX = ".png";

        public static final Path GUI       = new Path(BASE, "guis/",                 SUFFIX);
        public static final Path MISC      = new Path(BASE, "misc/",                 SUFFIX);
        public static final Path GUNS      = new Path(BASE, "guns/texture_",         SUFFIX);
        public static final Path ITEMS     = new Path(BASE, "items/",                SUFFIX);
        public static final Path BLOCKS    = new Path(BASE, "blocks/",               SUFFIX);
        public static final Path TURRETS   = new Path(BASE, "turrets/",              SUFFIX);
        public static final Path VEHICLES  = new Path(BASE, "vehicles/",             SUFFIX);
        public static final Path ENTITIES  = new Path(BASE, "entities/texture_",     SUFFIX);
        public static final Path PARTICLES = new Path(BASE, "particles/",            SUFFIX);
    }

    public static class MODEL {
        private static final String BASE = "models/";
        private static final String SUFFIX = ".json";

        public static final Path ITEMS     = new Path(BASE, "items/",                SUFFIX);
        public static final Path GUI       = new Path(BASE, "guis/",                 SUFFIX);
        public static final Path BLOCKS    = new Path(BASE, "block/",                SUFFIX);
    }

    public static class GEO_MODEL {
        private static final String BASE = "geo/";
        private static final String SUFFIX = ".geo.json";

        public static final Path GUNS      = new Path(BASE, "guns/",         SUFFIX);
        public static final Path VEHICLES  = new Path(BASE, "vehicles/",     SUFFIX);
        public static final Path ENTITIES  = new Path(BASE, "entities/",     SUFFIX);
    }

    public static class GEO_ANIMS {
        private static final String BASE = "animations/";
        private static final String SUFFIX = ".animation.json";

        public static final Path GUNS      = new Path(BASE, "guns/",         SUFFIX);
        public static final Path VEHICLES  = new Path(BASE, "vehicles/",     SUFFIX);
        public static final Path ENTITIES  = new Path(BASE, "entities/",     SUFFIX);
    }

    /**
     * Creates a custom {@link Path} with an empty base prefix.
     *
     * @param path   the relative path segment
     * @param suffix the file suffix
     * @return a new {@link Path} instance
     */
    public static Path create(String path, String suffix) {
        return new Path("", path, suffix);
    }

    /**
     * Returns the concatenated base and relative path segments (no suffix).
     *
     * @return the combined directory path
     */
    @Override
    public String toString() {
        return path0 + path1;
    }
}