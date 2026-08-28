package software.hacker_E303.pigeon_core.common.gui;

/**
 * Screen-relative position (and optional size or scale) of a GUI element.
 * Coordinates are relative to the GUI panel origin (leftPos / topPos).
 */
public final class LayoutBounds {

    private final int     x, y, width, height;
    private final boolean hasSize;
    private final float   scale; // used by insertEntity / insertItem; ignored elsewhere

    private LayoutBounds(int x, int y, int width, int height, boolean hasSize, float scale) {
        this.x       = x;
        this.y       = y;
        this.width   = width;
        this.height  = height;
        this.hasSize = hasSize;
        this.scale   = scale;
    }

    /**
     * Process-wide intern pool.
     * GUI render interfaces build many {@link LayoutBounds} every frame with the
     * same constant coordinates (e.g. {@code LayoutBounds.create(0, 0)}). Interning
     * returns a shared immutable instance for identical parameters so those
     * allocations are effectively eliminated across frames.
     */
    private static final int                          POOL_MASK = 0x3FF; // 1024 buckets
    private static final LayoutBounds[]               POOL      = new LayoutBounds[POOL_MASK + 1];

    private static LayoutBounds intern(int x, int y, int width, int height, boolean hasSize, float scale) {
        int  idx = Math.floorMod((x * 31 + y * 131 + width * 17 + height * 7
                + (hasSize ? 1 : 0) * 8191 + Float.floatToRawIntBits(scale)), POOL_MASK + 1);
        LayoutBounds existing = POOL[idx];
        if (existing != null && existing.x == x && existing.y == y && existing.width == width
                && existing.height == height && existing.hasSize == hasSize
                && existing.scale == scale) {
            return existing;
        }
        LayoutBounds created = new LayoutBounds(x, y, width, height, hasSize, scale);
        POOL[idx] = created;
        return created;
    }

    /** Position only — the element uses its default size. */
    public static LayoutBounds create(int x, int y) {
        return intern(x, y, 0, 0, false, 1.0f);
    }

    /** Position and explicit size. */
    public static LayoutBounds create(int x, int y, int width, int height) {
        return intern(x, y, width, height, true, 1.0f);
    }

    /**
     * Position and render scale — intended for {@code insertEntity} / {@code insertItem}.
     * <p>
     * For entities the scale is in pixels-per-block (e.g. {@code 30f} = a 1-block-tall
     * entity rendered 30 px tall). For items it is a multiplier on the standard 16 × 16
     * icon (e.g. {@code 2.0f} renders the item at 32 × 32).
     */
    public static LayoutBounds create(int x, int y, float scale) {
        return intern(x, y, 0, 0, false, scale);
    }

    public int     getX()      { return x;       }
    public int     getY()      { return y;       }
    public int     getWidth()  { return width;   }
    public int     getHeight() { return height;  }
    public boolean hasSize()   { return hasSize; }
    public float   getScale()  { return scale;   }
}
