package software.hacker_E303.pigeon_core.entity;

import java.util.EnumSet;

import javax.annotation.Nullable;

import software.hacker_E303.pigeon_core.PigeonCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import software.hacker_E303.pigeon_core.actions.IShoot;
import software.hacker_E303.pigeon_core.common.Settings;
import software.hacker_E303.pigeon_core.entity.animation.AnimatableEMob;
import software.hacker_E303.pigeon_core.entity.common.BoundingBox.BoundingBoxSerializer;
import software.hacker_E303.pigeon_core.entity.common.faction.Faction;
import software.hacker_E303.pigeon_core.entity.common.stats.TurretStats;
import software.hacker_E303.pigeon_core.init.PigeUtils;
import software.hacker_E303.pigeon_core.init.gui.TurretGui;
import software.hacker_E303.pigeon_core.common.PigeGui;
import software.hacker_E303.pigeon_core.util.BetterMath;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;
import software.hacker_E303.pigeon_core.util.world.EntityUtils;

/**
 * Base class for turret entities.
 *
 * <p>Manages turret state, AI behavior, targeting, and interaction logic.
 */
public abstract class ETurret extends AnimatableEMob implements IShoot {
        
    public static final Attribute POWER_DURATION = PigeonCore.getAttribute("pigeon_core", "power_duration");

