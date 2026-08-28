package software.hacker_E303.pigeon_core.client.gun.animation;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationController.State;
import software.bernie.geckolib.core.animation.AnimationProcessor.QueuedAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.model.GeoModel;
import software.hacker_E303.pigeon_core.common.Settings;
import software.hacker_E303.pigeon_core.geo.item.gun.EGun;
import software.hacker_E303.pigeon_core.gun.gear.GunTracker;
import software.hacker_E303.pigeon_core.gun.goals.LightGunnerAiGoal;
import software.hacker_E303.pigeon_core.main.event.network.RouterUtils;
import software.hacker_E303.pigeon_core.util.BetterData;
import software.hacker_E303.pigeon_core.util.BetterMath;

/**
 * Provides utilities for setting up GeckoLib animation controllers and updating
 * client-side gun animation state each frame.
 */
public class AnimationUtils {

	private static final float TIME_STEP    = AnimationManager.TIME_STEP;
	private static final float SOUND_FACTOR = AnimationManager.SOUND_FACTOR;

	private static final float BASE_WEIGHT  = AnimationManager.BASE_WEIGHT;
	private static final float BASE_LAMBDA  = AnimationManager.BASE_LAMBDA;

    /**
     * Registers the main animation controller for a gun, including idle, reload,
     * shoot, and hold animations, plus custom instruction and sound handlers.
     *
     * @param animatable the animatable gun
     * @param data the controller registrar
     */
    public static <T extends EGun> void setupMainController(T animatable, AnimatableManager.ControllerRegistrar data) {

		ItemStack[] currentStack = new ItemStack[]{ItemStack.EMPTY};
        AnimationController<T> actionController = new AnimationController<>(animatable, "animationHandler", 0, state -> {

            currentStack[0] = state.getData(DataTickets.ITEMSTACK);
			return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
		});
		
		if (animatable.getShellsCount() > 0) {

    		actionController.triggerableAnim("reload_empty", RawAnimation.begin().thenPlay("reload_empty"));
			actionController.triggerableAnim("reload_full", RawAnimation.begin().thenPlay("reload_full"));

		} else {
			actionController.triggerableAnim("reload", RawAnimation.begin().thenPlay("reload"));
		}
		actionController.triggerableAnim("shoot", RawAnimation.begin().thenPlay("shoot"));
		actionController.triggerableAnim("hold", RawAnimation.begin().thenPlay("hold"));

    	actionController.setCustomInstructionKeyframeHandler(state -> {
			
			String instruction = state.getKeyframeData().getInstructions();
			ItemStack stack = currentStack[0];

			EGun.process(stack, gun -> {
				AnimationCache.SparkData s = AnimationManager.getSparkForInstance(gun.getGeckoId());

				LocalPlayer player = Minecraft.getInstance().player;
				boolean isHeldByInstance = GunTracker.isHeldByInstancePlayer(gun.readCurrentStack());

				if (player == null) return;
				if (instruction.contains("show_spark")) {

					s.showSpark(gun.getProperties().minSparkSize(), gun.getProperties().maxSparkSize());
					if (isHeldByInstance) player.setXRot(player.getXRot() - gun.getProperties().recoil());

				} else if (instruction.contains("hide_spark")) s.hideSpark();

				if (instruction.contains("reload") && isHeldByInstance) RouterUtils.Internal.startReloadingGun();
				if (instruction.contains("render")) AnimationManager.IS_CLIENT_GUN_VISIBLE = true;
			});
    	});
		actionController.setSoundKeyframeHandler(state -> {

			ItemStack stack = currentStack[0];
			if (stack == null) return;

			Entity entity = GunTracker.getGunHolder(stack);
			if (entity == null) return;

			String instruction = state.getKeyframeData().getSound();

			if (animatable instanceof EGun)
				if (instruction.contains("ejection")) {

					if (entity instanceof Player || !(entity instanceof Mob mob))
						entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), animatable.getEjectionSound(), SoundSource.NEUTRAL, 0.61f, 0.6f + BetterMath.rand.nextFloat() * 0.5f, false);
					else {
						Player player = Minecraft.getInstance().player;
						double distance = mob.getMainHandItem() != null ?  BetterData.getData(mob.getMainHandItem(), LightGunnerAiGoal.DISTANCE, 0.0) : 0.0;

						if (Math.sqrt(mob.distanceToSqr(player)) < distance * 0.7) entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), animatable.getEjectionSound(), SoundSource.NEUTRAL, 0.352f, 0.6f + BetterMath.rand.nextFloat() * 0.5f, false);
					}
				}
		});
        data.add(actionController);
    }

    /**
     * Registers shell ejection animation controllers for the gun.
     *
     * @param animatable the animatable gun
     * @param data the controller registrar
     */
    public static <T extends EGun> void setupEjectionController(T animatable, AnimatableManager.ControllerRegistrar data) {
		if (animatable.getShellsCount() == 0) {

    		AnimationController<T> ejectController = new AnimationController<>(animatable, EGun.EJECTION_CONTROLLER, 0, state -> PlayState.CONTINUE);
    		ejectController.triggerableAnim("shell_ejection", RawAnimation.begin().thenPlay("shell_ejection"));
    
    		data.add(ejectController);
		} else for (int i = 0; i < animatable.getShellsCount(); i++) {

    		AnimationController<T> ejectController = new AnimationController<>(animatable, EGun.EJECTION_CONTROLLER + i, 0, state -> PlayState.CONTINUE);
    		ejectController.triggerableAnim("shell_ejection" + i, RawAnimation.begin().thenPlay("shell_ejection" + i));
    
    		data.add(ejectController);
		}
	}

    /**
     * Registers the fire-mode animation controller that loops between semi-automatic
     * and automatic poses based on the gun's current mode.
     *
     * @param animatable the animatable gun
     * @param data the controller registrar
     */
    public static <T extends EGun> void setupModeController(T animatable, AnimatableManager.ControllerRegistrar data) {

    	AnimationController<T> ejectController = new AnimationController<>(animatable, EGun.EJECTION_CONTROLLER, 5, state -> {

			return EGun.process(state.getData(DataTickets.ITEMSTACK), gun -> {
				if (gun.getMode().equals(EGun.Mode.SEMI_AUTOMATIC)) return state.setAndContinue(RawAnimation.begin().thenLoop("semi-automatic"));
				return state.setAndContinue(RawAnimation.begin().thenLoop("automatic"));
			}, null);
		});
    
    	data.add(ejectController);
	}

    /**
     * Updates the spark bone's rotation and scale based on the current spark data.
     *
     * @param stack the gun stack
     * @param model the geo model
     */
    private static <T extends GeoItem> void animateSpark(ItemStack stack, GeoModel<T> model) {

		EGun.process(stack, gun -> {
			AnimationCache.SparkData s = AnimationManager.getSparkForInstance(gun.getGeckoId());
    		CoreGeoBone spark = model.getAnimationProcessor().getBone("Spark");

			if (spark == null) spark = model.getAnimationProcessor().getBone("spark");
			if (spark == null) return;

        	spark.setRotZ(s.getRotation());
			float scale = s.getScale();

    		spark.setScaleX(scale);
    		spark.setScaleY(scale);
    		spark.setScaleZ(scale);
		});
    }

	private static boolean isFallen = false;

    /**
     * Updates the gun model bones each frame, including spark animation, bobbing,
     * camera shake, and interpolation.
     *
     * @param stack the gun stack
     * @param model the geo model
     * @param controlName the control bone name
     * @param instanceId the GeckoLib instance id
     */
    public static <T extends GeoItem> void animateGun(ItemStack stack, GeoModel<T> model, String controlName, long instanceId) {

		animateSpark(stack, model);

		if (EGun.from(stack) == null || !GunTracker.isHeldByInstancePlayer(stack)) {
			resetControl(model, controlName);
			return;
		}

		if (!AnimationManager.IS_HOLDING_STARTED)
		EGun.process(stack, gun -> {
			if (!AnimationManager.IS_HOLDING_STARTED) AnimationManager.resetValues();

			AnimatableInstanceCache cache = gun.getAnimatableInstanceCache();
            AnimatableManager<?> animatableManager = cache.getManagerForId(instanceId);

            String currentAnim = animatableManager.getAnimationControllers().values().stream()
                .filter(controller -> controller.getCurrentAnimation() != null)
                .findFirst()
                .map(controller -> controller.getCurrentAnimation().animation().name())
                .orElse("");

			if (currentAnim.equals("hold")) AnimationManager.IS_HOLDING_STARTED = true;
		});

		Minecraft mc = Minecraft.getInstance();
		if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
			
			AnimationCache animCache = AnimationManager.CLIENT_CACHE;
			boolean isAiming = GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS && mc.screen == null;
			AnimationManager.IS_CLIENT_AIMING = isAiming;
			long currentTime = System.nanoTime() / 1000000L;
			float deltaTime;

			if (animCache.bone.LAST_UPDATE_NANO == 0) deltaTime = TIME_STEP;
			else {
				deltaTime = (currentTime - animCache.bone.LAST_UPDATE_NANO) / 1000.0f;
				deltaTime = Math.max(0, Math.min(deltaTime, 0.2f));
			}
			animCache.bone.LAST_UPDATE_NANO = currentTime;
			animCache.bone.ACCUMULATOR += deltaTime;

			float prevPrevPosX = animCache.bone.PREV_POS_X;
			float prevPrevPosY = animCache.bone.PREV_POS_Y;
			float prevPrevPosZ = animCache.bone.PREV_POS_Z;
			float prevPrevRotX = animCache.bone.PREV_ROT_X;
			float prevPrevRotY = animCache.bone.PREV_ROT_Y;
			float prevPrevRotZ = animCache.bone.PREV_ROT_Z;

			while (animCache.bone.ACCUMULATOR >= TIME_STEP) {
				updateAnimationState(stack, mc.player, model, animCache, controlName, isPlayerAiming(animCache.weapon));
				animCache.bone.ACCUMULATOR -= TIME_STEP;
			}
			float alpha = animCache.bone.ACCUMULATOR / TIME_STEP;
			renderAnimationState(model, animCache.bone, controlName, alpha, prevPrevPosX, prevPrevPosY, prevPrevPosZ, prevPrevRotX, prevPrevRotY, prevPrevRotZ);
			updateCamera(model, animCache.camera);

		} else resetControl(model, controlName);
	}

    /**
     * Reads the current camera bone rotation from the model.
     *
     * @param model the geo model
     * @param c the camera data to update
     */
    private static <T extends GeoItem> void updateCamera(GeoModel<T> model, AnimationCache.CameraData c) {

		CoreGeoBone camera = model.getAnimationProcessor().getBone("Cam");
		if (camera == null) return;

		c.CAMERA_BONE_ROT_X = -camera.getRotX();
		c.CAMERA_BONE_ROT_Y = -camera.getRotY();
		c.CAMERA_BONE_ROT_Z =  camera.getRotZ();
	}

    /**
     * Computes the next animation state for the gun bones, including shake, bobbing,
     * impact, and smooth interpolation.
     *
     * @param stack the gun stack
     * @param entity the entity holding the gun
     * @param model the geo model
     * @param animCache the animation cache
     * @param controlName the control bone name
     * @param isAiming whether the player is aiming
     */
    private static <T extends GeoItem> void updateAnimationState(ItemStack stack, Entity entity, GeoModel<T> model, AnimationCache animCache, String controlName, boolean isAiming) {

		AnimationCache.CameraData c = animCache.camera;
		AnimationCache.BoneData   b =   animCache.bone;
		AnimationCache.PlayerData p = animCache.player;

		Settings settings = Settings.from(stack);
		if (!settings.gunAnimationNoise()) b.SHAKE_INTENSITY = 0;

		float[][] values = EGun.from(stack).getOffsets();
    	boolean isRunning   = isPlayerRunning(animCache.weapon);

		CoreGeoBone control = model.getAnimationProcessor().getBone(controlName);
		
		b.SHAKE_TARGET = isAiming ? 0.125f : (isRunning ? 0.275f : 0.175f);
		b.SHAKE_INTENSITY += (b.SHAKE_TARGET - b.SHAKE_INTENSITY) * 0.05f;

		b.NOISE_TIMER += TIME_STEP;
		if (b.NOISE_TIMER >= TIME_STEP) {

			b.NOISE_TIMER = 0f;
			b.NOISE_PHASE += 0.01f;
		}
		float nX = BetterMath.noise(b.NOISE_PHASE, 0f) 		  * 1.5f;
		float nY = BetterMath.noise(b.NOISE_PHASE + 14.0f, 0f) * 1.5f;
		float nZ = BetterMath.noise(b.NOISE_PHASE + 21.0f, 0f) * 1.5f;

		float sx = nX * b.SHAKE_INTENSITY;
		float sy = nY * b.SHAKE_INTENSITY;
		float sz = nZ * b.SHAKE_INTENSITY;

		final float idlePosX = values[0][0], idlePosY = values[0][1], idlePosZ = values[0][2];
		final float runPosX  = values[1][0], runPosY  = values[1][1], runPosZ  = values[1][2];
		final float aimPosX  = values[2][0], aimPosY  = values[2][1], aimPosZ  = values[2][2];
		final float runRotX  = values[3][0], runRotY  = values[3][1], runRotZ  = values[3][2];

		float tPosX = (!isAiming && !isRunning) ? idlePosX : (isAiming ? aimPosX : runPosX);
		float tPosY = (!isAiming && !isRunning) ? idlePosY : (isAiming ? aimPosY : runPosY);
		float tPosZ = (!isAiming && !isRunning) ? idlePosZ : (isAiming ? aimPosZ : runPosZ);
		float tRotX = (!isAiming && !isRunning) ? 0.0f : (isAiming ? 0.0f : runRotX);
		float tRotY = (!isAiming && !isRunning) ? 0.0f : (isAiming ? 0.0f : runRotY);
		float tRotZ = (!isAiming && !isRunning) ? 0.0f : (isAiming ? 0.0f : runRotZ);

		float weight = EGun.process(stack, gun -> gun.getWeight(), 1.2f);

		float weightFactor = Math.max(0.0f, Math.min(2.0f, weight / BASE_WEIGHT));

		float shakePosMultiplier = weightFactor * 0.12f;
		float shakeRotMultiplier = weightFactor * 0.06f;

		tPosX += sx * shakePosMultiplier;
		tPosY += sy * shakePosMultiplier;
		tPosZ += sz * shakePosMultiplier;
		tRotX += sx * shakeRotMultiplier;
		tRotY += sy * shakeRotMultiplier;
		tRotZ += sz * shakeRotMultiplier;

		if (p.IMPACT_VELOCITY > 0.1f) {
			
			float normalizedImpact = Math.min(Math.max(p.IMPACT_VELOCITY, 5.7f + weightFactor * 1.1f) / 10.0f, 1.2f);
			float maxDuration = 0.25f + normalizedImpact * 1.0f;
			float t = animCache.player.IMPACT_TIMER / maxDuration;
			
			if (t <= 1.0f) {
				float impactDepth = normalizedImpact * normalizedImpact * normalizedImpact * 1.8f;
				float loweringDuration = 0.3f - (normalizedImpact * 0.15f);

				float loweringPhase   = Math.min(t / loweringDuration, 1.0f);
				float raisingDuration = maxDuration - loweringDuration;
				float raisingPhase    = Math.max((t - loweringDuration) / raisingDuration, 0.0f);
				
				float loweredAmount = loweringPhase * impactDepth;
				float easeOutQuint  = 1.0f - (float) Math.pow(1.0f - raisingPhase, 5);
				float raisedAmount  = easeOutQuint * impactDepth;
				
				float impactY = loweredAmount - raisedAmount;

				float rotationX = impactY * 0.10f;
				float rotationY = impactY * 0.05f;

				tRotX += rotationX * weightFactor * 0.8f;
				tRotY += rotationY * weightFactor * 0.8f;
				tPosY -= impactY   * weightFactor * 0.8f;

				float cameraOffsetY = -weightFactor * 1.6f - 0.1f;
    			c.CAMERA_OFFSET_Y += cameraOffsetY;

				if (!isFallen && entity.onGround()) {

					float impactFactor = Math.min(p.IMPACT_VELOCITY / 10.0f, 1.0f);
					float weightFactorLocal = Math.max(0.5f, weight / BASE_WEIGHT);
					
					float dynamicVolume = 0.2f + (impactFactor * 0.6f) + (weightFactorLocal * 0.2f);
					dynamicVolume = Math.min(Math.max(dynamicVolume, 0.2f), 0.7f) * 0.3f;
					
					float dynamicPitch = 0.6f + (impactFactor * 0.5f) + BetterMath.rand.nextFloat() * 0.37f * SOUND_FACTOR;
					dynamicPitch = Math.min(Math.max(dynamicPitch, 0.5f), 1.5f);
					
					entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), EGun.Sounds.FALL_STEP, SoundSource.PLAYERS, dynamicVolume, dynamicPitch, false);
					isFallen = true;
				}
			}
			if (!entity.onGround()) isFallen = false;
		} else isFallen = false;

		weightFactor = Math.max(0.3f, weight / BASE_WEIGHT);

		updateCamera(entity, animCache, weightFactor);
		updateBobbing(entity, animCache, weight, isAiming);
		
		float inertiaFactor    = 1.0f + (weightFactor * 0.3f);
		float bobbingIntensity = 1.0f + (weightFactor * 0.1f);
		
		tPosX += b.BOBBING_POS_X * bobbingIntensity / inertiaFactor;
		tPosY += b.BOBBING_POS_Y * bobbingIntensity / inertiaFactor;
		tPosZ += b.BOBBING_POS_Z * bobbingIntensity / inertiaFactor;
		tRotX += b.BOBBING_ROT_X * bobbingIntensity / inertiaFactor;
		tRotY += b.BOBBING_ROT_Y * bobbingIntensity / inertiaFactor;
		tRotZ += b.BOBBING_ROT_Z * bobbingIntensity / inertiaFactor;

		int currentPose = 0;
		if (isAiming) currentPose = 1;
		else if (isRunning) currentPose = 2;
		
		boolean isRunningTransition = (b.PREVIOUS_POSE == 0 && currentPose == 2) || 
		                              (b.PREVIOUS_POSE == 2 && currentPose == 0) ||
		                              (b.PREVIOUS_POSE == 1 && currentPose == 2) || 
		                              (b.PREVIOUS_POSE == 2 && currentPose == 1);
		
		weightFactor = 5.0f - Math.max(0.0f, Math.min(5.0f, weight / BASE_WEIGHT));
		float lambda = BASE_LAMBDA + weightFactor;
		
		if (isRunningTransition) lambda *= 0.65f;

		if (!b.IS_INITIALIZED) {
			b.PREV_POS_X = control.getPosX();
			b.PREV_POS_Y = control.getPosY();
			b.PREV_POS_Z = control.getPosZ();
			b.PREV_ROT_X = control.getRotX();
			b.PREV_ROT_Y = control.getRotY();
			b.PREV_ROT_Z = control.getRotZ();

			b.IS_INITIALIZED = true;
		}
		float deltaPosX = tPosX - b.PREV_POS_X;
		float deltaPosY = tPosY - b.PREV_POS_Y;
		float deltaPosZ = tPosZ - b.PREV_POS_Z;

		float baseFactor = 0.161f;
		float runningFactor = baseFactor * 0.35f;
		
		float translationToRotationFactor = baseFactor;
		
		if (isRunningTransition) translationToRotationFactor = runningFactor;

		float rotWeightFactor = Math.max(0.1f, Math.min(1.2f, weight / BASE_WEIGHT * 0.12f));
		translationToRotationFactor *= rotWeightFactor;

		float translationRotX = deltaPosY * translationToRotationFactor;
		float translationRotY = deltaPosX * translationToRotationFactor;
		float translationRotZ = deltaPosZ * translationToRotationFactor;

		tRotX += translationRotX;
		tRotY += translationRotY;
		tRotZ += translationRotZ;

		b.PREVIOUS_POSE = currentPose;

		float newPosX = BetterMath.damp(b.PREV_POS_X, tPosX, lambda, TIME_STEP);
		float newPosY = BetterMath.damp(b.PREV_POS_Y, tPosY, lambda, TIME_STEP);
		float newPosZ = BetterMath.damp(b.PREV_POS_Z, tPosZ, lambda, TIME_STEP);
		float newRotX = BetterMath.damp(b.PREV_ROT_X, tRotX, lambda, TIME_STEP);
		float newRotY = BetterMath.damp(b.PREV_ROT_Y, tRotY, lambda, TIME_STEP);
		float newRotZ = BetterMath.damp(b.PREV_ROT_Z, tRotZ, lambda, TIME_STEP);

		b.PREV_POS_X = newPosX;
		b.PREV_POS_Y = newPosY;
		b.PREV_POS_Z = newPosZ;
		b.PREV_ROT_X = newRotX;
		b.PREV_ROT_Y = newRotY;
		b.PREV_ROT_Z = newRotZ;
		b.LAMBDA	  = lambda;
	}

    /**
     * Interpolates between the previous and current bone transforms and writes
     * the result to the control bone.
     *
     * @param model the geo model
     * @param b the bone data
     * @param controlName the control bone name
     * @param alpha interpolation alpha
     * @param prevPrevPosX previous position X
     * @param prevPrevPosY previous position Y
     * @param prevPrevPosZ previous position Z
     * @param prevPrevRotX previous rotation X
     * @param prevPrevRotY previous rotation Y
     * @param prevPrevRotZ previous rotation Z
     */
    private static <T extends GeoItem> void renderAnimationState(GeoModel<T> model, AnimationCache.BoneData b, String controlName, float alpha,
			float prevPrevPosX, float prevPrevPosY, float prevPrevPosZ, float prevPrevRotX, float prevPrevRotY, float prevPrevRotZ) {

		CoreGeoBone control = model.getAnimationProcessor().getBone(controlName);

		float newPosX = BetterMath.interpolate(prevPrevPosX, b.PREV_POS_X, alpha);
		float newPosY = BetterMath.interpolate(prevPrevPosY, b.PREV_POS_Y, alpha);
		float newPosZ = BetterMath.interpolate(prevPrevPosZ, b.PREV_POS_Z, alpha);
		float newRotX = BetterMath.interpolate(prevPrevRotX, b.PREV_ROT_X, alpha);
		float newRotY = BetterMath.interpolate(prevPrevRotY, b.PREV_ROT_Y, alpha);
		float newRotZ = BetterMath.interpolate(prevPrevRotZ, b.PREV_ROT_Z, alpha);

		control.setPosX(newPosX);
		control.setPosY(newPosY);
		control.setPosZ(newPosZ);
		control.setRotX(newRotX);
		control.setRotY(newRotY);
		control.setRotZ(newRotZ);
	}

    /**
     * Updates camera shake and bobbing offsets based on player movement and fall impact.
     *
     * @param entity the entity
     * @param animCache the animation cache
     * @param weightFactor the weight factor
     */
    private static void updateCamera(Entity entity, AnimationCache animCache, float weightFactor) {

    	impactCamera(animCache, weightFactor);

    	float horizontalSpeed = animCache.player.VELOCITY;
    	float targetX, targetY, targetRoll;

    	if (horizontalSpeed < 0.01f || !entity.onGround()) {

        	targetX 	= 0.0f;
        	targetY 	= 0.0f;
        	targetRoll 	= 0.0f;

        	animCache.player.STEP_TIME  = 0;
        	animCache.bone.AMP_FACTOR = 1.0f;
        	animCache.player.MOVEMENT_START_TIME = 0L;

    	} else {

        	long currentTime = System.currentTimeMillis();
        	if (animCache.player.MOVEMENT_START_TIME == 0L) {
            	animCache.player.MOVEMENT_START_TIME = currentTime;
        	}
        	float movementDuration = (currentTime - animCache.player.MOVEMENT_START_TIME) / 1000.0f;

        	if (movementDuration < 0.7f) {
            	animCache.bone.AMP_FACTOR = 1.0f;

        	} else if (movementDuration < 3.0f) {

            	float transitionProgress = (movementDuration - 0.7f) / 1.0f;
            	animCache.bone.AMP_FACTOR = 1.0f - (transitionProgress * 0.34f);
        	} else {
            	animCache.bone.AMP_FACTOR = 0.47f;
        	}
        	float bobSpeed = 0.35f * horizontalSpeed;
        	animCache.player.STEP_TIME += (bobSpeed * 5.0f) * TIME_STEP;
        
        	if (animCache.player.STEP_TIME > 1000.0f) animCache.player.STEP_TIME -= 1000.0f;

        	float phase = animCache.player.STEP_TIME;
			float fixedWeight = weightFactor * 0.1f;

        	float baseAmpX 	  = 0.09f + fixedWeight;
        	float baseAmpY 	  = 0.09f + fixedWeight;
	        float baseAmpRoll = 0.90f + fixedWeight;

        	float ampX = baseAmpX * animCache.bone.AMP_FACTOR;
        	float ampY = baseAmpY * animCache.bone.AMP_FACTOR;
        	float ampRoll = baseAmpRoll;
        
        	targetX 	= (float) Math.sin(phase) * ampX;
        	targetY 	= (float) Math.abs(Math.cos(phase)) * ampY;
        	targetRoll 	= (float) Math.sin(phase) * ampRoll;
        	
        	playStepSound(entity, animCache.player, phase, weightFactor);
    	}

    	animCache.camera.CAMERA_OFFSET_X = targetX;
    	animCache.camera.CAMERA_OFFSET_Y = targetY;
    	animCache.camera.ROTATION_ROLL   = targetRoll;
	}
	
	/**
	 * Step sound offset in radians. Positive values play sound before impact, negative after.
	 * Adjust this value to fine-tune step sound timing.
	 * - 0: Exact impact time
	 * - π/16: Slightly before impact (≈11 degrees)
	 * - π/8: More before impact (≈22 degrees)
	 */
	private static final float STEP_SOUND_OFFSET = (float) Math.PI / 2.414f;

    /**
     * Plays a step sound when the step phase crosses the threshold, alternating
     * between light and heavy steps.
     *
     * @param entity the entity making the step
     * @param p the player animation data
     * @param phase current step phase
     * @param weight the gun weight
     */
	private static void playStepSound(Entity entity, AnimationCache.PlayerData p, float phase, float weight) {
		
		float stepThreshold = (float) (Math.PI / 2 - STEP_SOUND_OFFSET);
		float cycleDuration = (float) Math.PI;

		float currentStepPhase = phase % cycleDuration;
		float lastStepPhase = p.LAST_STEP_TIME % cycleDuration;

		boolean crossedStepBoundary = false;
		if (lastStepPhase > stepThreshold && currentStepPhase <= stepThreshold) crossedStepBoundary = true;
		
		if (crossedStepBoundary) {

			float totalPhase = phase % (2 * cycleDuration);
			boolean isLightStep = totalPhase <= cycleDuration;

			float pitch = 0.7f + BetterMath.rand.nextFloat() * 0.2f;

			float mul = entity.isSprinting() ? 0.81f : 0.64f;
			float level = -0.1f + Math.min(Math.max(0.35f, mul * weight * 0.3f), 1.25f) * 0.53f * SOUND_FACTOR;

			entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), (isLightStep ? EGun.Sounds.STEP_LIGHT : EGun.Sounds.STEP_HEAVY), SoundSource.PLAYERS, level, pitch, false);
		}
		p.LAST_STEP_TIME = phase;
	}

    /**
     * Updates weapon bobbing based on movement and impact, applying walking,
     * sprinting, and swing motions.
     *
     * @param entity the entity
     * @param animCache the animation cache
     * @param weight the gun weight
     * @param isAiming whether the player is aiming
     */
    private static void updateBobbing(Entity entity, AnimationCache animCache, float weight, boolean isAiming) {

    	float horizontalSpeed = animCache.player.VELOCITY;
    	boolean isRunning = isPlayerRunning(animCache.weapon);

    	if (horizontalSpeed < 0.01f || !entity.onGround()) {

        	float damping = 0.1f;
        	animCache.bone.BOBBING_POS_X *= (1 - damping);
        	animCache.bone.BOBBING_POS_Y *= (1 - damping);
        	animCache.bone.BOBBING_POS_Z *= (1 - damping);
        	animCache.bone.BOBBING_ROT_X *= (1 - damping);
        	animCache.bone.BOBBING_ROT_Y *= (1 - damping);
        	animCache.bone.BOBBING_ROT_Z *= (1 - damping);
        	animCache.bone.TRANSITION_PROGRESS *= (1 - damping);
        	
        	animCache.bone.BOBBING_TIME  = 0.0f;
        	animCache.bone.BOBBING_PHASE = 0.0f;
        	return;
    	}
    	animCache.bone.BOBBING_PHASE = animCache.player.STEP_TIME;

    	float transitionSpeed = isRunning ? 0.15f : 0.08f;
    	float targetProgress = isRunning ? 1.0f : 0.0f;
    	animCache.bone.TRANSITION_PROGRESS += (targetProgress - animCache.bone.TRANSITION_PROGRESS) * transitionSpeed;
    	animCache.bone.TRANSITION_PROGRESS = Math.max(0.0f, Math.min(1.0f, animCache.bone.TRANSITION_PROGRESS));

    	calculateWalkingBobbing(animCache.bone);
    	calculateSprintingBobbing(animCache.bone);

    	combineBobbingMovements(animCache.bone);
		if (!isAiming) combineSwingMotion(animCache, weight, entity.isCrouching());
    	
    	if (animCache.player.VERTICAL_VELOCITY < 0.0f && entity.onGround() && animCache.player.IS_GROUNDED == false) {
    		float impactIntensity = Math.abs(animCache.player.VERTICAL_VELOCITY) * 0.1f;
    		animCache.player.IMPACT_VELOCITY = Math.min(impactIntensity, 20.0f);
    		animCache.player.IMPACT_TIMER = 0.0f;
    	}
    	animCache.player.IS_GROUNDED = entity.onGround();
	}
	
    /**
     * Adds swing motion offsets to the bobbing data when transitioning between
     * walking and sprinting.
     *
     * @param animCache the animation cache
     * @param weight the gun weight
     * @param sneaking whether the entity is sneaking
     */
    private static void combineSwingMotion(AnimationCache animCache, float weight, boolean sneaking) {

    	float phase = animCache.bone.BOBBING_PHASE;
    	float weightFactor = Math.max(0.0f, Math.min(2.0f, weight / BASE_WEIGHT));
    	
    	float intensityY = 1.05f - (weightFactor * 0.15f);
    	float intensityX = 0.28f - (weightFactor * 0.03f);

		if (animCache.bone.TRANSITION_PROGRESS > 0.5f && !sneaking) {

    		intensityY *= 4.0f;
    		intensityX *= 6.2f;

    		float verticalRotation = (float) Math.sin(phase * 2.0f) * (0.09f * intensityY);
    		float horizontalRotation = (float) Math.sin(phase * 2.0f) * (0.18f * intensityX);
    
    		animCache.bone.BOBBING_ROT_X += verticalRotation;
    		animCache.bone.BOBBING_ROT_Z += horizontalRotation;

    		float tipTranslationX = (float) Math.sin(phase * 2.0f) * 0.02f * intensityX;
    		float tipTranslationY = (float) Math.sin(phase * 2.0f) * 0.03f * intensityY;

    		animCache.bone.BOBBING_POS_X += tipTranslationX;
    		animCache.bone.BOBBING_POS_Y += tipTranslationY;

    		float amplitude = 0.3f + weightFactor * 0.5f;
    		float speed = 0.6f;
    	
    		float theta = phase * speed;
    		float cosTheta = (float) Math.cos(theta);
    		float sinTheta = (float) Math.sin(theta);
    		float denominator = 1 + cosTheta * cosTheta;
    	
    		float x = amplitude * sinTheta / denominator;
    		float y = amplitude * sinTheta * cosTheta / denominator;
    		float z = amplitude * 0.2f * (float) Math.sin(theta * 2) / denominator;
    	
    		animCache.bone.BOBBING_POS_X += x;
    		animCache.bone.BOBBING_POS_Y += y;
    		animCache.bone.BOBBING_POS_Z += z;
    	
    		float dx = amplitude * (cosTheta * (1 + cosTheta * cosTheta) + 2 * sinTheta * sinTheta * cosTheta) / (denominator * denominator);
    		float dy = amplitude * (cosTheta * cosTheta - sinTheta * sinTheta) / denominator;
    		float dz = amplitude * 0.4f * (float) Math.cos(theta * 2) / denominator;
    	
    		float rotX = dy * 0.07f;
    		float rotZ = dx * 0.08f;
    		float rotY = dz * 0.06f;
    	
    		animCache.bone.BOBBING_ROT_X += rotX;
    		animCache.bone.BOBBING_ROT_Z += rotZ;
    		animCache.bone.BOBBING_ROT_Y += rotY;
		}
    	float uPhaseY = 1.3f * (phase % (float) Math.PI) / (float) Math.PI;
    	float easedPhaseY = BetterMath.easeInOutQuart(uPhaseY);
    	float curveY = (float) Math.sin(easedPhaseY * Math.PI);
    	float uMotionY = curveY * intensityY;
    	
    	animCache.bone.BOBBING_POS_Y -= uMotionY - 0.3f;
    	
    	float tiltIntensity = 0.02f + (weightFactor * 0.01f);
    	animCache.bone.BOBBING_ROT_X -= uMotionY * tiltIntensity;
    	
    	float cameraOffsetY = uMotionY * weightFactor * 0.33f;
    	animCache.camera.CAMERA_OFFSET_Y += cameraOffsetY;
	}

    /**
     * Calculates sinusoidal walking bobbing offsets.
     *
     * @param b the bone data
     */
    private static void calculateWalkingBobbing(AnimationCache.BoneData b) {

    	float phase = b.BOBBING_PHASE;
    	
    	b.WALKING_POS_X = (float) (Math.sin(phase) * 0.35f);
    	b.WALKING_POS_Y = (float) (Math.abs(Math.cos(phase)) * 0.25f) - 0.3f;
    	b.WALKING_POS_Z = (float) (Math.sin(phase * 0.5f) * 0.15f);
    	
    	b.WALKING_ROT_X = b.WALKING_POS_Y * 0.15f;
    	b.WALKING_ROT_Y = b.WALKING_POS_X * 0.10f;
    	b.WALKING_ROT_Z = b.WALKING_POS_X * 0.08f;
	}

    /**
     * Calculates parabolic sprinting bobbing offsets.
     *
     * @param b the bone data
     */
    private static void calculateSprintingBobbing(AnimationCache.BoneData b) {

    	float phase = b.BOBBING_PHASE;
    	
    	b.SPRINTING_POS_X = (float) (Math.sin(phase) * 0.20f);
    	b.SPRINTING_POS_Y = (float) (Math.abs(Math.cos(phase)) * 0.35f) - 0.3f;
    	
    	float uPhase = (phase % (float) Math.PI) / (float)Math.PI;
    	b.SPRINTING_POS_Z = (float) (-Math.pow(uPhase - 0.5f, 2) * 4 * 0.4f);
    	
    	b.SPRINTING_ROT_X = b.SPRINTING_POS_Y * 0.20f;
    	b.SPRINTING_ROT_Y = b.SPRINTING_POS_X  * 0.15f;
    	b.SPRINTING_ROT_Z = b.SPRINTING_POS_X  * 0.10f;
	}

    /**
     * Blends walking and sprinting bobbing movements using smoothstep interpolation.
     *
     * @param b the bone data
     */
    private static void combineBobbingMovements(AnimationCache.BoneData b) {

    	float t = b.TRANSITION_PROGRESS;
    	float easeT = t * t * (3.0f - 2.0f * t);

    	b.BOBBING_POS_X = BetterMath.interpolate(b.WALKING_POS_X, b.SPRINTING_POS_X, easeT);
    	b.BOBBING_POS_Y = BetterMath.interpolate(b.WALKING_POS_Y, b.SPRINTING_POS_Y, easeT);
    	b.BOBBING_POS_Z = BetterMath.interpolate(b.WALKING_POS_Z, b.SPRINTING_POS_Z, easeT);
    	b.BOBBING_ROT_X = BetterMath.interpolate(b.WALKING_ROT_X, b.SPRINTING_ROT_X, easeT);
    	b.BOBBING_ROT_Y = BetterMath.interpolate(b.WALKING_ROT_Y, b.SPRINTING_ROT_Y, easeT);
    	b.BOBBING_ROT_Z = BetterMath.interpolate(b.WALKING_ROT_Z, b.SPRINTING_ROT_Z, easeT);
	}
	
    /**
     * Applies camera shake based on fall impact velocity and damping.
     *
     * @param animCache the animation cache
     * @param weightFactor the weight factor
     */
    private static void impactCamera(AnimationCache animCache, float weightFactor) {

    	if (animCache.player.IMPACT_VELOCITY <= 0.1f) return;

    	float normalizedImpact = Math.min(animCache.player.IMPACT_VELOCITY / 15.0f, 1.0f);
    	float maxDuration = 0.2f + normalizedImpact * 1.2f;
    	float t = animCache.player.IMPACT_TIMER / maxDuration;

    	float impactIntensity = normalizedImpact * normalizedImpact * 1.8f * weightFactor * (1.0f - t * t);
    
    	float initialFrequency = 0.3f + normalizedImpact * 18.0f;
    	float impactFrequency = initialFrequency * (1.0f - t * 0.5f);	
    	float impactDamping = (float) Math.exp(-animCache.player.IMPACT_TIMER * (1.0f / maxDuration));
    
    	float easeOutCubic = 1.0f - (float) Math.pow(1.0f - t, 3);
    	float phase = animCache.player.IMPACT_TIMER * impactFrequency;
    	float impactAcceleration = (float) Math.sin(phase) * impactIntensity * impactDamping * easeOutCubic;
    
    	float velocityImpulse = impactAcceleration * 2.0f;
    	animCache.camera.VELOCITY_Y += velocityImpulse;
    
    	if (animCache.player.IMPACT_TIMER < 0.3f) animCache.camera.CAMERA_CURRENT_Y += impactAcceleration * 0.3f;
    	animCache.player.IMPACT_TIMER += TIME_STEP;
    
    	if (animCache.player.IMPACT_TIMER > maxDuration || impactDamping < 0.05f) {
    	    animCache.player.IMPACT_VELOCITY = 0.0f;
    	    animCache.player.IMPACT_TIMER = 0.0f;
    	}
	}
	
    /**
     * Resets a control bone to its default transform.
     * 
     * @param model the geo model
     * @param controlName the control bone name
     */
    private static <T extends GeoItem> void resetControl(GeoModel<T> model, String controlName) {

		CoreGeoBone control = model.getAnimationProcessor().getBone(controlName);

		control.setPosX(0f);
		control.setPosY(0f);
		control.setPosZ(0f);
		control.setRotX(0f);
		control.setRotY(0f);
		control.setRotZ(0f);
	}

    /**
     * Checks whether the player is currently aiming, including aim cooldown.
     *
     * @param w the weapon data
     * @return {@code true} if the player is aiming or finishing an aim
     */
    public static boolean isPlayerAiming(AnimationCache.WeaponData w) {

		boolean isAiming = AnimationManager.IS_CLIENT_AIMING;

		if (isAiming) w.setAimCooldown(3);
		return isAiming || w.getAimCooldown() > 0;
    }

    /**
     * Checks whether the player is currently running, including run cooldown.
     *
     * @param w the weapon data
     * @return {@code true} if the player is running or finishing a run
     */
    public static boolean isPlayerRunning(AnimationCache.WeaponData w) {

		boolean isRunning = AnimationManager.IS_CLIENT_RUNNING;

		if (isRunning) w.setRunCooldown(3);
		return isRunning || w.getRunCooldown() > 0;
    }

	/**
	 * Checks whether the given animation is currently running.
	 * 
	 * @param state the animation state
	 * @param animToCompare the animation name to compare
	 * @return {@code true} if the animation matches and is running
	 */
	@SuppressWarnings({ "unused", "rawtypes" })
	private static boolean isCurrentAnimation(AnimationState state, String animToCompare) {
		QueuedAnimation quequedAnim = state.getController().getCurrentAnimation();

		String currentAnim = quequedAnim != null && quequedAnim.animation() != null ? quequedAnim.animation().name() : "";
		return currentAnim.equals(animToCompare) && state.getController().getAnimationState() == State.RUNNING;
	}


}