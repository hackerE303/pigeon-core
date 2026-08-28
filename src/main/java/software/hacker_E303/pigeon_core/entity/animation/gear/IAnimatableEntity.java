package software.hacker_E303.pigeon_core.entity.animation.gear;

import java.util.Map;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Marks an entity as capable of playing animations synced from server to client.
 * <p>
 * Implementations manage one or more animation managers through entity data
 * and expose helpers for starting, stopping, and querying animation state.
 *
 * @param <T> the concrete entity type
 */
@SuppressWarnings("unchecked")
public interface IAnimatableEntity<T extends Entity> {

    /**
     * Stores the entity data accessors for a single animation instance.
     */
    public record AnimationManager(
        /**
         * The currently active animation index.
         */
        EntityDataAccessor<Integer> anim, 
        /**
         * The tick key identifying the current animation playback.
         */
        EntityDataAccessor<Integer> key, 
        /**
         * The animation duration in ticks.
         */
        EntityDataAccessor<Integer> duration, 
        /**
         * The remaining cooldown in ticks.
         */
        EntityDataAccessor<Float> cooldown,
        /**
         * The playback speed multiplier.
         */
        EntityDataAccessor<Float> speed, 
        /**
         * The transition blend length in ticks.
         */
        EntityDataAccessor<Float> transition,
        /**
         * Whether the animation loops when it reaches its end.
         */
        EntityDataAccessor<Boolean> looped
    ) {}

    /**
     * Groups available animations by index for lookup by ID.
     */
    public class Animations {

        private final Animation[] animations;

        /**
         * Creates a new animation registry from the provided animations.
         *
         * @param animations the animations to register
         */
        public Animations(Animation... animations) {
            this.animations = animations;
        }

        /**
         * Returns the animation at the given index, or {@code null} if out of bounds.
         *
         * @param index the animation index
         * @return the animation, or {@code null}
         */
        @Nullable
        public Animation get(int index) {
            if (index >= 0 && index < count()) {
                return animations[index];
            }
            return null;
        }
    
        /**
         * Returns the total number of registered animations.
         *
         * @return the animation count
         */
        public int count() {
            return animations.length;
        }
    }

    /**
     * Manages an array of {@link AnimationManager} instances, one per animation slot.
     */
    public class AnimationManagers {

        private final AnimationManager[] MANAGERS;

        /**
         * Creates animation managers for the given entity class and manager count.
         *
         * @param clazz the entity class used to define tracked data IDs
         * @param count the number of managers to create
         */
        public AnimationManagers(Class<? extends Entity> clazz, int count) {
            MANAGERS = IntStream.range(0, count)

            .mapToObj(index -> new AnimationManager(
                SynchedEntityData.defineId(clazz, EntityDataSerializers.INT),
                SynchedEntityData.defineId(clazz, EntityDataSerializers.INT),
                SynchedEntityData.defineId(clazz, EntityDataSerializers.INT),
                SynchedEntityData.defineId(clazz, EntityDataSerializers.FLOAT),
                SynchedEntityData.defineId(clazz, EntityDataSerializers.FLOAT),
                SynchedEntityData.defineId(clazz, EntityDataSerializers.FLOAT),
                SynchedEntityData.defineId(clazz, EntityDataSerializers.BOOLEAN)
            )).toArray(AnimationManager[]::new);
        }

        /**
         * Returns the raw manager array.
         *
         * @return the animation managers
         */
        public AnimationManager[] array() {
            return MANAGERS;
        }

        /**
         * Returns the number of managed animation slots.
         *
         * @return the manager count
         */
        public int count() {
            return MANAGERS.length;
        }
    }
    /**
     * Returns the registered animations for this entity.
     *
     * @return the animation registry
     */
    public Animations getAnimations();

    /**
     * Returns the animation managers for this entity.
     *
     * @return the manager registry
     */
    public AnimationManagers getAnimationManagers();

    /**
     * Casts this interface back to the underlying entity type.
     *
     * @return the entity as {@code T}
     */
    default T asEntity() {
        return (T) this;
    }

    /**
     * Checks whether the given manager index is valid for this entity.
     *
     * @param index the manager index
     * @return {@code true} if the index is within bounds
     */
    default boolean isValidManager(int index) {
        return index >= 0 && index < this.getAnimationManagers().count();
    }

    /**
     * Starts an animation with the default settings (non-looping, no transition).
     *
     * @param manager the manager index
     * @param animId the animation index
     * @param duration the duration in seconds
     */
    default void startAnim(int manager, int animId, double duration) {
        this.startAnim(manager, animId, duration, false);
    }

    /**
     * Starts an animation with the specified loop setting and no transition.
     *
     * @param manager the manager index
     * @param animId the animation index
     * @param duration the duration in seconds
     * @param looped whether the animation should loop
     */
    default void startAnim(int manager, int animId, double duration, boolean looped) {
        this.startAnim(manager, animId, duration, looped, false);
    }

