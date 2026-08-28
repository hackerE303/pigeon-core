package software.hacker_E303.pigeon_core.client.gun.animation;

import software.hacker_E303.pigeon_core.util.BetterMath;

/**
 * This class serves as a central repository for all animation-related data used by guns,
 * including camera movements, bone animations, player states, weapon states, and spark effects.
 */
public class AnimationCache {

    /** CameraData for this cache */
    public CameraData camera = new CameraData();
    /** BoneData for this cache */
    public BoneData     bone = new   BoneData();
    /** PlayerData for this cache */
    public PlayerData player = new PlayerData();
    /** WeaponData for this cache */
    public WeaponData weapon = new WeaponData();
    /** SparkData for this cache */
    public SparkData   spark = new  SparkData();

    /**
     * Contains camera-related data for gun animations.
     * Manages camera offsets, rotations, velocities, and initialization state for smooth camera movements.
     */
    public static class CameraData {

        /** Camera damping factor for smooth movement */
        public float CAMERA_DAMPING = 0.9f;
        /** Camera offset along X-axis */
        public float CAMERA_OFFSET_X = 0f;
        /** Camera offset along Y-axis */
        public float CAMERA_OFFSET_Y = 0f;
        /** Camera offset along Z-axis */
        public float CAMERA_OFFSET_Z = 0f;
        /** Current camera rotation around X-axis (pitch) */
        public float CAMERA_CURRENT_X = 0.0f;
        /** Current camera rotation around Y-axis (yaw) */
        public float CAMERA_CURRENT_Y = 0.0f;
        /** Current camera rotation around Z-axis (roll) */
        public float CAMERA_CURRENT_ROLL = 0.0f;

        /** Velocity along X-axis */
        public float VELOCITY_X = 0.0f;
        /** Velocity along Y-axis */
        public float VELOCITY_Y = 0.0f;
        /** Velocity along Z-axis */
        public float VELOCITY_Z = 0.0f;
        /** Rotation velocity around X-axis (pitch) */
        public float ROT_VELOCITY_PITCH = 0.0f;
        /** Rotation velocity around Y-axis (yaw) */
        public float ROT_VELOCITY_YAW = 0.0f;
        /** Rotation velocity around Z-axis (roll) */
        public float ROT_VELOCITY_ROLL = 0.0f;

        /** Current rotation around X-axis (pitch) */
        public float ROTATION_YAW = 0.0f;
        /** Current rotation around Z-axis (roll) */
        public float ROTATION_ROLL = 0.0f;
        /** Rotation velocity around Z-axis (roll) */
        public float VELOCITY_ROLL = 0.0f;

        /** Camera rotation around X-axis (pitch) from bone "Cam" */
        public float CAMERA_BONE_ROT_X = 0.0f;
        /** Camera rotation around Y-axis (yaw) from bone "Cam" */
        public float CAMERA_BONE_ROT_Y = 0.0f;
        /** Camera rotation around Z-axis (roll) from bone "Cam" */
        public float CAMERA_BONE_ROT_Z = 0.0f;
        
        /** Current interpolated camera bone rotation around X-axis (pitch) */
        public float CAMERA_CURRENT_BONE_ROT_X = 0.0f;
        /** Current interpolated camera bone rotation around Y-axis (yaw) */
        public float CAMERA_CURRENT_BONE_ROT_Y = 0.0f;
        /** Current interpolated camera bone rotation around Z-axis (roll) */
        public float CAMERA_CURRENT_BONE_ROT_Z = 0.0f;
        
        /** Velocity for camera bone rotation around X-axis (pitch) */
        public float VELOCITY_BONE_ROT_X = 0.0f;
        /** Velocity for camera bone rotation around Y-axis (yaw) */
        public float VELOCITY_BONE_ROT_Y = 0.0f;
        /** Velocity for camera bone rotation around Z-axis (roll) */
        public float VELOCITY_BONE_ROT_Z = 0.0f;

        /** Update time in nanoseconds for accurate deltaTime calculation */
        public long LAST_UPDATE_NANO = 0L;
        /** Detects if the camera has ever been used */
        public boolean IS_INITIALIZED = false;
        
