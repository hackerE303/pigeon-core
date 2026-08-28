package software.hacker_E303.pigeon_core.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import software.hacker_E303.pigeon_core.entity.common.BoundingBox;
import software.hacker_E303.pigeon_core.entity.common.IEntityTexture;
import software.hacker_E303.pigeon_core.entity.common.IEntityTick;
import software.hacker_E303.pigeon_core.entity.common.IEntitySounds;
import software.hacker_E303.pigeon_core.entity.common.BoundingBox.BoundingBoxManager;
import software.hacker_E303.pigeon_core.entity.common.BoundingBox.BoundingBoxSerializer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.hacker_E303.pigeon_core.init.PigeUtils;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;
import software.hacker_E303.pigeon_core.actions.IBasic;
import software.hacker_E303.pigeon_core.actions.IGeneration;
import software.hacker_E303.pigeon_core.client.entity.model.CharacterModel;
import software.hacker_E303.pigeon_core.entity.common.faction.Faction;
import software.hacker_E303.pigeon_core.entity.common.faction.IFaction;
import software.hacker_E303.pigeon_core.entity.common.stats.MobStats;
import software.hacker_E303.pigeon_core.geo.item.gun.EGun;
import software.hacker_E303.pigeon_core.entity.common.stats.IStats;
import software.hacker_E303.pigeon_core.main.event.network.RouterUtils;

/**
 * Base class for custom mob entities.
 */
public abstract class EMob extends PathfinderMob implements IBasic, IEntityTexture, IStats, IEntityTick, IGeneration, IEntitySounds, IFaction {
    
	private static final EntityDataAccessor<String> TEXTURE_NAME = SynchedEntityData.defineId(EMob.class, EntityDataSerializers.STRING);

    @OnlyIn(Dist.CLIENT)
    private String lastTexture;
    private boolean textureLoaded = false;
    private ResourceLocation textureLocation = this.createQuickLocation(this.getTexturePath());

    private MobStats stats;

    protected final String modid = this.modid();
    protected final String pigeid = this.pigeid();

    /**
     * Creates a new mob entity.
     */
    public EMob(EntityType<? extends EMob> mob, Level level) {
        super(mob, level);
        if (!level.isClientSide()) this.setTexture(this.pigeid);
        this.generationEvent(GenerationType.INIT);
    }

    public final boolean isTextureLoaded() {
        return this.textureLoaded;
    }

    @Override
    public Path getTexturePath() {
        return Path.TEXTURE.ENTITIES;
    }

    @Override
    public final String getTexture() {
        return this.entityData.get(TEXTURE_NAME);
    }

    @Override
    public final void setTexture(String name) {
        this.entityData.set(TEXTURE_NAME, name);
        this.textureLocation = Location.create(this.getTexturePath(), name).from(this.modid);

        if (!this.level().isClientSide())
            RouterUtils.Debug.ensureTexture(this, textureLocation, true);
    }

    @Override
    public final ResourceLocation getTextureLocation() {
        String currentTexture = this.getTexture();

        if (currentTexture != null && !currentTexture.equals(this.lastTexture)) {
            this.lastTexture = currentTexture;

            this.textureLocation = Location.create(Path.TEXTURE.ENTITIES,
                currentTexture.replace("texture_", "")).from(this.modid);

            this.textureLoaded = Minecraft.getInstance().getResourceManager()
                .getResource(textureLocation).isPresent();
        }
        return this.textureLocation;
    }

    /**
     * Returns the mob statistics.
     */
    @Override
    public MobStats getStats() {
        if (stats == null) {
            stats = MobStats.create(this, PigeUtils.modidFrom(this), this.pigeid());
            BoundingBoxSerializer.registerListener(stats, this);
        }
        return stats;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        BoundingBox box = this.getStats().getBoundingBox();

        if (box != null) return EntityDimensions.scalable((float) box.getWidth(), (float) box.getHeight());
        return super.getDimensions(pose);
    }

