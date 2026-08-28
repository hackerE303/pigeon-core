package software.hacker_E303.pigeon_core.geo.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

/**
 * Represents a Geo entity controller with configuration methods.
 */
public final class GeoController {

    private record AnimEntry(String name, boolean loop, AnimUseEvent event) {}

    private final String id;
    private final GeoAnimatable animatable;
    private final int transition;
    private final String idle;
    private final AnimationController<?> raw;
    private final List<String> triggers = new ArrayList<>();
    private final List<AnimEntry> entries = new ArrayList<>();
    private final Map<String, Boolean> triggerLoops = new HashMap<>();
    private Consumer<String> action = null;

    private GeoController(String id, String idle, int transition, GeoAnimatable animatable) {
        this.id = id;
        this.idle = idle;
        this.transition = transition;
        this.animatable = animatable;
        this.raw = null;
    }

    private GeoController(AnimationController<?> raw) {
        this.raw = raw;
        this.id = null;
        this.idle = null;
        this.transition = 0;
        this.animatable = null;
    }

    /**
     * Creates a new GeoController for a Geo animatable.
     *
     * @param name       The controller ID
     * @param idle       The name of the idle (fallback) animation
     * @param transition The transition tick time between animations
     * @param animatable The Geo animatable to control
     * @return A new GeoController instance
     */
    public static GeoController create(String name, String idle, int transition, GeoAnimatable animatable) {
        return new GeoController(name, idle, transition, animatable);
    }

    /**
     * Wraps an existing {@link AnimationController} so it can be registered through
     * {@link GeoControllerContext}. {@link #build()} returns it as-is.
     *
     * @param controller The pre-built controller to wrap
     * @return A new GeoController instance
     */
    public static GeoController create(AnimationController<?> controller) {
        return new GeoController(controller);
    }

    /**
     * Adds a trigger animation, played when explicitly triggered via GeckoLib's trigger system.
     *
     * @param name The name of the trigger animation
     * @param loop Whether the animation loops
     * @return This controller for method chaining
     */
    public GeoController trigger(String name, boolean loop) {
        this.triggers.add(name);
        this.triggerLoops.put(name, loop);
        return this;
    }

    /**
     * Adds a conditional animation driven by an {@link AnimUseEvent} entity-state check.
     * When the controller ticks, all registered animations are evaluated in descending
     * priority order and the first one whose event condition holds is played.
     *
     * @param name  The name of the animation
     * @param loop  Whether the animation loops
     * @param event The condition that determines when this animation plays
     * @return This controller for method chaining
     */
    /**
     * Registers a handler for custom instruction keyframes defined in the animation JSON.
     * The consumer receives the instruction string from the keyframe and can dispatch
     * any logic based on it.
     *
     * @param handler Consumer that receives the instruction string from the keyframe
     * @return This controller for method chaining
     */
    public GeoController action(Consumer<String> handler) {
        this.action = handler;
        return this;
    }

    public GeoController anim(String name, boolean loop, AnimUseEvent event) {
        this.entries.add(new AnimEntry(name, loop, event));
        return this;
    }

    /**
     * Builds the {@link AnimationController} from this configuration.
     *
     * @return A configured AnimationController ready for registration
     */
    public AnimationController<?> build() {
        if (raw != null) return raw;

        List<AnimEntry> sorted = entries.stream()
            .sorted(Comparator.comparingInt(e -> -e.event().getPriority()))
            .toList();

        AnimationController<GeoAnimatable> controller = new AnimationController<>(animatable, id, transition,
            state -> {
                for (AnimEntry entry : sorted) {
                    if (entry.event().test(state)) {
                        return state.setAndContinue(entry.loop()
                            ? RawAnimation.begin().thenLoop(entry.name())
                            : RawAnimation.begin().thenPlay(entry.name()));
                    }
                }
                return state.setAndContinue(RawAnimation.begin().thenLoop(idle));
            });

        for (String trigger : triggers) {
            controller.triggerableAnim(trigger, triggerLoops.getOrDefault(trigger, false)
                ? RawAnimation.begin().thenLoop(trigger)
                : RawAnimation.begin().thenPlay(trigger));
        }
        if (action != null)
            controller.setCustomInstructionKeyframeHandler(
                event -> action.accept(event.getKeyframeData().getInstructions()));
        return controller;
    }
}
