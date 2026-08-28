package software.hacker_E303.pigeon_core.common.gui;

/**
 * Controls how an entity or item is displayed inside a GUI element.
 * Created fresh each render frame — configure it inside the lambda passed to
 * {@link GuiContext#insertEntity} / {@link GuiContext#insertItem}.
 */
public final class DisplayAction {

    public enum Axis { X, Y, Z }

    private float   spinSpeed        = 0f;  // degrees per second, Y axis
    private boolean catchesMouse     = false;
    private float   mouseDelayFactor = 1f;  // 1.0 = instant, 0.0 = no movement
    private float   rotX = 0f, rotY = 0f, rotZ = 0f; // fixed offsets
    private boolean glint            = false;

    /**
     * Spins the entity/item continuously around the Y axis.
     * @param degreesPerSecond rotation speed — negative values invert the direction
     */
    public void spin(float degreesPerSecond) {
        this.spinSpeed = degreesPerSecond;
    }

    /**
     * Makes the entity/item track the mouse cursor.
     * @param delayFactor smoothing — {@code 1.0f} = smoother/slower, lower = instant follow
     */
    public void catchMouse(float delayFactor) {
        this.catchesMouse     = true;
        this.mouseDelayFactor = 1.0f - delayFactor;
    }

    /**
     * Applies a fixed rotation around the given axis.
     * Stacks with {@link #spin} (spin is applied on top of fixed offsets).
     * Calling this method again for the same axis replaces the previous value.
     * @param axis    rotation axis
     * @param degrees angle in degrees
     */
    public void rotate(Axis axis, float degrees) {
        switch (axis) {
            case X -> rotX = degrees;
            case Y -> rotY = degrees;
            case Z -> rotZ = degrees;
        }
    }

    /**
     * Adds the enchantment glint overlay to item displays.
     * Has no effect on entity displays.
     */
    public void glint() {
        this.glint = true;
    }

    // ── Getters (used by the renderer) ────────────────────────────────────────

    public static class Data {

        private final DisplayAction action;

        private Data(DisplayAction action) {
            this.action = action;
        }

        public float   spinSpeed()         { return action.spinSpeed;        }
        public boolean catchMouse()        { return action.catchesMouse;     }
        public float   mouseDelayFactor()  { return action.mouseDelayFactor; }
        public float   rotX()              { return action.rotX;             }
        public float   rotY()              { return action.rotY;             }
        public float   rotZ()              { return action.rotZ;             }
        public boolean glint()             { return action.glint;            }
    }

    public Data data() {
        return new Data(this);
    }

    /** Resets all fields to their defaults so a single instance can be reused per frame. */
    public void reset() {
        this.spinSpeed        = 0f;
        this.catchesMouse     = false;
        this.mouseDelayFactor = 1f;
        this.rotX = 0f; this.rotY = 0f; this.rotZ = 0f;
        this.glint            = false;
    }
}