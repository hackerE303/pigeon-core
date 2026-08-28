package software.hacker_E303.pigeon_core.entity.animation.gear;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side registry of model parts that can be animated.
 * <p>
 * Each part is stored with its base transform so animations can
 * sample relative deltas without losing the original pose.
 */
@OnlyIn(Dist.CLIENT)
public final class AnimatableParts {
    
    private final Map<String, Entry> parts = new HashMap<>();

    /**
     * Registers a model part under the given name and returns this registry for chaining.
     *
     * @param name the part name used in animation definitions
     * @param part the {@link ModelPart} to register
     * @return this {@code AnimatableParts} instance
     */
    public AnimatableParts register(String name, ModelPart part) {
        parts.put(name, new Entry(part));
        return this;
    }
    
    /**
     * Returns all registered parts keyed by their animation name.
     *
     * @return an unmodifiable view of the parts map
     */
    public Map<String, Entry> getParts() {
        return parts;
    }

    /**
     * Holds a {@link ModelPart} together with its base transform values.
     */
    public static class Entry {
        
        public final ModelPart part;

        public final float basePx, basePy, basePz;
        public final float baseRx, baseRy, baseRz;
        public final float baseSx, baseSy, baseSz;

        /**
         * Captures the current transform of the given part as the base pose.
         *
         * @param part the model part to snapshot
         */
        public Entry(ModelPart part) {
            this.part = part;

            this.basePx = part.x;
            this.basePy = part.y;
            this.basePz = part.z;

            this.baseRx = part.xRot;
            this.baseRy = part.yRot;
            this.baseRz = part.zRot;
    
            this.baseSx = part.xScale;
            this.baseSy = part.yScale;
            this.baseSz = part.zScale;
        }
    }
}