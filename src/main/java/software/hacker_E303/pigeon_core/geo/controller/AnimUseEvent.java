package software.hacker_E303.pigeon_core.geo.controller;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.core.animation.AnimationState;

/**
 * Enum for animation use events with priority weights and entity-state conditions.
 */
public enum AnimUseEvent {
    OTHER(100) {
        @Override
        public boolean test(AnimationState<?> state) { return false; }
    },
    DEATH(90) {
        @Override
        public boolean test(AnimationState<?> state) {
            return state.getAnimatable() instanceof LivingEntity e && e.isDeadOrDying();
        }
    },
    HURT(85) {
        @Override
        public boolean test(AnimationState<?> state) {
            return state.getAnimatable() instanceof LivingEntity e && e.hurtTime > 0;
        }
    },
    ATTACK(80) {
        @Override
        public boolean test(AnimationState<?> state) {
            return state.getAnimatable() instanceof LivingEntity e && e.swinging && e.swingTime >= 0;
        }
    },
    FALL(65) {
        @Override
        public boolean test(AnimationState<?> state) {
            return state.getAnimatable() instanceof LivingEntity e && e.isFallFlying();
        }
    },
    HOSTILE(60) {
        @Override
        public boolean test(AnimationState<?> state) {
            return state.getAnimatable() instanceof Mob m && m.getTarget() != null;
        }
    },
    WALK(50) {
        @Override
        public boolean test(AnimationState<?> state) {
            return state.isMoving();
        }
    },
    SWIM(40) {
        @Override
        public boolean test(AnimationState<?> state) {
            return state.getAnimatable() instanceof LivingEntity e && e.isInWater();
        }
    },
    RIDE(25) {
        @Override
        public boolean test(AnimationState<?> state) {
            return state.getAnimatable() instanceof LivingEntity e && e.isPassenger();
        }
    };

    private final int priority;

    AnimUseEvent(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public abstract boolean test(AnimationState<?> state);
}
