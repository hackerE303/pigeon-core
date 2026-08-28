package software.hacker_E303.pigeon_core.geo.item.gun;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.hacker_E303.pigeon_core.PigeonCore;
import software.hacker_E303.pigeon_core.actions.IReload;
import software.hacker_E303.pigeon_core.actions.IShoot;
import software.hacker_E303.pigeon_core.client.gun.animation.AnimationManager;
import software.hacker_E303.pigeon_core.client.gun.animation.AnimationUtils;
import software.hacker_E303.pigeon_core.client.gun.renderer.GunRenderer;
import software.hacker_E303.pigeon_core.common.Settings;
import software.hacker_E303.pigeon_core.entity.EBullet;
import software.hacker_E303.pigeon_core.geo.IGeo;
import software.hacker_E303.pigeon_core.gun.gear.GunInventory;
import software.hacker_E303.pigeon_core.gun.gear.GunTracker;
import software.hacker_E303.pigeon_core.gun.goals.LightGunnerAiGoal;
import software.hacker_E303.pigeon_core.init.PigeUtils;
import software.hacker_E303.pigeon_core.item.EItem;
import software.hacker_E303.pigeon_core.item.EMagazine;
import software.hacker_E303.pigeon_core.main.event.network.RouterUtils;
import software.hacker_E303.pigeon_core.util.BetterData;
import software.hacker_E303.pigeon_core.util.BetterMath;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;
import software.hacker_E303.pigeon_core.util.misc.ShootUtils;

/**
 * Abstract base for GeoLib-enabled gun items.
 * <p>
 * Provides shared gun logic such as ammo, level, attachments, shooting,
 * reloading, and GeckoLib integration.
 */
