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

    /**
     * Default / no-op path that resolves to an empty string.
     * <p>
     * Resolves to: {@code <empty>}
     */
    public static final Path NONE = new Path("", "", "");

    /**
     * Sound resource paths.
     * <p>
     * Note: sound paths carry no file suffix, so the resource name must
     * already include the extension (e.g. {@code click.ogg}).
     */
    public static class SOUND {
        private static final String BASE = "sounds/";

        /** Resolves to: {@code assets/<modid>/sounds/misc/<resource>} */
        public static final Path MISC      = new Path(BASE, "misc/", "");
        /** Resolves to: {@code assets/<modid>/sounds/guns/<resource>} */
        public static final Path GUNS      = new Path(BASE, "guns/", "");
        /** Resolves to: {@code assets/<modid>/sounds/turrets/<resource>} */
        public static final Path TURRETS   = new Path(BASE, "turrets/", "");
    }

    /**
     * Texture (image) resource paths.
     * <p>
     * All texture paths resolve to a {@code .png} file.
     */
    public static class TEXTURE {
        private static final String BASE = "textures/";
        private static final String SUFFIX = ".png";

        /** Resolves to: {@code assets/<modid>/textures/guis/<resource>.png} */
        public static final Path GUI       = new Path(BASE, "guis/",                 SUFFIX);
        /** Resolves to: {@code assets/<modid>/textures/misc/<resource>.png} */
        public static final Path MISC      = new Path(BASE, "misc/",                 SUFFIX);
        /** Resolves to: {@code assets/<modid>/textures/guns/texture_<resource>.png} */
        public static final Path GUNS      = new Path(BASE, "guns/texture_",         SUFFIX);
        /** Resolves to: {@code assets/<modid>/textures/items/<resource>.png} */
        public static final Path ITEMS     = new Path(BASE, "items/",                SUFFIX);
        /** Resolves to: {@code assets/<modid>/textures/blocks/<resource>.png} */
        public static final Path BLOCKS    = new Path(BASE, "blocks/",               SUFFIX);
        /** Resolves to: {@code assets/<modid>/textures/turrets/<resource>.png} */
        public static final Path TURRETS   = new Path(BASE, "turrets/",              SUFFIX);
        /** Resolves to: {@code assets/<modid>/textures/vehicles/<resource>.png} */
        public static final Path VEHICLES  = new Path(BASE, "vehicles/",             SUFFIX);
        /** Resolves to: {@code assets/<modid>/textures/entities/texture_<resource>.png} */
        public static final Path ENTITIES  = new Path(BASE, "entities/texture_",     SUFFIX);
        /** Resolves to: {@code assets/<modid>/textures/particles/<resource>.png} */
        public static final Path PARTICLES = new Path(BASE, "particles/",            SUFFIX);
    }

    /**
     * Vanilla model (JSON) resource paths.
     * <p>
     * All model paths resolve to a {@code .json} file.
     */
    public static class MODEL {
        private static final String BASE = "models/";
        private static final String SUFFIX = ".json";

        /** Resolves to: {@code assets/<modid>/models/items/<resource>.json} */
        public static final Path ITEMS     = new Path(BASE, "items/",                SUFFIX);
        /** Resolves to: {@code assets/<modid>/models/guis/<resource>.json} */
        public static final Path GUI       = new Path(BASE, "guis/",                 SUFFIX);
        /** Resolves to: {@code assets/<modid>/models/block/<resource>.json} */
        public static final Path BLOCKS    = new Path(BASE, "block/",                SUFFIX);
    }

    /**
     * GeckoLib animated model ({@code .geo.json}) resource paths.
     */
    public static class GEO_MODEL {
        private static final String BASE = "geo/";
        private static final String SUFFIX = ".geo.json";

        /** Resolves to: {@code assets/<modid>/geo/guns/<resource>.geo.json} */
        public static final Path GUNS      = new Path(BASE, "guns/",         SUFFIX);
        /** Resolves to: {@code assets/<modid>/geo/vehicles/<resource>.geo.json} */
        public static final Path VEHICLES  = new Path(BASE, "vehicles/",     SUFFIX);
        /** Resolves to: {@code assets/<modid>/geo/entities/<resource>.geo.json} */
        public static final Path ENTITIES  = new Path(BASE, "entities/",     SUFFIX);
    }

    /**
     * GeckoLib animation ({@code .animation.json}) resource paths.
     */
    public static class GEO_ANIMS {
        private static final String BASE = "animations/";
        private static final String SUFFIX = ".animation.json";

        /** Resolves to: {@code assets/<modid>/animations/guns/<resource>.animation.json} */
        public static final Path GUNS      = new Path(BASE, "guns/",         SUFFIX);
        /** Resolves to: {@code assets/<modid>/animations/vehicles/<resource>.animation.json} */
        public static final Path VEHICLES  = new Path(BASE, "vehicles/",     SUFFIX);
        /** Resolves to: {@code assets/<modid>/animations/entities/<resource>.animation.json} */
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