	private static final EntityDataAccessor<String>  OWNER   = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Float>   PITCH   = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.FLOAT);

	private static final EntityDataAccessor<Integer> FUEL   = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> AMMO   = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> HEALTH = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.INT);

	private static final EntityDataAccessor<Boolean> TARGET_PLAYER = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> TARGET_ANIMAL = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> TARGET_MONSTER  = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> TARGET_ATTACKER = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.BOOLEAN);

	private static final EntityDataAccessor<Boolean> IS_LOCKED = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> IS_POWERED  = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.BOOLEAN);

	private static final EntityDataAccessor<Integer> DAMAGE_MODULES = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> HEALTH_MODULES = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> UPGRADED = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.BOOLEAN);

	private static final EntityDataAccessor<String>  NAME_TARGET   = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> ALWAYS_TARGET = SynchedEntityData.defineId(ETurret.class, EntityDataSerializers.BOOLEAN);

	private TurretStats stats;
    private long powerCooldown  = 0;

	private final float BASE_ROTATION_SPEED = 13.0f;
	private float ROTATION_SPEED = BASE_ROTATION_SPEED;

	private double lookPosX = 0.0;
	private double lookPosY = this.getEyeY();
	private double lookPosZ = 0.0;

	private double LOOK_POS_X = this.lookPosX;
	private double LOOK_POS_Y = this.lookPosY;
	private double LOOK_POS_Z = this.lookPosZ;

    private float renderPitch  = 0.0f;
    private float renderPitchO = 0.0f;

    private boolean renderInit = false;

    private int yawSoundCooldown   = 0;
    private int pitchSoundCooldown = 0;
	private int pitchSoundRelease  = 0;

    /**
     * Returns the turret texture path.
     */
    @Override
    public Path getTexturePath() {
        return Path.TEXTURE.TURRETS;
    }

    /**
     * Creates a new turret entity.
     */
    public ETurret(EntityType<? extends ETurret> turret, Level level) {
        super(turret, level);

        this.setNoGravity(true);
		this.setPersistenceRequired();
    }

    /**
     * Returns the bullet entity type fired by this turret.
     */
    public abstract EBullet getBullet();

    /**
     * Returns the turret type.
     */
    public abstract Type getTurretType();

    /**
     * Returns the turret statistics.
     */
    @Override
    public TurretStats getStats() {
        if (stats == null) {
			stats = TurretStats.create(this, PigeUtils.modidFrom(this), this.pigeid);
			BoundingBoxSerializer.registerListener(stats, this);
		}
        return stats;
	}

    /**
     * Supported turret firing behaviors that differentiate shooting patterns.
     */
	public enum Type {
		MACHINE_GUN,
		SINGLE_SHOOT,
		VOLLEY_SHOTS,
		ROCKET_SHOTS,
		ENERGETIC
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public final float getStepHeight() {
        return 0.0f;
    }

    /**
     * Holds the static sound definitions and rotating sound events for turrets.
     */
	private static class SoundHolder {
		static final SoundEvent TURRET_HURT = PigeonCore.getSound("pigeon_core", "turret.hurt");
		static final BehaviorSounds SOUNDS = BehaviorSounds.create(null, TURRET_HURT, null, null, 1.0f, 1.0f);

		static final SoundEvent YAW = PigeonCore.getSound("pigeon_core", "turret.rotating.yaw");
		static final SoundEvent PITCH_UP = PigeonCore.getSound("pigeon_core", "turret.rotating.pitch_up");
		static final SoundEvent PITCH_DOWN = PigeonCore.getSound("pigeon_core", "turret.rotating.pitch_down");
	}

    /**
     * Returns the behavior sounds for this turret.
     */
    @Override
    public final BehaviorSounds getSounds() {
        return SoundHolder.SOUNDS;
    }

    /**
     * Returns whether this entity performs ranged attacks.
     *
     * @return always {@code true} for turrets
     */
    @Override
    public final boolean isRanged() {
        return true;
    }

    /**
     * Returns the turret turn speed.
     */
    public final float getTrottleSpeed() {
        float turnSpeed = this.ROTATION_SPEED / (0.5f + this.getBbWidth());
        return Mth.clamp(turnSpeed, 1.5f, 20.0f);
    }

    /**
     * Main tick handler for power, fuel, ammo, and rotation logic.
     */
    @Override
    public void baseTick() {
        super.baseTick();

        if (this.entityData.get(FUEL) > 0 && this.entityData.get(IS_POWERED)) {
            if (this.powerCooldown < this.getStats().getPowerDuration()) this.powerCooldown++;
            else {
                this.powerCooldown = 0;
                this.entityData.set(FUEL, this.entityData.get(FUEL) - 1);
            }
        } else this.setTarget(null);
        this.removeAllEffects();

        if (this.level().isClientSide()) this.updateClientPitch();
        else {
			double dX = this.lookPosX - this.getX();
			double dY = this.lookPosY - this.getEyeY();
			double dZ = this.lookPosZ - this.getZ();
			double dXZ = Math.sqrt(dX * dX + dZ * dZ);
			if (dXZ == 0) return;

			float targetYaw = (float) (Mth.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0f;
			float targetPitch = (float) -(Mth.atan2(dY, dXZ) * (180.0 / Math.PI));

			float oldYaw = this.getYRot();
			float oldPitch = this.getPitch();

			this.updateServerYaw(targetYaw);

			float yawDiff = Mth.wrapDegrees(targetYaw - this.getYRot());
			float yawAlignmentThreshold = 10.0f;
			boolean pitchActive = Math.abs(yawDiff) <= yawAlignmentThreshold;

			if (pitchActive) this.updateServerPitch(targetPitch);

			float newYaw   = this.getYRot();
			float newPitch = this.getPitch();

			if (this.yawSoundCooldown > 0)   this.yawSoundCooldown--;
			if (this.pitchSoundCooldown > 0) this.pitchSoundCooldown--;

			int interval = Math.max(2, (int) (5.0f / this.getTrottleSpeed()));

			boolean yawMoved = Math.abs(Mth.wrapDegrees(newYaw - oldYaw)) > 0.001f;
			boolean pitchMoved = Math.abs(Mth.wrapDegrees(newPitch - oldPitch)) > 0.001f;

			if (pitchMoved && this.pitchSoundRelease < 20) this.pitchSoundRelease++;
			else this.pitchSoundRelease--;

			if (pitchMoved || pitchSoundRelease > 0) {
				if (this.pitchSoundCooldown <= 0) {

					this.playRotatingSound(newPitch < oldPitch ? 2 : 3);
					this.pitchSoundCooldown = interval;
				}
			} else this.pitchSoundCooldown = 0;

			if (yawMoved) {
				if (this.yawSoundCooldown <= 0) {

					this.playRotatingSound(1);
					this.yawSoundCooldown = interval;
				}
			} else this.yawSoundCooldown = 0;
		}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean shouldShowName() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isPushable() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean canBeCollidedWith() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void travel(Vec3 pTravelVector) {
        this.setDeltaMovement(Vec3.ZERO);
    }

    /**
     * Damage type tag that this turret is fully immune to.
     */
	public static final TagKey<DamageType> IMMUNE_DAMAGES = TagKey.create(Registries.DAMAGE_TYPE, 
    	Location.create(Path.NONE, "turret_immune").from("pigeon_core"));

    /**
     * Returns the turret's damage source for projectile impacts.
     */
    @Override
    public boolean whenHurt(DamageSource source, float amount) {

		Entity directSource = source.getDirectEntity();

		if (directSource instanceof Projectile && this.getY() + this.getBbHeight() <= directSource.getY()) directSource.discard();
		if (source.is(IMMUNE_DAMAGES) || source.getEntity() instanceof ETurret || this.invulnerableTime > 0) return false;

        if (!this.level().isClientSide()) {

			if (source.getEntity() instanceof LivingEntity living)
				if (!EntityUtils.isValid(this.getTarget())) this.setLastHurtByMob(living);

            this.playMobSound(BehaviorSounds.SoundType.HURT);
            this.setHealth(this.getHealth() - amount);

			int dynamicTicks = Math.round(amount * 2.0F); 
        	this.invulnerableTime = BetterMath.clamp(dynamicTicks, 3, 20);

		    float healthPct = this.getHealth() / this.getMaxHealth();
		    this.entityData.set(HEALTH, Math.round(healthPct * 9));

		    if (this.getHealth() <= 0) 
				this.whenKilled(source);
        }
        return false;
    }

    /**
     * Handles turret destruction.
     */
    @Override
    protected boolean whenKilled(DamageSource source) {

		this.discard();
		/*ExplodeProcedure.explode(this.level(), 0.8F,
			this.getX(), this.getY(), this.getZ(), false, false);*/

		return false;
	}

    /**
     * Returns the sound category used when emitting turret sounds.
     *
     * @return the {@link SoundSource#BLOCKS} category
     */
    @Override
    public final SoundSource getSoundSource() {
        return SoundSource.BLOCKS;
    }

    /**
     * Handles a player interaction with this turret.
     *
     * <p>Shift-clicking while holding a turret interaction item applies the item's
     * effect, otherwise the turret GUI is opened.
     *
     * @param player the interacting player
     * @param hand the hand used for the interaction
     * @return {@code true} if the interaction was handled
     */
    @Override
    protected boolean whenClicked(Player player, InteractionHand hand) {

		if (this.tickCount < 3 || !this.isOwner(player) && !this.isAccessible()) return false;
		ItemStack held = player.getItemInHand(hand);

		if (player.isShiftKeyDown() && held.is(ItemTags.create(new ResourceLocation("btw:turret_interact_items")))) {

			if (held.getItem() == Items.AIR) return false;
			if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer)

				this.handleInteraction(serverPlayer, held);
			return true;
		}
		this.getTurretGui().open(player, this);
		return true;
	}

    /**
     * Returns the GUI used for turret interaction.
     */
    protected PigeGui getTurretGui() {
		return PigeGui.get(TurretGui.class);
	}

    /**
     * Registers all synchronized entity data values used by this turret.
     */
    @Override
    protected void defineSynchedData() {
		super.defineSynchedData();

		this.entityData.define(OWNER, "");
		this.entityData.define(PITCH, 0.0f);

		this.entityData.define(FUEL,   0);
		this.entityData.define(AMMO,   0);
		this.entityData.define(HEALTH, 9);

		this.entityData.define(TARGET_PLAYER,   false);
		this.entityData.define(TARGET_ANIMAL,   false);
		this.entityData.define(TARGET_MONSTER,  false);
		this.entityData.define(TARGET_ATTACKER, false);

		this.entityData.define(IS_LOCKED,  false);
		this.entityData.define(IS_POWERED,  false);

		this.entityData.define(DAMAGE_MODULES, 0);
		this.entityData.define(HEALTH_MODULES, 0);
		this.entityData.define(UPGRADED, false);

		this.entityData.define(NAME_TARGET, "Manual Target Selection");
		this.entityData.define(ALWAYS_TARGET, false);
	}

    /**
     * Writes the turret's persistent state to the given NBT tag.
     *
     * @param nbt the compound tag to write to
     */
    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);

		nbt.putString("owner", this.entityData.get(OWNER));
		nbt.putFloat("pitch", this.entityData.get(PITCH));
		nbt.putLong("fuel_cooldown", this.powerCooldown);

		nbt.putInt("fuel",   this.entityData.get(FUEL));
		nbt.putInt("ammo",   this.entityData.get(AMMO));
		nbt.putInt("health", this.entityData.get(HEALTH));

		nbt.putBoolean("target_player",   this.entityData.get(TARGET_PLAYER));
		nbt.putBoolean("target_animal",   this.entityData.get(TARGET_ANIMAL));
		nbt.putBoolean("target_monster",  this.entityData.get(TARGET_MONSTER));
		nbt.putBoolean("target_attacker", this.entityData.get(TARGET_ATTACKER));

		nbt.putBoolean("locked", this.entityData.get(IS_LOCKED));
		nbt.putBoolean("power",  this.entityData.get(IS_POWERED));

		nbt.putInt("damage_modules", this.entityData.get(DAMAGE_MODULES));
		nbt.putInt("health_modules", this.entityData.get(HEALTH_MODULES));
		nbt.putBoolean("upgraded",   this.entityData.get(UPGRADED));

		nbt.putString("name_target",    this.entityData.get(NAME_TARGET));
		nbt.putBoolean("always_target", this.entityData.get(ALWAYS_TARGET));

		nbt.putDouble("init_look_pos_x", this.LOOK_POS_X);
		nbt.putDouble("init_look_pos_y", this.LOOK_POS_Y);
		nbt.putDouble("init_look_pos_z", this.LOOK_POS_Z);
	}

    /**
     * Reads the turret's persistent state from the given NBT tag.
     *
     * @param nbt the compound tag to read from
     */
    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);

		if (nbt.contains("owner"))
			this.entityData.set(OWNER, nbt.getString("owner"));
		if (nbt.contains("pitch"))
			this.entityData.set(PITCH, nbt.getFloat("pitch"));
		if (nbt.contains("fuel_cooldown"))
			this.powerCooldown = nbt.getLong("fuel_cooldown");

		if (nbt.contains("fuel"))
			this.entityData.set(FUEL, nbt.getInt("fuel"));
		if (nbt.contains("ammo"))
			this.entityData.set(AMMO, nbt.getInt("ammo"));
		if (nbt.contains("health"))
			this.entityData.set(HEALTH, nbt.getInt("health"));

		if (nbt.contains("target_player"))
			this.entityData.set(TARGET_PLAYER, nbt.getBoolean("target_player"));
		if (nbt.contains("target_animal"))
			this.entityData.set(TARGET_ANIMAL, nbt.getBoolean("target_animal"));
		if (nbt.contains("target_monster"))
			this.entityData.set(TARGET_MONSTER, nbt.getBoolean("target_monster"));
		if (nbt.contains("target_attacker"))
			this.entityData.set(TARGET_ATTACKER, nbt.getBoolean("target_attacker"));

		if (nbt.contains("locked"))
			this.entityData.set(IS_LOCKED, nbt.getBoolean("locked"));
		if (nbt.contains("power"))
			this.entityData.set(IS_POWERED, nbt.getBoolean("power"));

		if (nbt.contains("damage_modules"))
			this.entityData.set(DAMAGE_MODULES, nbt.getInt("damage_modules"));
		if (nbt.contains("health_modules"))
			this.entityData.set(HEALTH_MODULES, nbt.getInt("health_modules"));
		if (nbt.contains("upgraded"))
			this.entityData.set(UPGRADED, nbt.getBoolean("upgraded"));

		if (nbt.contains("name_target"))
			this.entityData.set(NAME_TARGET, nbt.getString("name_target"));
		if (nbt.contains("always_target"))
			this.entityData.set(ALWAYS_TARGET, nbt.getBoolean("always_target"));

		if (nbt.contains("init_look_pos_x")) {
			this.LOOK_POS_X = nbt.getDouble("init_look_pos_x");
			this.lookPosX = this.LOOK_POS_X;
		}
		if (nbt.contains("init_look_pos_y")) {
			this.LOOK_POS_Y = nbt.getDouble("init_look_pos_y");
			this.lookPosY = this.LOOK_POS_Y;
		}
		if (nbt.contains("init_look_pos_z")) {
			this.LOOK_POS_Z = nbt.getDouble("init_look_pos_z");
			this.lookPosZ = this.LOOK_POS_Z;
		}
		if (this.tickCount != 0) this.resetLookPos();
	}

    /**
     * Returns whether the turret should despawn when far from a player.
     *
     * @param distByPlayer the distance to the nearest player
     * @return always {@code false}; turrets never despawn
     */
    @Override
    public boolean removeWhenFarAway(double distByPlayer) {
        return false;
    }

    /**
     * Resets turret health to maximum.
     */
    public void resetHealth() {

        this.setHealth(this.getMaxHealth());
        this.entityData.set(HEALTH, 9);
    }

    /**
     * Increases the turret's damage module count.
     */
    public boolean improveDamage() {

        int damageModules = this.entityData.get(DAMAGE_MODULES);
        if (damageModules > 4) return false;

        this.entityData.set(DAMAGE_MODULES, damageModules + 1);
        resetHealth();
        return true;
    }

    /**
     * Increases the turret's health module count.
     */
    public boolean improveHealth() {

        int healthModules = this.entityData.get(HEALTH_MODULES);
        if (healthModules > 4) return false;
        
        this.entityData.set(HEALTH_MODULES, healthModules + 1);
        resetHealth();
        return true;
    }

    /**
     * Upgrades the turret to unlock module slots.
     */
    public boolean upgrade() {

        boolean upgraded = this.entityData.get(UPGRADED);
        if (upgraded) return false;
        
        this.entityData.set(UPGRADED, true);
        resetHealth();
        return true;
    }

    /**
     * Returns the raw fuel amount used by the fuel HUD bar.
     *
     * @return the current fuel count
     */
	public int getBarFuel() {
        return this.entityData.get(FUEL);
    }

    /**
     * Returns the clamped ammo level (0-9) shown on the HUD bar.
     *
     * @return the ammo level for display
     */
	public int getBarAmmo() {
        return Math.min((this.entityData.get(AMMO) + 199) / 200, 9);
    }

    /**
     * Returns the health level (0-9) shown on the HUD bar.
     *
     * @return the displayed health level
     */
	public int getBarHealth() {
        return this.entityData.get(HEALTH);
    }

    /**
     * Returns the number of installed damage modules.
     *
     * @return the damage module count
     */
	public int getDamageModules() {
        return this.entityData.get(DAMAGE_MODULES);
    }

    /**
     * Returns the number of installed health modules.
     *
     * @return the health module count
     */
	public int getHealthModules() {
        return this.entityData.get(HEALTH_MODULES);
    }

    /**
     * Returns the combined upgrade level of this turret.
     *
     * @return the sum of health and damage module counts
     */
	public int getLevel() {
        return this.getHealthModules() + this.getDamageModules();
    }

    /**
     * Returns the turret owner name.
     */
    public String getOwnerName() {
		return this.entityData.get(OWNER);
	}

    /**
     * Sets the owner name of this turret.
     *
     * @param value the owner's scoreboard name
     */
	public void setOwnerName(String value) {
		this.entityData.set(OWNER, value);
	}

    /**
     * Returns the turret's current vertical aim pitch.
     *
     * @return the pitch in degrees
     */
	public float getPitch() {
		return this.entityData.get(PITCH);
	}

    /**
     * Sets the turret's vertical aim pitch.
     *
     * @param value the pitch in degrees
     */
	public void setPitch(float value) {
		this.entityData.set(PITCH, value);
	}

    /**
     * Returns whether the specified entity owns this turret.
     */
    public boolean isOwner(Entity entity) {
		return entity.getScoreboardName().equals(this.getOwnerName());
	}

    /**
     * Returns whether the turret is currently powered.
     *
     * @return {@code true} if powered
     */
	public boolean isPowered() {
		return this.entityData.get(IS_POWERED);
	}

    /**
     * Returns whether a non-owner player can interact with this turret.
     *
     * @return {@code true} if not locked
     */
	public boolean isAccessible() {
		return !this.entityData.get(IS_LOCKED);
	}

    /**
     * Returns whether the turret may target players.
     *
     * @return {@code true} if players can be targeted
     */
	public boolean canTargetPlayers() {
		return this.entityData.get(TARGET_PLAYER);
	}

    /**
     * Sets whether the turret may target players.
     *
     * @param value {@code true} to allow targeting players
     */
	public void canTargetPlayers(boolean value) {
		this.entityData.set(TARGET_PLAYER, value);
	}

    /**
     * Returns whether the turret may target animals.
     *
     * @return {@code true} if animals can be targeted
     */
	public boolean canTargetAnimals() {
		return this.entityData.get(TARGET_ANIMAL);
	}

    /**
     * Sets whether the turret may target animals.
     *
     * @param value {@code true} to allow targeting animals
     */
	public void canTargetAnimals(boolean value) {
		this.entityData.set(TARGET_ANIMAL, value);
	}

    /**
     * Returns whether the turret may target monsters.
     *
     * @return {@code true} if monsters can be targeted
     */
	public boolean canTargetMonsters() {
		return this.entityData.get(TARGET_MONSTER);
	}

    /**
     * Sets whether the turret may target monsters.
     *
     * @param value {@code true} to allow targeting monsters
     */
	public void canTargetMonsters(boolean value) {
		this.entityData.set(TARGET_MONSTER, value);
	}

    /**
     * Returns whether the turret may target entities that attacked it.
     *
     * @return {@code true} if attackers can be targeted
     */
	public boolean canTargetAttackers() {
		return this.entityData.get(TARGET_ATTACKER);
	}

    /**
     * Sets whether the turret may target entities that attacked it.
     *
     * @param value {@code true} to allow targeting attackers
     */
	public void canTargetAttackers(boolean value) {
		this.entityData.set(TARGET_ATTACKER, value);
	}

    /**
     * Toggles the boolean synchronized value referenced by the given {@link Data} key.
     *
     * @param type the data key whose accessor value should be flipped
     */
	public void switchValueOf(Data type) {
		this.entityData.set(type.getAccessor(), !this.entityData.get(type.getAccessor()));
	}

    /**
     * Returns the configured manual target name.
     *
     * @return the manual target name
     */
	public String getNameTarget() { return this.entityData.get(NAME_TARGET); }
    /**
     * Sets the configured manual target name.
     *
     * @param value the manual target name
     */
	public void   setNameTarget(String value) { this.entityData.set(NAME_TARGET, value); }

    /**
     * Returns whether the turret always engages its configured target.
     *
     * @return {@code true} if always targeting
     */
	public boolean isAlwaysTarget() { return this.entityData.get(ALWAYS_TARGET); }
    /**
     * Sets whether the turret always engages its configured target.
     *
     * @param value {@code true} to always target
     */
	public void    setAlwaysTarget(boolean value) { this.entityData.set(ALWAYS_TARGET, value); }

    /**
     * Keys mapping turret toggle flags to their synchronized entity data accessors.
     */
	public enum Data {
		TARGET_PLAYERS(TARGET_PLAYER),
		TARGET_ANIMALS(TARGET_ANIMAL),
		TARGET_MONSTERS(TARGET_MONSTER),
		TARGET_ATTACKERS(TARGET_ATTACKER),
		ALIMENTATION(IS_POWERED),
		ACESSIBILITY(IS_LOCKED);

		private final EntityDataAccessor<Boolean> accessor;

    	/**
    	 * Creates a data key bound to the given boolean entity data accessor.
    	 *
    	 * @param accessor the synchronized data value backing this key
    	 */
    	Data(EntityDataAccessor<Boolean> accessor) {
        	this.accessor = accessor;
    	}

    	/**
    	 * Returns the entity data accessor backing this key.
    	 *
    	 * @return the boolean data accessor
    	 */
    	private EntityDataAccessor<Boolean> getAccessor() {
        	return this.accessor;
    	}
	}

    /**
     * Returns the faction this turret belongs to.
     *
     * @return {@link Faction#TURRETS} when neutral or {@link Faction#HOSTILE_TURRETS} when hostile
     */
    @Override
    public final Faction getFaction() {
        return !this.isHostile() ? Faction.TURRETS : Faction.HOSTILE_TURRETS;
    }

    /**
     * Returns whether this turret is a hostile variant.
     *
     * @return {@code true} if hostile
     */
    protected boolean isHostile() {
        return false;
    }

    /**
     * Returns whether the turret is currently active (powered with fuel and ammo).
     */
    public boolean isActive() {
		return this.entityData.get(IS_POWERED) && this.entityData.get(FUEL) > 0 && this.entityData.get(AMMO) > 0;
	}

    /**
     * Configures turret AI goals and targeting behavior.
     */
    protected void handleTurretBehavior(float growthRate, float baseInaccuracy) {

		if (this.level().isClientSide()) return;

		this.goalSelector.addGoal(0, new TurretRangedGoal(this, growthRate, baseInaccuracy));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {

			private final ETurret turret = ETurret.this;

        /**
         * Returns whether the goal should run.
         */
        @Override
        public boolean canUse() {
				return this.turret.canTargetAttackers() && this.turret.isActive() && super.canUse();
			}

        /**
         * Returns whether the goal can continue.
         */
        @Override
        public boolean canContinueToUse() {
				return this.turret.canTargetAttackers() && this.turret.isActive() && super.canContinueToUse();
			}
		});

		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 1, false, false, living -> {

			if (!canTargetPlayers()) return false;
			if (!isActive()) return false;

			String owner = ETurret.this.entityData.get(OWNER);
			return owner == null || owner.isEmpty() || !living.getDisplayName().getString().equals(owner);
		}));

		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 1, false, false, living ->
			canTargetMonsters() && isActive()));

		this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, EMob.class, 1, false, false, living -> {
			return isActive() && EntityUtils.isAnimal(living, true, true, true);
		}));
	}

    /**
     * Handles turret generation by a player spawner.
     */
    @Override
    public final boolean generationEvent(Player spawner) {

        if (spawner == null) return false;
        this.setOwnerName(spawner.getScoreboardName());

        float playerYaw = Mth.wrapDegrees(spawner.getYRot());
        float oppositeYaw = Mth.wrapDegrees(playerYaw + 180.0F);

        float snappedYaw = Math.round(oppositeYaw / 45.0F) * 45.0F;
        snappedYaw = Mth.wrapDegrees(snappedYaw);

        float yawRadians = snappedYaw * ((float)Math.PI / 180F);
        double distance = 5.0;

        this.lookPosX = this.getX() - (double)Mth.sin(yawRadians) * distance;
        this.lookPosY = this.getEyeY();
        this.lookPosZ = this.getZ() + (double)Mth.cos(yawRadians) * distance;

        if (this.lookPosX == this.getX() && this.lookPosY == this.getZ())
            this.lookPosZ += 1.0; 

        this.LOOK_POS_X = this.lookPosX;
        this.LOOK_POS_Y = this.lookPosY;
        this.LOOK_POS_Z = this.lookPosZ;

        this.resetLookPos();
        return true;
    }

    /**
     * Resets the turret's look position to its initial orientation.
     */
    private void resetLookPos() {
   
        double dX = this.lookPosX - this.getX();
        double dZ = this.lookPosZ - this.getZ();

        float targetYaw = (float) (Mth.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0F;
        targetYaw = Mth.wrapDegrees(targetYaw);

        this.setYRot(targetYaw);
        this.setYHeadRot(targetYaw);
        this.setXRot(0);

        this.yRotO = targetYaw;
        this.xRotO = 0;
        this.yHeadRotO = targetYaw;

        this.yBodyRot  = targetYaw;
        this.yBodyRotO = targetYaw;
    }

    /**
     * Synchronizes position and rotation from the server, snapping on first spawn.
     *
     * @param x the target x coordinate
     * @param y the target y coordinate
     * @param z the target z coordinate
     * @param yaw the target yaw
     * @param pitch the target pitch
     * @param posRotationIncrements the number of interpolation steps
     * @param teleport whether the move is a teleport
     */
    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {

        if (this.level().isClientSide() && this.tickCount < 3) {
            this.setPos(x, y, z);
            this.setYRot(yaw);
            this.setXRot(pitch);
            this.yRotO = yaw;
            this.xRotO = pitch;
            this.yBodyRot = yaw;
            this.yBodyRotO = yaw;
            return;
        }
        super.lerpTo(x, y, z, yaw, pitch, posRotationIncrements, teleport);
    }

    /**
     * Synchronizes the turret head rotation, snapping on first spawn.
     *
     * @param yaw the target head yaw
     * @param pitchIncrements the number of interpolation steps
     */
    @Override
    public void lerpHeadTo(float yaw, int pitchIncrements) {
        if (this.level().isClientSide() && this.tickCount < 3) {
            this.setYHeadRot(yaw);
            this.yHeadRotO = yaw;
            return;
        }
        super.lerpHeadTo(yaw, pitchIncrements);
    }

    /**
     * Returns the turret's visual pitch, using the render value on the client.
     *
     * @return the pitch in degrees
     */
    @Override
    public float getXRot() {

        if (this.level().isClientSide()) return this.renderPitch;
        return super.getXRot();
    }

    /**
     * Returns the interpolated visual pitch used for rendering.
     *
     * @param partialTick the render partial tick
     * @return the interpolated pitch
     */
    @Override
    public float getViewXRot(float partialTick) {

        if (this.level().isClientSide()) return Mth.lerp(partialTick, this.renderPitchO, this.renderPitch);
        return super.getViewXRot(partialTick);
    }

    /**
     * AI goal that aims this turret at a target and fires its {@link EBullet} projectiles
     * when the barrel is aligned and the target is in range.
     */
    private class TurretRangedGoal extends Goal {

        private final ETurret turret;
        private final int bulletCount;

        private final int delayValue;
        private final float powerValue;
        private final int cooldownValue;
        private final float inaccuracyValue;
        private final float baseInaccuracyValue;

        private final float speed;
        private final float growthRate;

        @Nullable
        private LivingEntity target;

        private int delay = 0;
        private int cooldown = 0;
        private float inaccuracy = 0;

        private int targetLostTicks = 0;
        private boolean targetTooClose = false;

        /**
         * Creates a ranged attack goal for this turret, sampling its shoot action
         * to precompute speed, delay, power, count, cooldown, and inaccuracy.
         *
         * @param turret the owning turret
         * @param growthRate the per-module damage growth rate
         * @param baseInaccuracy the base projectile inaccuracy
         */
        TurretRangedGoal(ETurret turret, float growthRate, float baseInaccuracy) {

			IShoot.ShootAction action = turret.handleShoot(null, turret);

			this.speed = action.speed();
			this.delayValue = action.delay();
			this.powerValue = action.power();
			this.bulletCount = action.count();
			this.cooldownValue = action.cooldown();
			this.inaccuracyValue = action.inaccuracy();

			this.turret = turret;
			this.growthRate = growthRate;
			this.baseInaccuracyValue = baseInaccuracy;

			this.setFlags(EnumSet.of(Flag.LOOK));
		}

		/**
		 * Determines whether the goal is allowed to run for the current target.
		 *
		 * @return {@code true} if the turret is active, has a valid target, and hasn't lost it
		 */
		private boolean shouldUse()  {
			return this.turret.isActive() && this.isValidTarget() && this.targetLostTicks < 12;
		}

		/**
		 * Validates that the current target is an allowed entity type and not the owner.
		 *
		 * @return {@code true} if the target may be engaged
		 */
		private boolean isValidTarget() {
			if (this.target == null || !this.target.isAlive()) return false;
			if (this.turret.isOwner(this.target)) return false;

			if (this.target instanceof Player && this.turret.canTargetPlayers())   return true;
			if (this.target instanceof Monster && this.turret.canTargetMonsters()) return true;

			if (EntityUtils.isAnimal(this.target, true, true, true) 
				&& this.turret.canTargetAnimals()) return true;

			if (this.turret.getLastHurtByMob() == this.target) if (this.turret.getLastHurtByMob() == this.target
				&& this.turret.canTargetAttackers()) return true;
			return false;
		}

		/**
		 * Checks whether the target is within the turret's maximum shooting range.
		 *
		 * @return {@code true} if the target is reachable
		 */
		private boolean canReachTarget() {
			return EntityUtils.distBetween(this.turret, target) <= this.turret.getStats().getShootRange();
		}

		/**
		 * Returns whether the attack goal should begin.
		 *
		 * @return {@code true} if a valid target is set, in range, and the goal should run
		 */
		@Override
		public boolean canUse() {

			if (EntityUtils.isValid(turret.getTarget()))
				this.target = this.turret.getTarget();

			return this.shouldUse() && this.canReachTarget();
		}

		/**
		 * Returns whether the attack goal should continue.
		 *
		 * @return {@code true} if the goal should run and a line of sight is held
		 */
		@Override
		public boolean canContinueToUse() {
			return this.shouldUse() &&
				(this.turret.getSensing().hasLineOfSight(target) || this.targetTooClose);
		}

        /**
         * Stops the goal and resets state.
         */
        @Override
        public void stop() {

			if (this.turret.getTarget() == this.target)
            	this.turret.setTarget(null);

			this.target = null;
			this.delay = 0;
			this.cooldown = 0;

			this.turret.lookPosX = this.turret.LOOK_POS_X;
            this.turret.lookPosY = this.turret.LOOK_POS_Y;
            this.turret.lookPosZ = this.turret.LOOK_POS_Z;

			this.turret.ROTATION_SPEED = 6.0f;
			this.targetLostTicks = 0;
		}

        /**
         * Fires bullets when conditions are met.
         */
        @Override
        public void tick() {

			if (this.target == null) return;
			this.inaccuracy *= 0.8;

			this.turret.lookPosX = target.getX();
			this.turret.lookPosY = target.getEyeY() - target.getEyeHeight() * 0.25;
			this.turret.lookPosZ = target.getZ();

			this.turret.ROTATION_SPEED = BASE_ROTATION_SPEED;

			double distLimit = 0.5 + this.turret.getBbWidth() + this.target.getBbWidth();
			this.targetTooClose = EntityUtils.isTooClose(turret, target, distLimit);

			if (this.cooldown > 0) {
				this.cooldown--;
				return;
			}
			if (this.delay < this.delayValue) {
				this.delay++;
				return;
			}
			if (!this.canReachTarget()) {
				this.targetLostTicks++;
				return;
			}

			if (this.turret.isFriendlyInFront(target, this.inaccuracy + 
				(double) (this.turret.getBbWidth() * this.turret.getBbHeight() * 2) + 0.8)) return;

			boolean isMachineGun = this.turret.getTurretType() == ETurret.Type.MACHINE_GUN;
			float modifier = targetTooClose ? 3.5f : 1.0f;

			float yawTol   = modifier * (isMachineGun ? 20.0f : 7.0f);
			float pitchTol = modifier * (isMachineGun ? 16.0f : 5.0f);

			if (!turret.isFacingTarget(target, yawTol, pitchTol)) return;

			this.delay = 0;
			this.cooldown = this.cooldownValue;
			this.targetLostTicks = 0;

			if (!isActive()) return;
			SynchedEntityData data = this.turret.getEntityData();

			int ammo = data.get(AMMO);
			if (ammo <= 0) return;

			double baseDamage = this.turret.getStats().get("attack.damage");
			float damage = (float) (baseDamage + (baseDamage * data.get(DAMAGE_MODULES) * this.growthRate));

			this.inaccuracy += this.inaccuracyValue;
			for (int rounds = 0; rounds < this.bulletCount; rounds++) {

				if (ammo <= 0) break;
				ammo--;

				this.turret.getBullet().shoot(this.turret, "", damage, this.speed,
					this.baseInaccuracyValue + this.inaccuracy, powerValue, powerValue * 2.4);
			}
			turret.level().playSound(null, turret.getX(), turret.getY(), turret.getZ(), PigeonCore.getSound("pigeon_core", "generic_shoot"), SoundSource.HOSTILE, 1, 1);
			data.set(AMMO, ammo);
		}

		/**
		 * Returns whether this goal should be ticked every game tick.
		 *
		 * @return always {@code true}
		 */
		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}
	}

    /**
     * Handles player interaction with turret items (fuel, ammo, modules, repair).
     */
    private void handleInteraction(ServerPlayer player, ItemStack held) {

		Item item = held.getItem();
		Level level = this.level();

		// BATTERY
		if (item == this.getFuel()) {
			int fuel = this.entityData.get(FUEL);
			if (fuel < 7) {
				this.entityData.set(FUEL, fuel + 3);
				this.playTurretSound(level, "TURRET_INTERACTION");
				this.consumeItem(player, held);
			} else {
				this.sendDeny(level, player, Component.literal("\u00A77This turret has reached its max energy capacity!"));
			}
			return;
		}

		// RAILGUN AMMO
		if (item == this.getMagazine()) {
			if (this.getTurretType() == Type.MACHINE_GUN) {
				int ammoSlot = this.entityData.get(AMMO);
				if (ammoSlot <= 1200) {
					this.entityData.set(AMMO, ammoSlot + 600);
					this.playTurretSound(level, "TURRET_INTERACTION");
					this.consumeItem(player, held);
				} else {
					this.sendDeny(level, player, Component.literal("\u00A77This turret has reached its max ammo capacity!"));
				}
			} else {
				this.sendDeny(level, player, Component.literal("\u00A77These projectiles are not compatible with this turret!"));
			}
			return;
		}

		// HEAVY AMMO
		if (item == this.getMagazine()) {
			if (this.getTurretType() == Type.SINGLE_SHOOT) {
				int ammoSlot = this.entityData.get(AMMO);
				if (ammoSlot <= 1600) {
					this.entityData.set(AMMO, ammoSlot + 200);
					this.playTurretSound(level, "TURRET_INTERACTION");
					this.consumeItem(player, held);
				} else {
					this.sendDeny(level, player, Component.literal("\u00A77This turret has reached its max ammo capacity!"));
				}
			} else {
				this.sendDeny(level, player, Component.literal("\u00A77These projectiles are not compatible with this turret!"));
			}
			return;
		}

		// SHOTGUN AMMO
		if (item == this.getMagazine()) {
			if (this.getTurretType() == Type.VOLLEY_SHOTS) {
				int ammoSlot = this.entityData.get(AMMO);
				if (ammoSlot <= 1200) {
					this.entityData.set(AMMO, ammoSlot + 600);
					this.playTurretSound(level, "TURRET_INTERACTION");
					this.consumeItem(player, held);
				} else {
					this.sendDeny(level, player, Component.literal("\u00A77This turret has reached its max ammo capacity!"));
				}
			} else {
				this.sendDeny(level, player, Component.literal("\u00A77These projectiles are not compatible with this turret!"));
			}
			return;
		}

		// ROCKET BOX
		if (item == this.getMagazine()) {
			if (this.getTurretType() == Type.ROCKET_SHOTS) {
				int ammoSlot = this.entityData.get(AMMO);
				if (ammoSlot <= 1600) {
					this.entityData.set(AMMO, ammoSlot + 200);
					this.playTurretSound(level, "TURRET_INTERACTION");
					this.consumeItem(player, held);
				} else {
					this.sendDeny(level, player, Component.literal("\u00A77This turret has reached its max ammo capacity!"));
				}
			} else {
				this.sendDeny(level, player, Component.literal("\u00A77These projectiles are not compatible with this turret!"));
			}
			return;
		}

		// BOX OF ENERGY CELLS
		if (item == this.getMagazine()) {
			if (this.getTurretType() == Type.ENERGETIC) {
				int ammoSlot = this.entityData.get(AMMO);
				if (ammoSlot <= 1200) {
					this.entityData.set(AMMO, ammoSlot + 600);
					this.playTurretSound(level, "TURRET_INTERACTION");
					this.consumeItem(player, held);
				} else {
					this.sendDeny(level, player, Component.literal("\u00A77This turret has reached its max ammo capacity!"));
				}
			} else {
				this.sendDeny(level, player, Component.literal("\u00A77These projectiles are not compatible with this turret!"));
			}
			return;
		}

		// DAMAGE MODULE
		if (item == this.getDamageModule()) {
			if (this.entityData.get(UPGRADED) || this.entityData.get(DAMAGE_MODULES) + this.entityData.get(HEALTH_MODULES) < 5) {
				if (this.improveDamage()) {
					this.playTurretSound(level, new ResourceLocation("block.anvil.place"));
					this.consumeItem(player, held);
				} else {
					this.sendDeny(level, player, Component.literal("\u00A77Turret damage has been improved to the max!"));
				}
			} else {
				this.sendDeny(level, player, Component.literal("\u00A77You need to upgrade this turret first!"));
			}
			return;
		}

		// HEALTH MODULE
		if (item == this.getHealthModule()) {
			if (this.entityData.get(UPGRADED) || this.entityData.get(DAMAGE_MODULES) + this.entityData.get(HEALTH_MODULES) < 5) {
				if (this.improveHealth()) {
					double base = this.getStats().get("health");
					int healthMods = this.entityData.get(HEALTH_MODULES);
					double newMax = base + (base / 6.0) * healthMods;
					if (this.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
						this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMax);
					}
					this.playTurretSound(level, new ResourceLocation("block.anvil.place"));
					this.consumeItem(player, held);
					this.setHealth(this.getMaxHealth());
				} else {
					this.sendDeny(level, player, Component.literal("\u00A77Turret health has been improved to the max!"));
				}
			} else {
				this.sendDeny(level, player, Component.literal("\u00A77You need to upgrade this turret first!"));
			}
			return;
		}

		// UPGRADE MODULE
		if (item == this.getUpgradeModule()) {
			if (this.upgrade()) {
				this.playTurretSound(level, new ResourceLocation("block.anvil.place"));
				this.consumeItem(player, held);
			} else {
				this.sendDeny(level, player, Component.literal("\u00A77This turret has already been updated!"));
			}
			return;
		}

		// REPAIR KIT
		if (item == this.getRepaiKit()) {
			if (this.entityData.get(HEALTH) < 9) {
				this.resetHealth();
				this.playTurretSound(level, new ResourceLocation("block.anvil.use"));
				this.consumeItem(player, held);
			} else {
				this.sendDeny(level, player, Component.literal("\u00A77This turret is at full health!"));
			}
		}
	}

    /**
     * Returns the item used to refuel this turret.
     *
     * @return the fuel item
     */
	protected Item getFuel() {
		return Settings.from(this).turretFuelItem();
	}

    /**
     * Returns the item used to reload this turret's ammunition.
     *
     * @return the magazine item
     */
	protected Item getMagazine() {
		return Settings.from(this).turretMagazineItem();
	}

    /**
     * Returns the item used to repair this turret.
     *
     * @return the repair kit item
     */
	protected Item getRepaiKit() {
		return Settings.from(this).turretRepairKitItem();
	}

    /**
     * Returns the item used to install a health module.
     *
     * @return the health module item
     */
	protected Item getHealthModule() {
		return Settings.from(this).turretHealthModuleItem();
	}

    /**
     * Returns the item used to install a damage module.
     *
     * @return the damage module item
     */
	protected Item getDamageModule() {
		return Settings.from(this).turretDamageModuleItem();
	}

    /**
     * Returns the item used to upgrade this turret.
     *
     * @return the upgrade module item
     */
	protected Item getUpgradeModule() {
		return Settings.from(this).turretUpgradeModuleItem();
	}

    /**
     * Sends a deny message and sound to the player.
     */
    private void sendDeny(Level level, ServerPlayer player, Component message) {
		player.displayClientMessage(message, false);
		this.playTurretSound(level, new ResourceLocation("entity.villager.no"));
	}

    /**
     * Consumes one item from the player's hand unless in creative mode.
     */
    private void consumeItem(ServerPlayer player, ItemStack held) {
		if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) return;
		held.shrink(1);
		player.getInventory().setChanged();
	}

    /**
     * Plays a turret sound by field name.
     */
    private void playTurretSound(Level level, String soundField) {
		if (level instanceof ServerLevel serverLevel) {
			SoundEvent event = PigeonCore.getSound("pigeon_core", soundField);
			if (event != null)
				serverLevel.playSound(null, this.blockPosition(), event, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

    /**
     * Plays a turret sound by registry name.
     */
    private void playTurretSound(Level level, ResourceLocation soundId) {
		if (level instanceof ServerLevel serverLevel) {
			SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
			if (event != null)
				serverLevel.playSound(null, this.blockPosition(), event, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

    /**
     * Smoothly interpolates the turret yaw toward the target.
     */
    private void updateServerYaw(float targetYaw) {

        float baseTurnSpeed = this.ROTATION_SPEED;
        float entityWidth = this.getBbWidth(); 
        float turnSpeed = baseTurnSpeed / (0.5f + entityWidth); 
        turnSpeed = Mth.clamp(turnSpeed, 1.5f, 20.0f);

        float currentYaw = this.getYRot();
        float newYaw = currentYaw + Mth.clamp(Mth.wrapDegrees(targetYaw - currentYaw), -turnSpeed, turnSpeed);

		this.yBodyRot = newYaw;
		this.yBodyRotO = newYaw;

        this.setYRot(newYaw);
        this.setYHeadRot(newYaw);
    }

    /**
     * Smoothly interpolates the turret pitch toward the target.
     */
    private void updateServerPitch(float targetPitch) {

        float baseTurnSpeed = this.ROTATION_SPEED;
        float entityHeight = this.getBbHeight(); 
        float turnSpeed = baseTurnSpeed / (0.5f + entityHeight); 
        turnSpeed = Mth.clamp(turnSpeed, 1.5f, 20.0f);
        
        float pitchSpeed = Math.max(turnSpeed * 0.7f, 0.15f);

        float currentPitch = this.getPitch();
        float diff = Mth.wrapDegrees(targetPitch - currentPitch);
        
        float newPitch;
        if (Math.abs(diff) > pitchSpeed) {
            newPitch = currentPitch + Math.signum(diff) * pitchSpeed;
        } else {
            newPitch = targetPitch;
        }
        this.entityData.set(PITCH, newPitch);
        this.setXRot(newPitch);
    }

    /**
     * Updates the client-side pitch for smooth rendering.
     */
    private void updateClientPitch() {

        if (!this.renderInit) {
            float initial = this.entityData.get(PITCH);

            this.renderPitch  = initial;
            this.renderPitchO = initial;
            this.renderInit = true;
        }

        float target = this.entityData.get(PITCH);
        this.renderPitchO = this.renderPitch;
        this.renderPitch = Mth.lerp(0.15f, this.renderPitch, target);

        this.xRotO = this.renderPitchO;
        this.setXRot(this.renderPitch);
    }

    /**
     * Plays a rotating sound for yaw or pitch movement.
     */
    private void playRotatingSound(int sound) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound == 1 ? SoundHolder.YAW : (sound == 2 ? SoundHolder.PITCH_UP : SoundHolder.PITCH_DOWN),
			SoundSource.BLOCKS, 0.15f, 1.0f);
    }
}