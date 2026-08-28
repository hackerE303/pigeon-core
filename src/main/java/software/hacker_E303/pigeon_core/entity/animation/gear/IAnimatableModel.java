package software.hacker_E303.pigeon_core.entity.animation.gear;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Provides the bridge between animation data and model rendering.
 * <p>
 * Implementations resolve named parts via {@link AnimatableParts} and
 * apply sampled animation transforms each frame.
 */
@OnlyIn(Dist.CLIENT)
public interface IAnimatableModel {

    /**
     * Returns the animatable parts registry for this model.
     *
     * @return the {@link AnimatableParts} instance
     */
    public AnimatableParts getAnimatableParts();

    /**
     * Advances all active animations and applies their transforms to the model parts.
     *
     * @param <T> the entity type
     * @param animable the entity to animate
     */
    default <T extends Entity & IAnimatableEntity<T>> void handleAnimations(T animable) {

    	for (int manager = 0; manager < animable.getAnimationManagers().count(); manager++) {
        	if (!animable.hasAnim(manager)) continue;

        	Animation anim = animable.getAnimations().get(animable.getAnim(manager));
        	if (anim == null) continue;

        	IAnimatableEntity.ClientState state = animable.getClientState(manager);
        	int serverKey = animable.getAnimKey(manager);
        	
        	if (state.serverKey != serverKey) {
        	    state.serverKey = serverKey;
        	    state.localElapsedMs = 0f;
        	    state.lastUpdateNs = System.nanoTime();
        	    state.lastSpeed = animable.getAnimSpeed(manager);
        	}
        	
        	long nowNs = System.nanoTime();
        	long deltaNs = nowNs - state.lastUpdateNs;
        	state.lastUpdateNs = nowNs;
        	
        	float deltaMs = deltaNs / 1_000_000f;
        	state.localElapsedMs += deltaMs * state.lastSpeed;
        	state.lastSpeed = animable.getAnimSpeed(manager);

        	float elapsedMs = state.localElapsedMs;
        	float durationMs = animable.getAnimDuration(manager) * 50f;
        	float transitionMs = animable.getAnimTransition(manager) * 50f;

        	float t = elapsedMs / durationMs;

        	if (t > 1.0f) {
        	    if (animable.isAnimLooped(manager)) t %= 1.0f;
        	    else t = 1.0f;
        	}
        	float weight = 1.0f;

        	if (elapsedMs < transitionMs)
        	    weight = elapsedMs / transitionMs;

        	else if (elapsedMs > (durationMs - transitionMs))
        	    weight = (durationMs - elapsedMs) / transitionMs;

        	weight = Mth.clamp(weight, 0f, 1f);
        	if (weight > 0.001f) anim.play(this, t * anim.getLength(), weight);
    	}
    }

    /**
     * Resets all registered model parts to their base transforms.
     */
    default void resetParts() {

    	AnimatableParts parts = this.getAnimatableParts();
    	if (parts == null) return;

    	parts.getParts().forEach((name, entry) -> {
        	ModelPart part = entry.part;

        	part.x = entry.basePx;
        	part.y = entry.basePy;
        	part.z = entry.basePz;

        	part.xRot = entry.baseRx;
        	part.yRot = entry.baseRy;
        	part.zRot = entry.baseRz;

        	part.xScale = entry.baseSx;
        	part.yScale = entry.baseSy;
        	part.zScale = entry.baseSz;
    	});
    }
}