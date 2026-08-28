package software.hacker_E303.pigeon_core.entity.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Provides entity sound data for mobs that implement this interface.
 */
public interface IEntitySounds {

    /**
     * Returns the sound set associated with this entity.
     *
     * @return the behavior sounds for this entity
     */
    public BehaviorSounds getSounds();

    /**
     * Holds a collection of behavior-related sound events and default playback parameters.
     */
    public class BehaviorSounds {

        private final SoundEvent ambient;
        private final SoundEvent hurt;
        private final SoundEvent death;
        private final SoundEvent step;
        private final float defaultVolume;
        private final float defaultPitch;

        /**
         * Creates a new behavior sound set.
         *
         * @param ambient the ambient sound event
         * @param hurt the hurt sound event
         * @param death the death sound event
         * @param step the step sound event
         * @param defaultVolume the default playback volume
         * @param defaultPitch the default playback pitch
         */
        private BehaviorSounds(SoundEvent ambient, SoundEvent hurt, SoundEvent death, SoundEvent step, float defaultVolume, float defaultPitch) {
            this.ambient = ambient;
            this.hurt = hurt;
            this.death = death;
            this.step = step;
            this.defaultVolume = defaultVolume;
            this.defaultPitch = defaultPitch;
        }

        /**
         * Returns the ambient sound event.
         *
         * @return the ambient sound
         */
        public SoundEvent ambient() {
            return this.ambient;
        }

        /**
         * Returns the hurt sound event.
         *
         * @return the hurt sound
         */
        public SoundEvent hurt() {
            return this.hurt;
        }

        /**
         * Returns the death sound event.
         *
         * @return the death sound
         */
        public SoundEvent death() {
            return this.death;
        }

        /**
         * Returns the step sound event.
         *
         * @return the step sound
         */
        public SoundEvent step() {
            return this.step;
        }

        /**
         * Returns the default playback volume.
         *
         * @return the default volume
         */
        public float defaultVolume() {
            return this.defaultVolume;
        }

        /**
         * Returns the default playback pitch.
         *
         * @return the default pitch
         */
        public float defaultPitch() {
            return this.defaultPitch;
        }

        /**
         * Returns the sound event for the specified behavior type.
         *
         * @param type the sound type to look up
         * @return the matching sound event, or {@code null} if not found
         */
        public SoundEvent get(SoundType type) {
            return switch (type) {
                case AMBIENT -> ambient;
                case HURT -> hurt;
                case DEATH -> death;
                case STEP -> step;
            };
        }

        /**
         * Enumerates the supported behavior sound categories.
         */
        public enum SoundType {
            AMBIENT,
            HURT,
            DEATH,
            STEP
        }
        
        private static final ResourceLocation VILLAGER_AMBIENT = new ResourceLocation("entity.villager.ambient");
        private static final ResourceLocation VILLAGER_HURT    = new ResourceLocation("entity.villager.hurt");
        private static final ResourceLocation VILLAGER_DEATH   = new ResourceLocation("entity.villager.death");
        private static final ResourceLocation VILLAGER_STEP    = new ResourceLocation("entity.zombie_villager.step");

        private static final ResourceLocation ZOMBIE_AMBIENT = new ResourceLocation("entity.zombie.ambient");
        private static final ResourceLocation ZOMBIE_HURT    = new ResourceLocation("entity.zombie.hurt");
        private static final ResourceLocation ZOMBIE_DEATH   = new ResourceLocation("entity.zombie.death");
        private static final ResourceLocation ZOMBIE_STEP    = new ResourceLocation("entity.zombie.step");

        public static final BehaviorSounds VILLAGER = BehaviorSounds.create(asSound(VILLAGER_AMBIENT), asSound(VILLAGER_HURT),
            asSound(VILLAGER_DEATH), asSound(VILLAGER_STEP), 1.0f, 1.0f);

        public static final BehaviorSounds ZOMBIE = BehaviorSounds.create(asSound(ZOMBIE_AMBIENT), asSound(ZOMBIE_HURT),
            asSound(ZOMBIE_DEATH), asSound(ZOMBIE_STEP), 1.0f, 1.0f);

        /**
         * Resolves a {@link ResourceLocation} to a registered {@link SoundEvent}.
         *
         * @param location the resource location of the sound
         * @return the sound event, or {@code null} if unregistered
         */
        private static SoundEvent asSound(ResourceLocation location) {
            return ForgeRegistries.SOUND_EVENTS.getValue(location);
        }

        /**
         * Creates a new {@code BehaviorSounds} instance with the given parameters.
         *
         * @param ambient the ambient sound event
         * @param hurt the hurt sound event
         * @param death the death sound event
         * @param step the step sound event
         * @param defVolume the default playback volume
         * @param defPitch the default playback pitch
         * @return a new behavior sounds holder
         */
        public static BehaviorSounds create(SoundEvent ambient, SoundEvent hurt, SoundEvent death, SoundEvent step, float defVolume, float defPitch) {
            return new BehaviorSounds(ambient, hurt, death, step, defVolume, defPitch);
        }
    }
}