        /** Target field of view (FOV) when aiming */
        public float TARGET_FOV = 0.0f;
        /** Current interpolated field of view (FOV) */
        public float CURRENT_FOV = 0.0f;
        /** Velocity for FOV interpolation */
        public float FOV_VELOCITY = 0.0f;
        /** FOV damping factor for smooth movement */
        public float FOV_DAMPING = 6.0f;
        /** FOV spring strength for smooth movement */
        public float FOV_SPRING_STRENGTH = 60.0f;
    }
    
    /**
     * Contains bone-related data for gun animations.
     * Manages bone positions, rotations, bobbing effects, walking/sprinting animations, and shake effects.
     */
    public static class BoneData {

        /** Previous position along X-axis for interpolation */
        public float PREV_POS_X = 0f;
        /** Previous position along Y-axis for interpolation */
        public float PREV_POS_Y = 0f;
        /** Previous position along Z-axis for interpolation */
        public float PREV_POS_Z = 0f;
        /** Previous rotation around X-axis for interpolation */
        public float PREV_ROT_X = 0f;
        /** Previous rotation around Y-axis for interpolation */
        public float PREV_ROT_Y = 0f;
        /** Previous rotation around Z-axis for interpolation */
        public float PREV_ROT_Z = 0f;

        /** Final X position offset for bobbing */
        public float BOBBING_POS_X = 0f;
        /** Final Y position offset for bobbing */
        public float BOBBING_POS_Y = 0f;
        /** Final Z position offset for bobbing */
        public float BOBBING_POS_Z = 0f;
        /** Final X rotation offset for bobbing */
        public float BOBBING_ROT_X = 0f;
        /** Final Y rotation offset for bobbing */
        public float BOBBING_ROT_Y = 0f;
        /** Final Z rotation offset for bobbing */
        public float BOBBING_ROT_Z = 0f;

        /** X position offset for walking animation */
        public float WALKING_POS_X = 0f;
        /** Y position offset for walking animation */
        public float WALKING_POS_Y = 0f;
        /** Z position offset for walking animation */
        public float WALKING_POS_Z = 0f;
        /** X rotation offset for walking animation */
        public float WALKING_ROT_X = 0f;
        /** Y rotation offset for walking animation */
        public float WALKING_ROT_Y = 0f;
        /** Z rotation offset for walking animation */
        public float WALKING_ROT_Z = 0f;

        /** X position offset for sprinting animation */
        public float SPRINTING_POS_X = 0f;
        /** Y position offset for sprinting animation */
        public float SPRINTING_POS_Y = 0f;
        /** Z position offset for sprinting animation */
        public float SPRINTING_POS_Z = 0f;
        /** X rotation offset for sprinting animation */
        public float SPRINTING_ROT_X = 0f;
        /** Y rotation offset for sprinting animation */
        public float SPRINTING_ROT_Y = 0f;
        /** Z rotation offset for sprinting animation */
        public float SPRINTING_ROT_Z = 0f;

        /** Accumulated time for bobbing animation */
        public float BOBBING_TIME = 0f;
        /** Current phase of bobbing oscillation */
        public float BOBBING_PHASE = 0f;
        /** Progress of transition between walking and sprinting */
        public float TRANSITION_PROGRESS = 0f;
        /** Current amplitude reduction factor */
        public float AMP_FACTOR = 1.0f;

        /** Current shake intensity */
        public float SHAKE_INTENSITY = 0f;
        /** Target shake intensity */
        public float SHAKE_TARGET = 0f;
        /** Noise phase for random bone effects */
        public float NOISE_PHASE = 0f;
        /** Noise timer for random bone effects */
        public float NOISE_TIMER = 0f;

        /** Damping lambda value for interpolation */
        public float LAMBDA = 0f;
        /** Accumulator for animation calculations */
        public float ACCUMULATOR = 0f;

        /** Previous pose state (0 = idle, 1 = aiming, 2 = running) for transition detection */
        public int PREVIOUS_POSE = 0;

