package software.hacker_E303.pigeon_core.entity.animation.gear;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Runtime representation of a skeletal animation built from an {@link AnimationDefinition}.
 * <p>
 * Animations are sampled per frame and applied to {@link AnimatableParts} through
 * an {@link IAnimatableModel} during rendering.
 */
public final class Animation {
	
    private final float length;
    private final Map<String, Map<TransformType, List<KFA>>> boneMaps = new HashMap<>();

	private final Vec3f tmpRot = new Vec3f();
	private final Vec3f tmpPos = new Vec3f();
	private final Vec3f tmpScl = new Vec3f();

    /**
     * Parses an {@link AnimationDefinition} into an optimized runtime structure.
     *
     * @param def the animation definition to parse
     */
    public Animation(AnimationDefinition def) {
        this.length = def.lengthInSeconds();

        def.boneAnimations().forEach((partName, channels) -> {
            Map<TransformType, List<KFA>> targetMap = new EnumMap<>(TransformType.class);
                
            for (AnimationChannel channel : channels) {
                TransformType type = getTransformType(channel.target());
                
                if (type != null) {
                	List<KFA> kfas = new ArrayList<>();
                    for (Keyframe kf : channel.keyframes()) {
                    	var vec = kf.target();
                    	kfas.add(new KFA(kf.timestamp(), vec.x(), vec.y(), vec.z()));
                    }
                    targetMap.put(type, kfas);
                }
            }
            boneMaps.put(partName, targetMap);
        });
    }

    /**
     * Returns the total length of this animation in seconds.
     *
     * @return the animation length
     */
    public float getLength() {
    	return length;
    }

    /**
     * Samples and applies this animation to the given model at the specified time and weight.
     *
     * @param model the {@link IAnimatableModel} whose parts should be transformed
     * @param t the local time in seconds
     * @param weight the blend weight (0.0f to 1.0f)
     */
	public void play(IAnimatableModel model, float t, float weight) {

    	AnimatableParts parts = model.getAnimatableParts();
    	boneMaps.forEach((partName, channels) -> {

        	AnimatableParts.Entry entry = parts.getParts().get(partName);
        	if (entry == null) return;

        	ModelPart part = entry.part;
        	if (channels.containsKey(TransformType.ROTATION)) {
            	sampleVec(channels.get(TransformType.ROTATION), tmpRot, t, 0.0f);
            
            	part.xRot += smartDelta(entry.baseRx, entry.baseRx + tmpRot.x) * weight;
            	part.yRot += smartDelta(entry.baseRy, entry.baseRy + tmpRot.y) * weight;
            	part.zRot += smartDelta(entry.baseRz, entry.baseRz + tmpRot.z) * weight;
        	}

        	if (channels.containsKey(TransformType.POSITION)) {
            	sampleVec(channels.get(TransformType.POSITION), tmpPos, t, 0.0f);
            
            	part.x += tmpPos.x * weight;
            	part.y += tmpPos.y * weight;
            	part.z += tmpPos.z * weight;
        	}

			if (channels.containsKey(TransformType.SCALE)) {
				sampleVec(channels.get(TransformType.SCALE), tmpScl, t, 0.0f);

				float deltaX = Math.abs(tmpScl.x) > 0.5f && tmpScl.x != 0.0f ? tmpScl.x - 1.0f : tmpScl.x;
				float deltaY = Math.abs(tmpScl.y) > 0.5f && tmpScl.y != 0.0f ? tmpScl.y - 1.0f : tmpScl.y;
				float deltaZ = Math.abs(tmpScl.z) > 0.5f && tmpScl.z != 0.0f ? tmpScl.z - 1.0f : tmpScl.z;

				if (deltaX == -1.0f) deltaX = 0.0f;
				if (deltaY == -1.0f) deltaY = 0.0f;
				if (deltaZ == -1.0f) deltaZ = 0.0f;

				part.xScale = entry.baseSx + (deltaX * weight);
				part.yScale = entry.baseSy + (deltaY * weight);
				part.zScale = entry.baseSz + (deltaZ * weight);
			}
    	});
	}

    /**
     * Categorizes animation channel targets into transform operations.
     */
	public enum TransformType {
		POSITION, ROTATION, SCALE
	}

    /**
     * Maps a Minecraft {@link AnimationChannel.Target} to a {@link TransformType}.
     *
     * @param target the channel target
     * @return the matching transform type, or {@code null}
     */
    private TransformType getTransformType(AnimationChannel.Target target) {
    	
        if (target == AnimationChannel.Targets.ROTATION) return TransformType.ROTATION;
        if (target == AnimationChannel.Targets.POSITION) return TransformType.POSITION;
        if (target == AnimationChannel.Targets.SCALE)    return TransformType.SCALE;
        return null;
    }

    /**
     * A single keyframe value in an animation channel.
     */
    public static class KFA {

        float time;
        float x, y, z;

        /**
         * Creates a keyframe value.
         *
         * @param time the timestamp in seconds
         * @param x the X component
         * @param y the Y component
         * @param z the Z component
         */
        public KFA(float time, float x, float y, float z) {
            this.time = time;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /**
     * Temporary 3-component vector used during sampling.
     */
	public static class Vec3f {
		
    	float x, y, z;

    	/**
    	 * Sets all components of this vector.
    	 *
    	 * @param x the X value
    	 * @param y the Y value
    	 * @param z the Z value
    	 */
    	public void set(float x, float y, float z) {
    	    this.x = x;
    	    this.y = y;
    	    this.z = z;
    	}
	}

    /**
     * Samples a list of keyframes at the given time using linear interpolation.
     *
     * @param filtered the sorted keyframe list
     * @param out the output vector
     * @param t the time in seconds
     * @param value the fallback value if no keyframes are present
     */
	private void sampleVec(List<KFA> filtered, Vec3f out, float t, float value) {
		
    	if (filtered == null || filtered.isEmpty()) {
        	out.set(value, value, value);
        	return;
    	}
    	if (t <= filtered.get(0).time) {
    	    KFA k = filtered.get(0);
    	    out.set(k.x, k.y, k.z);
    	    return;
    	}
    	if (t >= filtered.get(filtered.size() - 1).time) {
        	KFA k = filtered.get(filtered.size() - 1);
        	out.set(k.x, k.y, k.z);
        	return;
    	}
    	for (int i = 0; i < filtered.size() - 1; i++) {
        	KFA a = filtered.get(i);
        	KFA b = filtered.get(i + 1);

        	if (t >= a.time && t <= b.time) {
        	    float span = b.time - a.time;
        	    float alpha = (span == 0) ? 0 : (t - a.time) / span;

        	    out.set(
        	        Mth.lerp(alpha, a.x, b.x),
        	        Mth.lerp(alpha, a.y, b.y),
        	        Mth.lerp(alpha, a.z, b.z)
        	    );
        	    return;
        	}
    	}
    	out.set(value, value, value);
	}

    /**
     * Computes the shortest angular delta from {@code current} to {@code target}.
     *
     * @param current the current angle in radians
     * @param target the target angle in radians
     * @return the normalized delta
     */
	private float smartDelta(float current, float target) {

    	float delta = target - current;
    	float PI = (float) Math.PI;

    	while (delta < -PI) delta += PI * 2;
    	while (delta >= PI) delta -= PI * 2;
    	return delta;
	}
}