    @Override
    protected final SoundEvent getAmbientSound() {

        BehaviorSounds snd = this.getSounds();

        return (snd != null && snd.ambient() != null) 
               ? snd.ambient() : super.getAmbientSound();
    }

    @Override
    protected final SoundEvent getHurtSound(DamageSource damageSource) {

        BehaviorSounds snd = this.getSounds();

        return (snd != null && snd.hurt() != null) 
               ? snd.hurt() : super.getHurtSound(damageSource);
    }

    @Override
    protected final SoundEvent getDeathSound() {

        BehaviorSounds snd = this.getSounds();

        return (snd != null && snd.death() != null) 
               ? snd.death() : super.getDeathSound();
    }

    @Override
    protected final void playStepSound(BlockPos pos, BlockState state) {

        BehaviorSounds snd = this.getSounds();

        if (snd != null && snd.step() != null)
            this.playSound(snd.step(), snd.defaultVolume() * 0.15F, snd.defaultPitch());
        else 
            super.playStepSound(pos, state);
    }

    /**
     * Adds custom AI goals for this mob.
     */
    public abstract void addCustomGoals();

    /**
     * Registers AI goals for this mob.
     */
    @Override
    protected final void registerGoals() {
        this.addCustomGoals();
    }

    /**
     * Plays a mob sound of the given type.
     */
    public void playMobSound(BehaviorSounds.SoundType type) {
        this.playSound(this.getSounds().get(type), this.getSounds().defaultVolume(), this.getSounds().defaultPitch());
    }