        /** Update time in nanoseconds for accurate deltaTime calculation */
        public long LAST_UPDATE_NANO = 0L;
        /** Gives the first values to the bone */
        public boolean IS_INITIALIZED = false;
    }

    /**
     * Contains player-related data for gun animations.
     * Manages impact effects, step timings, movement velocities, and grounded state.
     */
    public static class PlayerData {

        /** Impact velocity */
        public float IMPACT_VELOCITY = 0f;
        /** Impact damping factor */
        public float IMPACT_DAMPING = 0.85f;
        /** Impact bounce amount */
        public float IMPACT_BOUNCE = 0f;
        /** Impact timer */
        public float IMPACT_TIMER = 0f;

        /** Current step time */
        public float STEP_TIME = 0f;
        /** Step interval */
        public float STEP_INTERVAL = 0.5f;
        /** Step variation */
        public float STEP_VARIATION = 0.1f;
        /** Last step time */
        public float LAST_STEP_TIME = 0f;
        /** Step intensity */
        public float STEP_INTENSITY = 0f;
        /** Time when the player started moving continuously */
        public long MOVEMENT_START_TIME = 0L;

        /** Current player velocity */
        public float VELOCITY = 0f;
        /** Last player velocity */
        public float LAST_VELOCITY = 0f;
        /** Vertical velocity */
        public float VERTICAL_VELOCITY = 0f;
        /** Maximum fall height */
        public float MAX_FALL_HEIGHT = 0f;
        /** Whether the player is on the ground */
        public boolean IS_GROUNDED    = true;
    }

    /**
     * Contains weapon-related data for gun animations.
     * Manages aim cooldown, run cooldown, shooting state, and spark effects during shooting.
     */
    public static class WeaponData {

        /** Player aim cooldown in ticks */
        private int AIM_COOLDOWN = 0;
        /** Player run cooldown in ticks */
        private int RUN_COOLDOWN = 0;

        /**
         * Gets the player aim cooldown.
         * @return Aim cooldown in ticks
         */
        public int getAimCooldown() {
            return AIM_COOLDOWN;
        }

        /**
         * Sets the player aim cooldown.
         * @param index Aim cooldown in ticks
         */
        public void setAimCooldown(int index) {
            AIM_COOLDOWN = index;
        }

        /**
         * Gets the player run cooldown.
         * @return Run cooldown in ticks
         */
        public int getRunCooldown() {
            return RUN_COOLDOWN;
        }

        /**
         * Sets the player run cooldown.
         * @param index Run cooldown in ticks
         */
        public void setRunCooldown(int index) {
            RUN_COOLDOWN = index;
        }
    }

    /**
     * Contains spark-related data for gun animations.
     * Manages random scale and rotation for spark effects during shooting.
     */
    public static class SparkData {

        /** Random scale for spark effects */
        private float SCALE = 0f;
        /** Random rotation for spark effects */
        private float ROTATION = 0f;

        /** Whether the gun is currently shooting */
        private boolean IS_SPARK_VISIBLE = false;

        /**
         * Gets the random scale for spark effects.
         * @return Random spark scale
         */
        public float getScale() {
            return SCALE;
        }

        /**
         * Gets the random rotation for spark effects.
         * @return Random spark rotation in radians
         */
        public float getRotation() {
            return ROTATION;
        }

        /**
         * Checks if the gun is currently shooting.
         * @return True if shooting, false otherwise
         */
        public boolean isSparkVisible() {
            return IS_SPARK_VISIBLE;
        }

        /**
         * Triggers a shooting animation with random spark effects.
         * @param minSparkScale Minimum scale for spark effects
         * @param maxSparkScale Maximum scale for spark effects
         */
        public void showSpark(float minSparkScale, float maxSparkScale) {
            IS_SPARK_VISIBLE = true;

            SCALE    = BetterMath.randomFrom(minSparkScale, maxSparkScale);
            ROTATION = BetterMath.randomRotation();
        }

        /**
         * Stops the shooting animation.
         */
        public void hideSpark() {
            SCALE      = 0f;
            ROTATION   = 0f;
            IS_SPARK_VISIBLE = false;
        }
    }
}