    /**
     * Starts an animation on the specified manager.
     *
     * @param manager the manager index
     * @param animId the animation index
     * @param duration the duration in seconds
     * @param looped whether the animation should loop
     * @param transition whether to enable transition blending
     */
    default void startAnim(int manager, int animId, double duration, boolean looped, boolean transition) {
        if (!this.isValidManager(manager)) return;

        AnimationManager mng = this.getAnimationManagers().array()[manager];
        SynchedEntityData data = this.asEntity().getEntityData();

        if (animId < 0 || animId > getAnimations().count() - 1 || duration < 0) {
            stopAnim(manager);
            return;
        }
        int ticks = (int) (20 * duration);
        float smooth = transition ? 0.27f * Math.min(ticks, 20) : 0.0f;

        data.set(mng.anim(), animId);
        data.set(mng.duration(), ticks);
        data.set(mng.cooldown(), (float) ticks);
        data.set(mng.key(), this.asEntity().tickCount);
        data.set(mng.speed(), +1.0f);
        data.set(mng.transition(), smooth);
        data.set(mng.looped(), looped);
    }

    /**
     * Stops any playing animation on the specified manager.
     *
     * @param manager the manager index
     */
    default void stopAnim(int manager) {
        if (!this.isValidManager(manager)) return;

        AnimationManager mng = this.getAnimationManagers().array()[manager];
        SynchedEntityData data = this.asEntity().getEntityData();

        data.set(mng.anim(),     -1);
        data.set(mng.duration(), +0);
        data.set(mng.key(),      +0);
        data.set(mng.cooldown(), +0f);
        data.set(mng.looped(), false);
    }

    /**
     * Sets the playback speed for the animation on the specified manager.
     *
     * @param manager the manager index
     * @param speed the speed multiplier
     */
    default void setAnimSpeed(int manager, float speed) {
        if (!isValidManager(manager)) return;

        AnimationManager mng = this.getAnimationManagers().array()[manager];
        SynchedEntityData data = this.asEntity().getEntityData();

        data.set(mng.speed(), speed);
    }

    /**
     * Returns the active animation index for the specified manager.
     *
     * @param manager the manager index
     * @return the animation index, or {@code -1} if invalid
     */
    default int getAnim(int manager) {
        return isValidManager(manager) ? this.asEntity().getEntityData().get(this.getAnimationManagers().array()[manager].anim()) : -1;
    }

    /**
     * Checks whether the animation on the specified manager is looping.
     *
     * @param manager the manager index
     * @return {@code true} if the animation loops
     */
    default boolean isAnimLooped(int manager) {
        return isValidManager(manager) ? this.asEntity().getEntityData().get(this.getAnimationManagers().array()[manager].looped()) : false;
    }

    /**
     * Returns the duration of the animation on the specified manager, in ticks.
     *
     * @param manager the manager index
     * @return the duration in ticks
     */
    default int getAnimDuration(int manager) {
        return isValidManager(manager) ? this.asEntity().getEntityData().get(this.getAnimationManagers().array()[manager].duration()) : 0;
    }

    /**
     * Returns the current playback speed for the animation on the specified manager.
     *
     * @param manager the manager index
     * @return the speed multiplier
     */
    default float getAnimSpeed(int manager) {
        return isValidManager(manager) ? this.asEntity().getEntityData().get(this.getAnimationManagers().array()[manager].speed()) : 1.0f;
    }

    /**
     * Returns the transition blend length for the animation on the specified manager, in ticks.
     *
     * @param manager the manager index
     * @return the transition length in ticks
     */
    default float getAnimTransition(int manager) {
        return isValidManager(manager) ? this.asEntity().getEntityData().get(this.getAnimationManagers().array()[manager].transition()) : 0;
    }

    /**
     * Returns the server tick key for the animation on the specified manager.
     *
     * @param manager the manager index
     * @return the tick key
     */
    default int getAnimKey(int manager) {
        return isValidManager(manager) ? this.asEntity().getEntityData().get(this.getAnimationManagers().array()[manager].key()) : 0;
    }

    /**
     * Checks whether an animation is currently playing on the specified manager.
     *
     * @param manager the manager index
     * @return {@code true} if an animation is active
     */
    default boolean hasAnim(int manager) {
        return this.getAnim(manager) > -1;
    }

    /**
     * Checks whether any manager has an active animation.
     *
     * @return {@code true} if at least one animation is playing
     */
    default boolean hasAnyAnim() {
        for (int i = 0; this.getAnimationManagers().count() > i; i++) {
            if (this.hasAnim(i)) return true;
        }
        return false;
    }

    /**
     * Client-side playback state for a single animation manager.
     */
    @OnlyIn(Dist.CLIENT)
    public class ClientState {
        public long serverKey = -1;
        public long lastUpdateNs = System.nanoTime();
        public float localElapsedMs = 0f;
        public float lastSpeed = 1f;
    }