public abstract class EGun extends EItem implements GeoItem, AutoCloseable, IGeo, IShoot, IReload {

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, true);
	private static final ThreadLocal<ItemStack> STACK_INSTANCE = new ThreadLocal<>();

	public final String ID = PigeUtils.pigeidFrom(this);
	private static short keyId;

	/**
	 * Generates a unique data key for gun state storage.
	 *
	 * @return the generated key
	 */
	protected static final String newKey() {
		return "gun_data_" + keyId++;
	}

	/**
	 * Data keys for persistent gun state.
	 */
	public static final String GECKO	   = newKey();
	public static final String AMMO		   = newKey();
	public static final String LEVEL	   = newKey();
	public static final String ATTACHMENTS = newKey();
	public static final String MOUSE_RIGHT = newKey();
	public static final String MOUSE_LEFT  = newKey();

	/**
	 * GeckoLib animation controller names.
	 */
	public static final String EJECTION_CONTROLLER = "shellsController";
	public static final String MAIN_CONTROLLER     = "animationHandler";

    /**
     * Resource location for gun animations.
     */
    public ResourceLocation ANIMS_LOCATION   = Location.create(Path.GEO_ANIMS.GUNS, this.ID).from(PigeUtils.modidFrom(this));

    /**
     * Resource location for gun model.
     */
    public ResourceLocation MODEL_LOCATION   = Location.create(Path.GEO_MODEL.GUNS, this.ID).from(PigeUtils.modidFrom(this));

    /**
     * @return the animations resource location
     */
    @Override
    public ResourceLocation getAnimsLocation() {
        return this.ANIMS_LOCATION;
    }

    /**
     * @return the model resource location
     */
    @Override
    public ResourceLocation getModelLocation() {
        return this.MODEL_LOCATION;
    }

    /**
     * Sets the animations location.
     * 
     * @param location the animations location
     */
    @Override
    public void setAnims(Location location) {
        this.ANIMS_LOCATION = location.from(PigeUtils.modidFrom(this));
    }

    /**
     * Sets the model location.
     * 
     * @param location the model location
     */
    @Override
    public void setModel(Location location) {
        this.MODEL_LOCATION = location.from(PigeUtils.modidFrom(this));
    }

	/**
	 * Creates a new gun item.
	 * 
	 * @param durability the base durability
	 */
	public EGun(int durability) {
		super(Rarity.COMMON, durability);
		SingletonGeoAnimatable.registerSyncedAnimatable(this);
	}

	/**
	 * Runs an action with this gun set as the current stack context.
	 * 
	 * @param stack the gun stack
	 * @param action the action to run
	 */
	public static void process(ItemStack stack, Consumer<EGun> action) {
        if (!isValid(stack) || !(stack.getItem() instanceof EGun gun)) return;
		ItemStack old = STACK_INSTANCE.get();

        try {
            STACK_INSTANCE.set(stack);
            action.accept(gun);
        } finally {
          	if (old != null) STACK_INSTANCE.set(old);
        	else STACK_INSTANCE.remove();
        }
    }

	/**
	 * Runs a function with this gun set as the current stack context.
	 * 
	 * @param stack the gun stack
	 * @param action the function to run
	 * @param defaultValue the value to return if the stack is invalid
	 * @return the function result, or {@code defaultValue}
	 */
	public static <V> V process(ItemStack stack, Function<EGun, V> action, V defaultValue) {
    	if (!isValid(stack) || !(stack.getItem() instanceof EGun gun)) return defaultValue;
    	ItemStack old = STACK_INSTANCE.get();

    	try {
        	STACK_INSTANCE.set(stack);
        	return action.apply(gun);
    	} finally {
        	if (old != null) STACK_INSTANCE.set(old);
        	else STACK_INSTANCE.remove();
		}
	}

	/**
	 * @return the currently processed stack
	 */
	public ItemStack readCurrentStack() {
		return STACK_INSTANCE.get();
	}

	/**
	 * Sets the currently processed stack.
	 * 
	 * @param stack the stack to set
	 */
	public void writeCurrentStack(ItemStack stack) {
		STACK_INSTANCE.set(stack);
	}

	/**
	 * @return the GeckoLib entity id
	 */
	public long getGeckoId() {
        return getLongData(STACK_INSTANCE.get(), GECKO, 0L) -1;
	}

	/**
	 * Sets the GeckoLib entity id.
	 * 
	 * @param value the entity id
	 */
	public void setGeckoId(long value) {
		setLongData(STACK_INSTANCE.get(), GECKO, value + 1);
	}

	/**
	 * @return {@code true} if this gun has a GeckoLib id assigned
	 */
	public boolean hasGeckoId() {
		ItemStack stack = STACK_INSTANCE.get();
		return getGeckoId() != -1 && (BetterData.hasData(stack, GECKO) || GeoItem.getId(stack) < 575200);
	}

	/**
	 * @return the current ammo count
	 */
	public int getAmmo() {
        return getIntegerData(STACK_INSTANCE.get(), AMMO, 0);
	}

	/**
	 * Sets the current ammo count.
	 * 
	 * @param value the ammo count
	 */
	public void setAmmo(int value) {
		setIntegerData(STACK_INSTANCE.get(), AMMO, value);
	}

	/**
	 * Decreases ammo by one, clamped at zero.
	 */
	public void decreaseAmmo() {
		ItemStack stack = STACK_INSTANCE.get();
		setIntegerData(stack, AMMO, Math.max(getIntegerData(stack, AMMO, 0) - 1, 0));
	}

	/**
	 * Increases ammo by one.
	 */
	public void increaseAmmo() {
		ItemStack stack = STACK_INSTANCE.get();
		setIntegerData(stack, AMMO, getIntegerData(stack, AMMO, 0) + 1);
	}

	/**
	 * @return the current level (1-based)
	 */
	public int getLevel() {
		return getIntegerData(STACK_INSTANCE.get(), LEVEL, 0) + 1;
	}

	/**
	 * Sets the level (1-based).
	 * 
	 * @param value the level
	 */
	public void setLevel(int value) {
		setIntegerData(STACK_INSTANCE.get(), LEVEL, value - 1);
	}

	/**
	 * Decreases level by one.
	 */
	public void decreaseLevel() {
		ItemStack stack = STACK_INSTANCE.get();
		setIntegerData(stack, LEVEL, getIntegerData(stack, LEVEL, 0) - 1);
	}

	/**
	 * Increases level by one.
	 */
	public void increaseLevel() {
		ItemStack stack = STACK_INSTANCE.get();
		setIntegerData(stack, LEVEL, getIntegerData(stack, LEVEL, 0) + 1);
	}

	/**
	 * @param type the attachment slot type
	 * @return the attachment in the given slot
	 */
	public ItemStack getAttachment(EAttachment.Type type) {
    	return getInventory().getStackInSlot(type.ordinal());
	}

	/**
	 * Loads an attachment into its matching slot.
	 * 
	 * @param stack the attachment stack
	 */
	public void loadAttachment(ItemStack stack) {

    	if (stack.getItem() instanceof EAttachment attachment) {
        	EAttachment.Type type = attachment.getType();

        	int slot = type.ordinal();
        	getInventory().setStackInSlot(slot, stack.copy()); 
    	}
	}

	/**
	 * Removes the attachment from the given slot.
	 * 
	 * @param type the attachment slot type
	 */
	public void unloadAttachment(EAttachment.Type type) {
    	getInventory().setStackInSlot(type.ordinal(), ItemStack.EMPTY);
	}

	/**
	 * @return {@code true} if this gun can fire
	 */
	public boolean canShoot() {
		return getAmmo() > 0 && getCooldown() == 0 && (shouldShoot() || isLeftPressed()) && (getMode().isAuto() || !needsTriggerReset());
	}

	/**
	 * @param entity the entity
	 * @return {@code true} if this gun can reload
	 */
	public boolean canReload(Entity entity) {
		return getAmmo() < getMaxAmmo() && hasMagazine(entity) && BetterData.getProvider(entity).getBoolean("askingForReload");
	}

	/**
	 * @return {@code true} if the gun should fire
	 */
	public boolean shouldShoot() {
		return BetterData.getCap(Data.CAP, STACK_INSTANCE.get(), action -> action.shouldShoot());
	}

	/**
	 * Sets whether the gun should fire.
	 * 
	 * @param value {@code true} to fire
	 */
	public void shouldShoot(boolean value) {
		BetterData.setCap(Data.CAP, STACK_INSTANCE.get(), action -> action.shouldShoot(value));
	}

	/**
	 * @return {@code true} if the gun has just fired
	 */
	public boolean hasJustShooted() {
		return BetterData.getCap(Data.CAP, STACK_INSTANCE.get(), action -> action.hasJustShooted());
	}

	/**
	 * Sets whether the gun has just fired.
	 * 
	 * @param value {@code true} if just fired
	 */
	public void hasJustShooted(boolean value) {
		BetterData.setCap(Data.CAP, STACK_INSTANCE.get(), action -> action.hasJustShooted(value));
	}

	/**
	 * @return the current inaccuracy
	 */
	public float getInaccuracy() {
		return BetterData.getCap(Data.CAP, STACK_INSTANCE.get(), action -> action.getInaccuracy());
	}

	/**
	 * Increases inaccuracy by the given value.
	 * 
	 * @param value the inaccuracy to add
	 */
	public void increaseInaccuracy(float value) {
		BetterData.setCap(Data.CAP, STACK_INSTANCE.get(), action -> action.increaseInaccuracy(value));
	}

	/**
	 * @return the current cooldown in ticks
	 */
	public int getCooldown() {
		return BetterData.getCap(Data.CAP, STACK_INSTANCE.get(), action -> action.getCooldown());
	}

	/**
	 * Sets the cooldown in ticks.
	 * 
	 * @param value the cooldown
	 */
	public void setCooldown(int value) {
		BetterData.setCap(Data.CAP, STACK_INSTANCE.get(), action -> action.setCooldown(value));
	}

	/**
	 * Decreases cooldown by one tick, clamped at zero.
	 */
	public void decreaseCooldown() {
		BetterData.setCap(Data.CAP, STACK_INSTANCE.get(), action -> action.decreaseCooldown());
	}

	/**
	 * @return the current delay in ticks
	 */
	public int getDelay() {
		return BetterData.getCap(Data.CAP, STACK_INSTANCE.get(), action -> action.getDelay());
	}

	/**
	 * Sets the delay in ticks.
	 * 
	 * @param value the delay
	 */
	public void setDelay(int value) {
		BetterData.setCap(Data.CAP, STACK_INSTANCE.get(), action -> action.setDelay(value));
	}

	/**
	 * Increases delay by one tick.
	 */
	public void increaseDelay() {
		BetterData.setCap(Data.CAP, STACK_INSTANCE.get(), action -> action.increaseDelay());
	}

	/**
	 * @return {@code true} if the right mouse button is pressed
	 */
	public boolean isRightPressed() {
		return getBooleanData(STACK_INSTANCE.get(), MOUSE_RIGHT, false);
	}

	/**
	 * Sets whether the right mouse button is pressed.
	 * 
	 * @param value {@code true} if pressed
	 */
	public void isRightPressed(boolean value) {
		setBooleanData(STACK_INSTANCE.get(), MOUSE_RIGHT, value);
	}

	/**
	 * @return {@code true} if the left mouse button is pressed
	 */
	public boolean isLeftPressed() {
		return getBooleanData(STACK_INSTANCE.get(), MOUSE_LEFT, false);
	}

	/**
	 * Sets whether the left mouse button is pressed.
	 * 
	 * @param value {@code true} if pressed
	 */
	public void isLeftPressed(boolean value) {
		setBooleanData(STACK_INSTANCE.get(), MOUSE_LEFT, value);
	}

	/**
	 * @return the default fire mode
	 */
	public Mode getDefaultMode() {
		return Mode.AUTOMATIC;
	}

	/**
	 * @return {@code true} if this gun supports mode switching
	 */
	public boolean canSwitchMode() {
		return false;
	}

	/**
	 * @return the current fire mode
	 */
	public Mode getMode() {
		return BetterData.getCap(Data.CAP, STACK_INSTANCE.get(), action -> action.getMode());
	}

	/**
	 * Sets the fire mode.
	 * 
	 * @param mode the mode
	 */
	public void setMode(Mode mode) {
		BetterData.setCap(Data.CAP, STACK_INSTANCE.get(), action -> action.setMode(mode));
	}

	/**
	 * Toggles between automatic and semi-automatic mode.
	 */
	public void switchMode() {
		BetterData.setCap(Data.CAP, STACK_INSTANCE.get(), action -> action.switchMode());
	}

	/**
	 * Ticks the gun state, decreasing cooldown and inaccuracy.
	 */
	public void tick() {
		BetterData.setCap(Data.CAP, STACK_INSTANCE.get(), action -> action.tick());
	}

	/**
	 * @return {@code true} if the trigger needs to be reset
	 */
	private boolean needsTriggerReset() {
		return BetterData.getCap(Data.CAP, STACK_INSTANCE.get(), action -> action.needsTriggerReset());
	}

	/**
	 * Sets whether the trigger needs to be reset.
	 *
	 * @param value {@code true} if reset is needed
	 */
	private void needsTriggerReset(boolean value) {
		BetterData.setCap(Data.CAP, STACK_INSTANCE.get(), action -> action.needsTriggerReset(value));
	}

	/**
	 * @return {@code true} if the trigger sound needs to play
	 */
	private boolean needsTriggerSound() {
		return BetterData.getCap(Data.CAP, STACK_INSTANCE.get(), action -> action.needsTriggerSound());
	}

	/**
	 * Sets whether the trigger sound needs to play.
	 *
	 * @param value {@code true} if sound should play
	 */
	private void needsTriggerSound(boolean value) {
		BetterData.setCap(Data.CAP, STACK_INSTANCE.get(), action -> action.needsTriggerSound(value));
	}

	/**
	 * Checks whether an item stack is a valid gun stack.
	 *
	 * @param stack the stack to check
	 * @return {@code true} if the stack is non-null and non-empty
	 */
	private static boolean isValid(ItemStack stack) {
		return stack != null && !stack.isEmpty();
	}

	/**
	 * @return the gun's attachment inventory
	 */
	public GunInventory getInventory() {
    	return new GunInventory(STACK_INSTANCE.get());
	}



	/**
	 * Reads a long value from persistent gun data.
	 *
	 * @param stack the item stack
	 * @param key the data key
	 * @param defaultValue the value to return if the key is absent
	 * @return the stored value, or {@code defaultValue}
	 */
	private static long getLongData(ItemStack stack, String key, long defaultValue) {
		if (!isValid(stack)) return defaultValue;
		return BetterData.getData(stack, key, defaultValue);
	}

	/**
	 * Writes a long value to persistent gun data.
	 *
	 * @param stack the item stack
	 * @param key the data key
	 * @param value the value to store
	 */
	private static void setLongData(ItemStack stack, String key, long value) {
		if (isValid(stack)) BetterData.setData(stack, key, value);
	}

	/**
	 * Reads an integer value from persistent gun data.
	 *
	 * @param stack the item stack
	 * @param key the data key
	 * @param defaultValue the value to return if the key is absent
	 * @return the stored value, or {@code defaultValue}
	 */
	private static int getIntegerData(ItemStack stack, String key, int defaultValue) {
		if (!isValid(stack)) return defaultValue;
		return BetterData.getData(stack, key, defaultValue);
	}

	/**
	 * Writes an integer value to persistent gun data.
	 *
	 * @param stack the item stack
	 * @param key the data key
	 * @param value the value to store
	 */
	private static void setIntegerData(ItemStack stack, String key, int value) {
		if (isValid(stack)) BetterData.setData(stack, key, value);
	}

	/**
	 * Reads a boolean value from persistent gun data.
	 *
	 * @param stack the item stack
	 * @param key the data key
	 * @param defaultValue the value to return if the key is absent
	 * @return the stored value, or {@code defaultValue}
	 */
	private static boolean getBooleanData(ItemStack stack, String key, boolean defaultValue) {
		if (!isValid(stack)) return defaultValue;
		return BetterData.getData(stack, key, defaultValue);
	}

	/**
	 * Writes a boolean value to persistent gun data.
	 *
	 * @param stack the item stack
	 * @param key the data key
	 * @param value the value to store
	 */
	private static void setBooleanData(ItemStack stack, String key, boolean value) {
		if (isValid(stack)) BetterData.setData(stack, key, value);
	}

	/**
	 * @return the total damage after level scaling
	 */
	public float getTotalDamage() {

		Properties properties = getProperties();

		double baseDamage  = properties.baseDamage();
		double totalDamage = baseDamage + baseDamage * properties.growthRate() * (getLevel() - 1);

		return Math.round(totalDamage * 100.0) / 100.0f;
	}


	private String magazineName;
	
    /**
     * @return the localized magazine name
     */
    public String getMagazineName() {

        if (magazineName == null) magazineName = new ItemStack(getMagazine()).getHoverName()
			.getString().replace(" Box", "");
        return magazineName;
    }

	/**
	 * @return the maximum ammo capacity, including attachment modifiers
	 */
	public int getMaxAmmo() {

		ItemStack stack = getAttachment(EAttachment.Type.MAGAZINE);
		int capacityAddition = 0;

		if (stack.getItem() instanceof EAttachment magazine)
	    	capacityAddition = magazine != null ? (int) magazine.getModifier() : 0;

		return getBaseMaxAmmo() + capacityAddition;
	}

	/**
	 * @return the aim strength, including scope modifiers
	 */
	public float getAimStrength() {

		ItemStack stack = getAttachment(EAttachment.Type.SCOPE);
		float aimStrengthAddition = 0.0f;

		if (stack.getItem() instanceof EAttachment scope)
			aimStrengthAddition = scope != null ? (float) scope.getModifier() : 0.0f;

		return getBaseAimStrength() + aimStrengthAddition;
	}

	/**
	 * @return the total weight, including all attachment modifiers
	 */
	public float getWeight() {
    	float weight = getBaseWeight();
    	for (EAttachment.Type type : EAttachment.Type.values()) {

    	    ItemStack stack = getAttachment(type);
    	    if (stack.getItem() instanceof EAttachment attached) weight += (float) attached.getModifier();
    	}
    	return weight;
	}


	/**
	 * @return the magazine item
	 */
	public abstract EMagazine getMagazine();

	/**
	 * @param level the level
	 * @return the bullet to spawn
	 */
	public abstract EBullet getBullet(Level level);

	/**
	 * @return the base maximum ammo capacity
	 */
	protected abstract int getBaseMaxAmmo();

	/**
	 * @return the base aim strength
	 */
	protected abstract float getBaseAimStrength();

	/**
	 * @return the base weight
	 */
	protected abstract float getBaseWeight();

	/**
	 * @return the gun offsets for idle, run, aim, and run rotation
	 */
	public abstract float[][] getOffsets();

	/**
	 * @return the light part bone names
	 */
	public abstract String[] getLightParts();

	/**
	 * @return the tooltip lines
	 */
	public abstract String[] getTooltip();

	/**
	 * Fires the gun using the given shoot action.
	 * 
	 * @param level the level
	 * @param shooter the shooter
	 * @param action the shoot action
	 */
	public void shoot(Level level, Entity shooter, IShoot.ShootAction action) {

		ItemStack stack = STACK_INSTANCE.get();
		shouldShoot(true);

		if (getDelay() < action.delay() + 1) {
			increaseDelay();

			if (getDelay() == action.delay()) setDelay(0);
			else return;
		}
		for (int i = 0; i < action.count(); i++)
			getBullet(level).shoot(shooter, getTooltip()[3], getTotalDamage(), action.speed(),
				getProperties().baseInaccuracy() + getInaccuracy(), action.power(), action.power() * 2.4);

		shouldShoot(false);
		hasJustShooted(true);
		needsTriggerReset(true);

		decreaseAmmo();
		setCooldown(action.cooldown());
		increaseInaccuracy(action.inaccuracy());

		RouterUtils.Geckolib.playAnimation(stack, level, shooter, MAIN_CONTROLLER, "shoot");
		if (this.getShellsCount() > 0) {

			String ejectAnim = ShootUtils.getNextEjectAnimation(stack, level, this.getShellsCount());
			RouterUtils.Geckolib.playAnimation(stack, level, shooter, EJECTION_CONTROLLER + ejectAnim.replace("shell_ejection", ""), ejectAnim);

		}
		BlockPos pos = BlockPos.containing(shooter.getX() + shooter.getLookAngle().x, shooter.getY(), shooter.getZ() + shooter.getLookAngle().z);
		SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("pigeon_core", this.ID + "_shoot"));

		float pitch = 0.9f + level.random.nextFloat() * 0.2f;
		level.playSound(null, pos, sound, SoundSource.PLAYERS, 0.89f, pitch);
	}

	/**
	 * Reloads the gun from the player's inventory.
	 * 
	 * @param entity the entity reloading
	 */
	public void reload(Entity entity) {

		if (!(entity instanceof ServerPlayer player)) return;
		if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
			setAmmo(getMaxAmmo());
			return;
		}
		AtomicReference<IItemHandler> iitemhandlerref = new AtomicReference<>();
		player.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(iitemhandlerref::set);

		for (int index = 0; index < iitemhandlerref.get().getSlots(); index++) {
			ItemStack magazine = iitemhandlerref.get().getStackInSlot(index);

			if (getMagazine() != magazine.getItem()) continue;

			int ammoBefore = getAmmo();
			int spaceLeft = getMaxAmmo() - ammoBefore;
			int ammoAvailable = magazine.getMaxDamage() - magazine.getDamageValue();
			int ammoToReload = Math.min(spaceLeft, ammoAvailable);
			int ammoNow = ammoBefore + ammoToReload;

			ItemStack result = magazine.copy();
			if (result.hurt(ammoToReload, RandomSource.create(), null)) {
				result.shrink(1);
				result.setDamageValue(0);
			}
			final int slotId = index;
			BetterData.setCap(ForgeCapabilities.ITEM_HANDLER, player, action -> {
				if (action instanceof IItemHandlerModifiable handlerModifiable) handlerModifiable.setStackInSlot(slotId, result);
			});
			setAmmo(ammoNow);
			break;
		}
	}

	/**
	 * @param entity the entity
	 * @return {@code true} if the entity has the required magazine
	 */
	public boolean hasMagazine(Entity entity) {	

    	if (!(entity instanceof ServerPlayer player)) return false;
		if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) return true;

    	Item magazine = getMagazine();
    	for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
    	    ItemStack stack = player.getInventory().getItem(i);
    	    if (stack.getItem() == magazine) return true;
    	}
    	return false;
	}

	/**
	 * Initializes the gun for holding by an entity.
	 * 
	 * @param entity the entity holding the gun
	 */
	public void startHolding(Entity entity) {

		Level level = entity.level();
		if (!(level instanceof ServerLevel serverLevel)) return;

		ItemStack stack = readCurrentStack();

		setMode(getDefaultMode());
		setGeckoId(GeoItem.getOrAssignId(stack, serverLevel));

		RouterUtils.Internal.startHoldingGun(stack, entity);
		RouterUtils.Geckolib.playLocalAnimation(stack, level, entity, MAIN_CONTROLLER, "hold");

		setCooldown( 10);
		isRightPressed(false);

		setBooleanData(stack, HOLDING, true);
	}

	/**
	 * Cleans up when the entity stops holding the gun.
	 * 
	 * @param entity the entity
	 */
	public void stopHolding(Entity entity) {

		ItemStack stack = readCurrentStack();

		AnimationManager.IS_CLIENT_HOLDING_GUN = false;
		AnimationManager.IS_CLIENT_GUN_VISIBLE = false;

		GunTracker.removeGunHolder(stack);
		RouterUtils.Geckolib.playAnimation(stack, entity.level(), entity, MAIN_CONTROLLER, "idle");

		setBooleanData(stack, HOLDING, false);
	}

	/**
	 * Equips a gun onto a mob and configures its AI.
	 * 
	 * @param gun the gun
	 * @param level the level
	 * @param mob the mob
	 * @param dropChance the drop chance
	 * @param affinity the accuracy affinity
	 * @param speed the movement speed
	 * @param range the engagement range
	 * @param precise unused
	 */
	public static <T extends EGun> void equip(T gun, Level level, Mob mob, double dropChance, double affinity, double speed, double range, boolean precise) {
			if (level.isClientSide || mob == null || gun == null) return;

            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(gun));
            mob.setDropChance(EquipmentSlot.MAINHAND, BetterMath.rand.nextFloat() * (float) BetterMath.clamp(dropChance));

            mob.goalSelector.addGoal(0, new LightGunnerAiGoal(mob, BetterMath.clamp(affinity), speed, range));
			//(precise ? new HeavyGunnerAiGoal(mob, PigeMath.clamp(affinity), speed, range) :  new LightGunnerAiGoal(mob, PigeMath.clamp(affinity), speed, range)));
	}

	/**
	 * @param stack the item stack
	 * @return the gun, or {@code null}
	 */
	public static EGun from(ItemStack stack) {
		if (stack.getItem() instanceof EGun gun) return gun;
		return null;
	}

	/**
	 * @param animatable the GeoItem
	 * @return the gun, or {@code null}
	 */
	public static EGun from(GeoItem animatable) {
		if (animatable instanceof EGun gun) return gun;
		return null;
	}

	/**
	 * Cleans up the thread-local stack instance.
	 */
	@Override
	public void close() {
        STACK_INSTANCE.remove(); 
	}

    /**
     * Fire modes for guns.
     */
    public static enum Mode {
        /**
         * Fires continuously while the trigger is held.
         */
        AUTOMATIC,
        /**
         * Fires once per trigger press.
         */
        SEMI_AUTOMATIC;
    
    	/**
    	 * @return {@code true} if this mode is automatic
    	 */
    	public boolean isAuto() {
        	return this == AUTOMATIC;
    	}
    }

    /**
     * Sound events used by guns throughout the mod.
     */
    public static class Sounds {

		public static final SoundEvent HOLD = PigeonCore.getSound("pigeon_core", "gun.hold");
		public static final SoundEvent TRIGGER = PigeonCore.getSound("pigeon_core", "gun.trigger");
		public static final SoundEvent RICOCHET = PigeonCore.getSound("pigeon_core", "ricochet");
		public static final SoundEvent STEP_LIGHT = PigeonCore.getSound("pigeon_core", "gun.step.light");
		public static final SoundEvent STEP_HEAVY = PigeonCore.getSound("pigeon_core", "gun.step.heavy");
		public static final SoundEvent FALL_STEP = PigeonCore.getSound("pigeon_core", "gun.step.fall");
	}
	
    /**
     * Persistent gun state stored via capabilities.
     */
	private interface IData {
	
    	Mode getMode();
    	void setMode(EGun.Mode mode);
    	void switchMode();

    	int getCooldown();
    	void setCooldown(int value);
		void decreaseCooldown();

    	int getDelay();
    	void setDelay(int value);
		void increaseDelay();

    	float getInaccuracy();
    	void increaseInaccuracy(float value);
		void decreaseInaccuracy();

		boolean needsTriggerReset();
		void needsTriggerReset(boolean value);

		boolean needsTriggerSound();
		void needsTriggerSound(boolean value);

		boolean shouldShoot();
		void shouldShoot(boolean value);

		boolean hasJustShooted();
		void hasJustShooted(boolean value);

		default void tick() {
			decreaseCooldown();
			decreaseInaccuracy();
		}
	}

    /**
     * Default implementation of {@link IData}.
     */
	private static class Data implements IData {

		public static final Capability<IData> CAP = CapabilityManager.get(new CapabilityToken<>() {});

		private int cooldown = 0;
		private int delay	 = 0;

		private float inaccuracy = 0;
		private Mode mode = Mode.AUTOMATIC;

		private boolean trigger = false;
		private boolean fired   = false;
		private boolean shoot   = false;
		private boolean just    = false;

		@Override
		public Mode getMode() {
			return mode;
		}

		@Override
		public void setMode(Mode mode) {
			this.mode = mode;
		}

		@Override
		public void switchMode() {
			if (mode.isAuto()) mode = Mode.SEMI_AUTOMATIC;
			else mode = Mode.AUTOMATIC;
		}

		@Override
		public int getCooldown() {
			return cooldown;
		}

		@Override
		public void setCooldown(int value) {
			cooldown = value;
		}

		@Override
		public void decreaseCooldown() {
			if (cooldown > 0) cooldown--;
		}

		@Override
		public int getDelay() {
			return delay;
		}

		@Override
		public void setDelay(int value) {
			delay = value;
		}

		@Override
		public void increaseDelay() {
			delay++;
		}

		@Override
		public float getInaccuracy() {
			return inaccuracy;
		}

		@Override
		public void increaseInaccuracy(float value) {
			inaccuracy += value;
		}

		@Override
		public void decreaseInaccuracy() {
			if (inaccuracy > 0) inaccuracy *= 0.8;
		}

		@Override
		public boolean needsTriggerReset() {
			return fired;
		}

		@Override
		public void needsTriggerReset(boolean value) {
			fired = value;
		}

		@Override
		public boolean needsTriggerSound() {
			return trigger;
		}

		@Override
		public void needsTriggerSound(boolean value) {
			trigger = value;
		}

		@Override
		public boolean shouldShoot() {
			return shoot;
		}

		@Override
		public void shouldShoot(boolean value) {
			shoot = value;
		}

		@Override
		public boolean hasJustShooted() {
			return just;
		}

		@Override
		public void hasJustShooted(boolean value) {
			just = value;
		}
	}

    /**
     * Immutable record of base gun properties including damage, inaccuracy, recoil,
     * and spark size bounds.
     */
    public record Properties(float baseDamage, float growthRate, float baseInaccuracy, float recoil, float minSparkSize, float maxSparkSize) {
    }

	/**
	 * @return the gun properties record
	 */
	public abstract Properties getProperties();

    /**
     * Capability provider that exposes {@link IData} for an {@link EGun} item stack.
     */
    public static class DataProvider implements ICapabilityProvider {

		private final IData data = new Data();
		private final LazyOptional<IData> optional = LazyOptional.of(() -> data);

		/**
		 * @param cap the capability
		 * @param side the side
		 * @return the capability instance, or empty
		 */
		@Override
		public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
			return cap == Data.CAP ? optional.cast() : LazyOptional.empty();
		}
	}

	/**
	 * Registers GeckoLib animation controllers for this gun.
	 */
	@Override
	public final void registerControllers(AnimatableManager.ControllerRegistrar data) {
		
		AnimationUtils.setupMainController(this, data);
		if (canSwitchMode()) AnimationUtils.setupModeController(this, data);
		if (this.getShellsCount() > 0) AnimationUtils.setupEjectionController(this, data);
	}

	/**
	 * Guns always play animations even when the game is paused.
	 */
	@Override
	public final boolean shouldPlayAnimsWhileGamePaused() {
		return true;
	}

	/**
	 * Guns do not trigger a reequip animation when swapped.
	 */
	@Override
	public final boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return false;
	}

	/**
	 * Guns are not enchantable.
	 */
	@Override
	public final boolean isEnchantable(ItemStack stack) {
		return false;
	}

	/**
	 * Guns have no mining speed.
	 */
	@Override
	public final float getDestroySpeed(ItemStack stack, BlockState state) {
		return 0F;
	}

	/**
	 * @param stack the item stack
	 * @return {@code true} if the durability/ammo bar should be visible
	 */
	@Override
	public final boolean isBarVisible(ItemStack stack) {

		Settings settings = Settings.from(this);
		return settings.gunAmmoBarVisible() || settings.gunDurability();
	}

	/**
	 * @param stack the item stack
	 * @return the bar width based on ammo or durability
	 */
	@Override
	public final int getBarWidth(ItemStack stack) {

		return EGun.process(stack, gun -> {

			Settings settings = Settings.from(this);
			if (settings.gunDurability()) {

				return Math.round(13.0F - (float) stack.getDamageValue() *
					13.0F / (float) stack.getMaxDamage());
			}
			int current = gun.getAmmo();
			return Math.round(13 * ((float) current / gun.getMaxAmmo()));
		}, 0);
	}

	/**
	 * @param stack the item stack
	 * @return the bar color
	 */
	@Override
	public final int getBarColor(ItemStack stack) {

		Settings settings = Settings.from(this);
		if (settings.gunDurability()) {

			float f = Math.max(0.0F, (float) (stack.getMaxDamage() - stack.getDamageValue())) / stack.getMaxDamage();
			return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
		}
		return 0x00FFFF;
	}

	/**
	 * Stops holding when the gun is dropped.
	 */
	@Override
	public boolean onDroppedByPlayer(ItemStack stack, Player player) {

		process(stack, gun -> { gun.stopHolding(player); });
		return true;
	}

	/**
	 * Ticks gun logic every server tick while the item is selected.
	 */
	@Override
	public final void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		super.inventoryTick(stack, level, entity, slot, selected);

		if (level.isClientSide || !selected) return;

		ServerPlayer player = (ServerPlayer) entity;
		if (player.getCooldowns().isOnCooldown(this)) return;

		EGun.process(stack, gun -> {

			gun.tick();

			boolean isRightPressed = RouterUtils.Mouse.isRightHeld(entity);
			boolean isLeftPressed  = RouterUtils.Mouse.isLeftHeld(entity);

			gun.isLeftPressed(isLeftPressed && (!entity.isSprinting() || isRightPressed));
			gun.isRightPressed(isRightPressed);

			if (isRightPressed) entity.setSprinting(false);
			if (!isLeftPressed) gun.needsTriggerReset(false);
			
			gun.hasJustShooted(false);

			if (gun.canShoot()) {

				IShoot.ShootAction action = handleShoot(stack, entity);
				gun.shoot(level, entity, action);

				if (gun.hasJustShooted()) {

					Settings settings = Settings.from(this);
					if (settings.gunDurability() && !player.gameMode.isCreative()) {

						if (stack.hurt(1, RandomSource.create(), null)) {
							stack.shrink(1);
							stack.setDamageValue(0);
						}
					}
					if (settings.gunPushPlayer()) {

						double push = (action.speed() / getBaseWeight()) * (0.0079 + action.power());

						player.push((push * (0 - entity.getLookAngle().x)), (!entity.onGround() ? 0 : 0.1), (push * (0 - entity.getLookAngle().z)));
						player.hurtMarked = true;
					}
				}
			} else if (gun.getCooldown() == 0) {
				if (isLeftPressed) {

					if (gun.getAmmo() == 0 && gun.needsTriggerSound()) {
						gun.needsTriggerSound(false);

						BlockPos pos = BlockPos.containing(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());
						level.playSound(null, pos, Sounds.TRIGGER, SoundSource.PLAYERS, 0.7f, 1);
					}
				} else gun.needsTriggerSound(true);
				if (gun.canReload(entity)) {

					IReload.ReloadAction action = handleReload(stack, entity);
					setCooldown(action.cooldown());

					RouterUtils.Geckolib.playAnimation(stack, level, (Player) entity, MAIN_CONTROLLER,
						(gun.getShellsCount() > 0 ? (gun.getAmmo() > 0 ? "reload_full" : "reload_empty") : "reload"));

					BlockPos pos = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
					SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("pigeon_core", this.ID + "_reload"));

					level.playSound(null, pos, sound, SoundSource.PLAYERS, 0.55f, 1);

				}
			}
		});
	}

	/**
	 * Registers the custom client renderer for this gun.
	 */
	@Override
	public final void initializeClient(Consumer<IClientItemExtensions> consumer) {
		super.initializeClient(consumer);

		final BlockEntityWithoutLevelRenderer renderer = new GunRenderer<EGun>(new GunRenderer.Model());
		consumer.accept(new IClientItemExtensions() {

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return renderer;
			}
			
			@Override
			public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick, float equipProgress, float swingProgress) {
				
				float f = player.walkDist - player.walkDistO;
        		float f1 = -(player.walkDist + f * partialTick);
        		float f2 = Mth.lerp(partialTick, player.oBob, player.bob);

        		float sinF1 = Mth.sin(f1 * (float)Math.PI);
        		float cosF1 = -Math.abs(Mth.cos(f1 * (float)Math.PI));

        		poseStack.mulPose(Axis.XP.rotationDegrees(-(Math.abs(Mth.cos(f1 * (float)Math.PI - 0.2F) * f2) * 5.0F)));
        		poseStack.mulPose(Axis.ZP.rotationDegrees(-(sinF1 * f2 * 3.0F)));
        		poseStack.translate(-(sinF1 * f2 * 0.5F), -cosF1 * f2, 0.0D);

        		poseStack.translate(0.56F, -0.52F, -0.72F);

        		return true;
			}
		});
	}

	private static final String HOLDING = newKey();

    /**
     * @return {@code true} if the gun is currently being held
     */
	public boolean isHolding() {
		return getBooleanData(readCurrentStack(), HOLDING, false);
	}

    /**
     * Sets whether the gun is being held.
     * 
     * @param value {@code true} if held
     */
	public void isHolding(boolean value) {
		setBooleanData(readCurrentStack(), HOLDING, value);
	}

	/**
	 * Returns the GeckoLib animatable instance cache.
	 */
	@Override
	public final AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	/**
	 * @param stackA the first stack
	 * @param stackB the second stack
	 * @return {@code true} if the stacks represent the same gun instance
	 */
	public static boolean isSameStack(ItemStack stackA, ItemStack stackB) {

    	if (stackA == stackB) return true;
    	if (!isValid(stackA) || !isValid(stackB)) return false;
    
    	if (!stackA.is(stackB.getItem())) return false;

    	long idA = EGun.process(stackA, gun -> gun.getGeckoId(), -1L);
    	long idB = EGun.process(stackB, gun -> gun.getGeckoId(), -1L);
    
    	if (idA != -1 && idB != -1) return idA == idB;
    	return true; 
	}

	/**
	 * @param stack the gun stack
	 * @return the GeckoLib entity id
	 */
	public static long getGeckoIdBy(ItemStack stack) {
		return process(stack, gun -> gun.getGeckoId(), -1l);
	}

    /**
     * @return the number of shell ejection animations
     */
    public int getShellsCount() {
		return 0;
	}

    /**
     * @return the shell ejection sound event
     */
    public SoundEvent getEjectionSound() {
        return PigeonCore.getSound("pigeon_core", "gun.shell_ejection");
    }
}