    /**
     * Adds basic movement AI goals.
     */
    protected void handleBasicBehavior(float lookDistance, double walkSpeed) {

        this.goalSelector.addGoal(6, new FloatGoal(this));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, lookDistance));
        this.goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, walkSpeed));
    }

    protected void handleAttackTo(Class<? extends LivingEntity> clazz) {
		this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, clazz, true, false));
    }

    /**
     * Adds melee attack AI goals for this mob.
     */
    protected void handleAttackResponse(double attackRange, double walkSpeed, int attackTicks, boolean alert, Class<?>... ignoreEntities) {

        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, walkSpeed, false) {
            private int customCooldown = 0;

            @Override
            public void tick() {

                if (this.customCooldown > 0) this.customCooldown--;
                super.tick();
            }

            @Override
            public double getAttackReachSqr(LivingEntity living) {
                return attackRange * attackRange;
            }

            @Override
            protected void checkAndPerformAttack(LivingEntity target, double distanceSq) {

                double reach = this.getAttackReachSqr(target);
                if (distanceSq <= reach && this.customCooldown == 0) {

                    this.customCooldown = attackTicks;
                    this.mob.swing(InteractionHand.MAIN_HAND);
                    this.mob.doHurtTarget(target);

                    target.invulnerableTime = attackTicks;
                }
            }
        });

        Class<?>[] ignoreList = (ignoreEntities == null) ? new Class<?>[0] : ignoreEntities;
        HurtByTargetGoal goal = new HurtByTargetGoal(this, ignoreList) {

            @Override
            public boolean canUse() {
                LivingEntity lastHurtBy = mob.getLastHurtByMob();
            
                if (isFriend(lastHurtBy)) return false; 
                return super.canUse();
            }
        };
        if (alert) goal.setAlertOthers();
        this.targetSelector.addGoal(2, goal);
    }

    /**
     * Returns whether this mob uses ranged attacks.
     */
    public boolean isRanged() {

        boolean hasGun = EGun.from(this.getMainHandItem()) != null || EGun.from(this.getOffhandItem()) != null;
        if (hasGun) return true;

        ItemStack main = this.getMainHandItem();
        ItemStack off = this.getOffhandItem();

        if (!main.isEmpty()) {
            if (main.getItem() instanceof BowItem || main.getItem() instanceof CrossbowItem) return true;
        }
        if (!off.isEmpty()) {
            if (off.getItem() instanceof BowItem || off.getItem() instanceof CrossbowItem) return true;
        }
        return false;
    }

    /**
     * Checks whether a friendly entity blocks line of sight to the target.
     */
    public boolean isFriendlyInFront(Entity target, double area) {

        Vec3 start = new Vec3(this.getX(), this.getEyeY(), this.getZ());
        Vec3 end = new Vec3(target.getX(), target.getEyeY(), target.getZ());

        Faction faction = this.getFaction();
        HitResult hitResult = ProjectileUtil.getEntityHitResult(
            this.level(),
            this,
            start,
            end,
            new AABB(start, end).inflate(area),
            e -> e.isAlive() 
                && e != this 
                && e != target
                && e instanceof IFaction factionable 
                && factionable.getFaction() == faction,
            0.0f
        );
        return hitResult != null && hitResult.getType() == HitResult.Type.ENTITY;
    }

    /**
     * Returns whether the mob is facing the target within tolerances.
     */
    public boolean isFacingTarget(LivingEntity target, float yawTolerance, float pitchTolerance) {

        if (target == null) return false;

        double dx = target.getX() - this.getX();
        double dy = target.getEyeY() - this.getEyeY();
        double dz = target.getZ() - this.getZ();

        double distXZ = Math.sqrt(dx * dx + dz * dz);
        if (distXZ < 0.001) return true;

        float targetYaw   = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float targetPitch = (float) (-(Mth.atan2(dy, distXZ) * (180.0 / Math.PI)));

        float yawDiff   = Mth.abs(Mth.wrapDegrees(this.getYRot() - targetYaw));
        float pitchDiff = Mth.abs(Mth.wrapDegrees(this.getXRot() - targetPitch));

        return yawDiff <= yawTolerance && pitchDiff <= pitchTolerance;
    }

    public ModelLayerLocation getModelLayer() {
        return CharacterModel.LAYER_LOCATION;
    }

    @Override
    public final void tick() {
        if (this.tickEvent()) super.tick();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(TEXTURE_NAME, "texture_" + this.pigeid);
        BoundingBoxManager.defineSynchedData(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        compound.putString("Texture", this.entityData.get(TEXTURE_NAME));
        BoundingBoxManager.addAdditionalSaveData(this, compound);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

		if (compound.contains("Texture"))
			this.entityData.set(TEXTURE_NAME, compound.getString("Texture"));
        BoundingBoxManager.readAdditionalSaveData(this, compound);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        BoundingBoxManager.onSyncedDataUpdated(this, key);
    }

    protected boolean whenClicked(Player player, InteractionHand hand) {
        return false;
    }

    protected boolean whenHurt(DamageSource source, float amount) {
        return true;
    }

    protected boolean whenAttacks(LivingEntity target) {
        return true;
    }

    protected boolean whenKilled(DamageSource source) {
        return true;
    }

    /**
     * Handles player interaction with this mob.
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {

        if (this.whenClicked(player, hand))
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        
        return super.mobInteract(player, hand);
    }

    /**
     * Handles damage with custom hurt logic.
     */
    @Override
    public final boolean hurt(DamageSource source, float amount) {

        if (!this.whenHurt(source, amount)) return false;
        return super.hurt(source, amount);
    }

    /**
     * Handles attacking an entity with custom logic.
     */
    @Override
    public final boolean doHurtTarget(Entity target) {

        boolean hasHit = super.doHurtTarget(target);
        if (hasHit && target instanceof LivingEntity living)

            return this.whenAttacks(living);
        return hasHit;
    }

    /**
     * Handles mob death with custom death logic.
     */
    @Override
    public void die(DamageSource source) {
        if (this.whenKilled(source)) super.die(source);
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public float getStepHeight() {
        return 0.6f;
    }
}