    @OnlyIn(Dist.CLIENT)
    static Map<Entity, ClientState[]> CLIENT_CACHE = new java.util.WeakHashMap<>();

    /**
     * Returns the client-side state array for all managers, creating them if necessary.
     *
     * @return the client state array
     */
    @OnlyIn(Dist.CLIENT)
    default ClientState[] getClientStates() {
        return CLIENT_CACHE.computeIfAbsent(this.asEntity(), e -> {
            ClientState[] arr = new ClientState[this.getAnimationManagers().count()];
            for (int i = 0; i < arr.length; i++) arr[i] = new ClientState();
            return arr;
        });
    }

    /**
     * Returns the client-side state for the specified manager.
     *
     * @param manager the manager index
     * @return the client state
     */
    @OnlyIn(Dist.CLIENT)
    default ClientState getClientState(int manager) {
        return getClientStates()[manager];
    }

    /**
     * Initializes all animation entity data entries to their default values.
     */
    default void defineAnimatableData() {

        SynchedEntityData data = this.asEntity().getEntityData();
        for (AnimationManager mng : this.getAnimationManagers().array()) {

            data.define(mng.anim(),       -1);
            data.define(mng.key(),        +0);
            data.define(mng.duration(),   +0);
            data.define(mng.cooldown(),   +0f);
            data.define(mng.speed(),      +1f);
            data.define(mng.transition(), +0f);
            data.define(mng.looped(), false);
        }
    }

    /**
     * Serializes all animation data into the given compound tag.
     *
     * @param nbt the tag to write into
     */
    default void addAnimatableData(CompoundTag nbt) {

        SynchedEntityData data = this.asEntity().getEntityData();
        AnimationManager[] managers = this.getAnimationManagers().array();

        CompoundTag mainTag = new CompoundTag();
        for (int i = 0; i < managers.length; i++) {
            CompoundTag mngTag = new CompoundTag();

            mngTag.putInt("ID",           data.get(managers[i].anim()));
            mngTag.putInt("Duration",     data.get(managers[i].duration()));
            mngTag.putFloat("Cooldown",   data.get(managers[i].cooldown()));
            mngTag.putFloat("Speed",      data.get(managers[i].speed()));
            mngTag.putFloat("Transition", data.get(managers[i].transition()));
            mngTag.putBoolean("Loop",     data.get(managers[i].looped()));
            mainTag.put("Manager" + i, mngTag);
        }
        nbt.put("AnimationData", mainTag);
    }

    /**
     * Reads animation data back from the given compound tag.
     *
     * @param nbt the tag to read from
     */
    default void readAnimatableData(CompoundTag nbt) {
        if (nbt.contains("AnimationData")) {

            CompoundTag mainTag = nbt.getCompound("AnimationData");
            SynchedEntityData data = this.asEntity().getEntityData();
            AnimationManager[] managers = this.getAnimationManagers().array();

            for (int i = 0; i < managers.length; i++) {
                if (mainTag.contains("Manager" + i)) {

                    CompoundTag mngTag = mainTag.getCompound("Manager" + i);
                    data.set(managers[i].anim(),       mngTag.getInt("ID"));
                    data.set(managers[i].duration(),   mngTag.getInt("Duration"));
                    data.set(managers[i].cooldown(),   mngTag.getFloat("Cooldown"));
                    data.set(managers[i].speed(),      mngTag.getFloat("Speed"));
                    data.set(managers[i].transition(), mngTag.getFloat("Transition"));
                    data.set(managers[i].looped(),     mngTag.getBoolean("Loop"));
                }
            }
        }
    }

    /**
     * Ticks all animation managers, advancing cooldowns on the server and
     * synchronizing client playback state.
     */
    default void tickAnimatable() {

        Entity animable = this.asEntity();
        SynchedEntityData data = animable.getEntityData();
        AnimationManager[] managers = this.getAnimationManagers().array();

        for (int i = 0; i < managers.length; i++) {
            AnimationManager mng = managers[i];
            
            if (!animable.level().isClientSide) {
                float cooldown = data.get(mng.cooldown());
                float speed = data.get(mng.speed());

                if (cooldown > 0) {
                    cooldown -= speed;
                    if (cooldown <= 0) {
                        if (!isAnimLooped(i)) {
                            data.set(mng.anim(), -1);
                            cooldown = 0;
                        } else {
                            cooldown = data.get(mng.duration());
                            data.set(mng.transition(), 0.0f);
                            data.set(mng.key(), animable.tickCount);
                        }
                    }
                    data.set(mng.cooldown(), cooldown);
                }
            } else if (data.get(mng.anim()) != -1) {
                ClientState state = getClientState(i);

                int serverKey = data.get(mng.key());
                if (state.serverKey != serverKey) {
                    state.serverKey = serverKey;
                    state.localElapsedMs = 0f;
                    state.lastUpdateNs = System.nanoTime();
                    state.lastSpeed = data.get(mng.speed());
                }
            }
        }
    }
}