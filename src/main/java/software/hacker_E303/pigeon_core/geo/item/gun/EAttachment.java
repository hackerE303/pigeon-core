package software.hacker_E303.pigeon_core.geo.item.gun;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.hacker_E303.pigeon_core.item.EItem;

/**
 * Base class for gun attachments.
 */
public abstract class EAttachment extends EItem implements GeoAnimatable {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, true);

    /**
     * Creates a new gun attachment.
     */
    public EAttachment() {
        super(1);
    }

    /**
     * @return the unique attachment identifier
     */
    public abstract String getId();

    /**
     * @return the attachment slot type
     */
    public abstract Type getType();

    /**
     * @return the stat modifier applied by this attachment
     */
    public abstract Number getModifier();

    /**
     * Registers the idle animation controller.
     */
    @Override
    public void registerControllers(ControllerRegistrar data) {
        AnimationController<EAttachment> main = new AnimationController<>(this, "animationHandler", 0,
            state -> state.setAndContinue(RawAnimation.begin().thenLoop("idle")));
        data.add(main);
    }

    /**
     * Returns the GeckoLib animatable instance cache.
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    /**
     * Valid attachment slot types.
     */
    public enum Type { SCOPE, MUZZLE, MAGAZINE, GRIP }
}