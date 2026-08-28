package software.hacker_E303.pigeon_core.entity;

import java.lang.reflect.Field;
import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.hacker_E303.pigeon_core.PigeonCore;
import software.hacker_E303.pigeon_core.client.gun.renderer.TrailRenderer;
import software.hacker_E303.pigeon_core.init.PigeUtils;
import software.hacker_E303.pigeon_core.main.event.network.RouterUtils;
import software.hacker_E303.pigeon_core.util.BetterData;
import software.hacker_E303.pigeon_core.util.BetterMath;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;
import software.hacker_E303.pigeon_core.util.misc.ShootUtils;
import software.hacker_E303.pigeon_core.util.world.BlockUtils;

/**
 * Base class for projectile entities fired by turrets and other shooters.
 */
@SuppressWarnings("unchecked")
public abstract class EBullet extends AbstractArrow {

    public final String ID = PigeUtils.pigeidFrom(this);

    private static final SoundEvent RICOCHET_SOUND = PigeonCore.getSound("pigeon_core", "ricochet");

    private static final EntityDataAccessor<Float> DATA_DIRECTION_X = SynchedEntityData.defineId(EBullet.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Y = SynchedEntityData.defineId(EBullet.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Z = SynchedEntityData.defineId(EBullet.class, EntityDataSerializers.FLOAT);

    private static final Field LEFT_OWNER_FIELD;
    private static final Field PIERCING_IGNORE_ENTITY_IDS_FIELD;
    private static final Field PIERCED_AND_KILLED_ENTITIES_FIELD;

    protected boolean onFoliage = false;

    protected boolean isFirstTick() {
        return this.tickCount == 1;
    }

    private TrailRenderer trail;

    private Vec3 direction = Vec3.ZERO;
    private MobType bonus  = MobType.UNDEFINED;

    private Level spawnLevel  = null;
    private Entity owner = null;

    private float power  = 0.0f;
    private float damage = 0.0f;

    private double knockback = 0.0;

    protected int discardTime  = 0;
    protected int gunLevel     = 0;

    public enum Trail {
        NONE,
        NORMAL,
        FREEZING
    }

    public enum ImpactReaction {
        STOP,
        FOLIAGE,
        RICOCHET,
		PENETRATE,
    }

    /**
     * Creates a new bullet entity.
     */
    public EBullet(EntityType<? extends EBullet> type, Level level) {
        super(type, level);
        this.spawnLevel = level;
    }

    /**
     * Initializes the bullet with a level and returns this instance for chaining.
     */
    public  EBullet initLevel(Level level) {
        this.spawnLevel = level;
        return this;
    }

    protected abstract Trail getTrail();

    protected abstract int getDiscardTime();

    /**
     * Fires this bullet from the given shooter.
     */
    public final EBullet shoot(Entity shooter, String bonus, float damage, float speed, float inaccuracy, float power, double knockback) {

        setSilent(true);
        setNoGravity(true);

        Vec3 lookAngle = new Vec3(shooter.getLookAngle().x(), shooter.getLookAngle().y(), shooter.getLookAngle().z());

        this.direction = lookAngle.normalize().add(random.triangle(0.0D, 0.0172275D * (double) inaccuracy),
        random.triangle(0.0D, 0.0172275D * (double) inaccuracy), random.triangle(0.0D, 0.0172275D * (double) inaccuracy)).scale((double) speed);

        setPos(shooter.getX(), shooter.getEyeY() - 0.185, shooter.getZ());

        setDeltaMovement(direction);
        setSyncedDirection(lookAngle);

        this.knockback = knockback;
        this.owner = shooter;

        this.power  = power;
        this.damage = damage;

        setPierceLevel((byte) Math.floor(power * 8.6));
        if (getTrail().equals(Trail.FREEZING)) ShootUtils.spawnFreezeTrail((ServerLevel) shooter.level(), (Player) shooter, 32);

        bonus.toLowerCase();
        if (bonus.equals("Undead"))         this.bonus = MobType.UNDEAD;
        else if (bonus.equals("Illager"))   this.bonus = MobType.ILLAGER;
        else if (bonus.equals("Water"))     this.bonus = MobType.WATER;
        else if (bonus.equals("Anthropod")) this.bonus = MobType.ARTHROPOD;

        if (shooter instanceof LivingEntity living)
            BetterData.getData(living.getMainHandItem(), "gun_data_2", 1);

        spawnLevel.addFreshEntity(this);
        spawnLevel = null;

        handlePiercing(shooter);
        forceLeftOwner(true);
        return this;
    }

    private static final Location KINETIC_DAMAGE = Location.create(Path.NONE, "kinetic_round");

    @Override
    public final void onHitEntity(EntityHitResult result) {

        Entity entity = result.getEntity();
        if (!(entity instanceof LivingEntity living) || owner == entity) return;

        double relativeHitHeight = getY() + getDeltaMovement().y() - entity.getY();
        boolean isHeadshot = relativeHitHeight >= (living.getBbHeight() * 0.7);

        float finalDamage = damage;
        if (isHeadshot) {

            double dynamicbonus = Math.log1p(damage) * power;
            finalDamage += (float) (damage * dynamicbonus);
        }
        if (bonus != MobType.UNDEFINED && living.getMobType() == bonus) finalDamage += damage * 0.1f;
        Registry<DamageType> registry = level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);

        DamageSource damageSource = new DamageSource(registry.getHolderOrThrow(
            ResourceKey.create(Registries.DAMAGE_TYPE, KINETIC_DAMAGE.from("pigeon_core"))), this, owner);

        entity.invulnerableTime = 0;

        float clumpedDamage = BetterMath.decimalCut(finalDamage, 2);
        entity.hurt(damageSource, clumpedDamage);

        if (knockback > 0.0) {
            double resistence = Math.max(0.0, 1.0 - living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * (1.0 - power));
            Vec3 push = getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(knockback * (isHeadshot ? 0.25 : 0.01) * resistence * 0.01);

            if (push.lengthSqr() > 0.0) living.push(push.x(), power * 0.2, push.z());
        }
        if (owner != null) living.setLastHurtMob(owner);
        whenHits(entity, clumpedDamage);

        if (this.getPierceLevel() == 0) this.discard();
        handlePiercing(entity);
    }

    /**
     * Applies effects after the bullet hits an entity.
     */
    protected void whenHits(Entity entity, float damage) {
    }

    /**
     * Applies a mob effect to the hit entity if conditions are met.
     */
    protected final void applyEffect(Entity entity, MobEffect effect, int power, int duration, boolean accumulable) {

        if (!level().isClientSide && entity instanceof LivingEntity living) {
            if (entity instanceof ETurret) return;

            int addedDuration = (int) (duration * Math.max(gunLevel * 0.12, 1.0));
            int currentAmplifier = (int) Math.max(((double) gunLevel / damage * power * 2.5 - 1), 0.0f);

            MobEffectInstance existing = living.getEffect(effect);
            if (existing != null && accumulable) {

                int totalDuration = Math.min(existing.getDuration() + addedDuration, 600);
                living.addEffect(new MobEffectInstance(effect, totalDuration, currentAmplifier, false, false));

            } else living.addEffect(new MobEffectInstance(effect, addedDuration, currentAmplifier, false, false));
        }
    }

    @Override
    public final void tick() {

        Vec3 prevPos = this.position();
        super.tick();

        if (discardTime < getDiscardTime()) discardTime++;
        else discard();

        if (level().isClientSide()) {

            Vec3 direction = getSyncedDirection();
            TrailRenderer renderer = getTrailRenderer();

            if (getSyncedDirection().lengthSqr() > 0.001 && renderer.DIRECTION.lengthSqr() < 0.001) renderer.DIRECTION = direction;
            renderer.updateTrail(this);

        } else if (!this.isRemoved())
        if (!this.position().equals(prevPos)) {

            if (!BlockUtils.isFoliage(this.level(), this.position()) && onFoliage) {
                
                onFoliage = false;
                this.setNoPhysics(false);
            } else {

                BlockHitResult blockHit = this.level().clip(new ClipContext(prevPos, this.position(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

                if (blockHit != null && blockHit.getType() != HitResult.Type.MISS)
                    this.onHitBlock(blockHit);
            }
        }
        if (this.isRemoved()) return;
        if (this.onGround()) this.discard();

        setDeltaMovement(direction);
        clearFire();

        this.whenTick();
    }

    protected void whenTick() {
    }

    @Override
    public final void onHitBlock(BlockHitResult result) {

        if (this.level().isClientSide) return;
        if (this.isRemoved()) return;

        Vec3 pos = result.getLocation();
        Direction face = result.getDirection();

        switch (this.getImpactReaction(result.getBlockPos())) {

            case STOP:
                RouterUtils.Internal.spawnBulletHole(pos, this.level(), face);
                this.spawnImpactParticles(pos, face);
                this.discard();
                break;

            case RICOCHET:
                this.spawnRicochetParticles(pos, face);
                this.discard();
                break;

            case PENETRATE:
                this.level().destroyBlock(result.getBlockPos(), false);
                this.direction = this.direction.scale(0.6f);
                break;

            case FOLIAGE:
                this.onFoliage = true;
                this.setNoPhysics(true);
                break;
        }
    }

    @Override
    public final Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected ItemStack getPickupItem() {
        return null;
    }

    @Override
    public final boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        entityData.define(DATA_DIRECTION_X, 0.0f);
        entityData.define(DATA_DIRECTION_Y, 0.0f);
        entityData.define(DATA_DIRECTION_Z, 0.0f);
    }

    /**
     * Returns the bullet's current synchronized direction.
     */
    public Vec3 getSyncedDirection() {
        return new Vec3(
        entityData.get(DATA_DIRECTION_X),
        entityData.get(DATA_DIRECTION_Y),
        entityData.get(DATA_DIRECTION_Z));
    }

    /**
     * Sets the synchronized direction sent to clients.
     */
    public void setSyncedDirection(Vec3 direction) {

        entityData.set(DATA_DIRECTION_X, (float) direction.x);
        entityData.set(DATA_DIRECTION_Y, (float) direction.y);
        entityData.set(DATA_DIRECTION_Z, (float) direction.z);
    }

    /**
     * Determines the impact reaction for the given block position.
     */
    public ImpactReaction getImpactReaction(BlockPos pos) {

        BlockState state = this.level().getBlockState(pos);
        float hardness = state.getDestroySpeed(this.level(), pos);

        boolean low    = hardness <= 1.0f;
        boolean medium = hardness > 1.0f && hardness <= 5.0f;
        boolean hight  = hardness > 5.0f;

        if (hardness < 0) {
            return ImpactReaction.RICOCHET;
        }
        if (BlockUtils.isGlass(this.level(), pos)) {

            return low ? ImpactReaction.PENETRATE : 
                (medium ? ImpactReaction.STOP : ImpactReaction.RICOCHET);
        }
        if (BlockUtils.isMetal(this.level(), pos)) {
            return ImpactReaction.RICOCHET;
        }
        if (BlockUtils.isStone(this.level(), pos)) {
            return hight ? ImpactReaction.RICOCHET : ImpactReaction.STOP;
        }
        if (BlockUtils.isFoliage(this.level(), pos)) {
            return ImpactReaction.FOLIAGE;
        }
        return ImpactReaction.STOP;
    }

    /**
     * Forces the projectile left-owner state via reflection.
     */
    private void forceLeftOwner(boolean value) {
        try {
            LEFT_OWNER_FIELD.setBoolean(this, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates piercing state after hitting an entity.
     */
    private void handlePiercing(Entity entity) {
        if (this.getPierceLevel() <= 0) return;

        try {
            IntOpenHashSet piercingIgnoreEntityIds = (IntOpenHashSet) PIERCING_IGNORE_ENTITY_IDS_FIELD.get(this);
            if (piercingIgnoreEntityIds == null) {

                piercingIgnoreEntityIds = new IntOpenHashSet(5);
                PIERCING_IGNORE_ENTITY_IDS_FIELD.set(this, piercingIgnoreEntityIds);
            }
            List<Entity> piercedAndKilledEntities = (List<Entity>) PIERCED_AND_KILLED_ENTITIES_FIELD.get(this);
            if (piercedAndKilledEntities == null) {

                piercedAndKilledEntities = Lists.newArrayListWithCapacity(5);
                PIERCED_AND_KILLED_ENTITIES_FIELD.set(this, piercedAndKilledEntities);
            }
            if (piercingIgnoreEntityIds.size() >= this.getPierceLevel() + 1) {
                this.discard();
                return;
            }
            piercingIgnoreEntityIds.add(entity.getId());

            if (!entity.isAlive())
            piercedAndKilledEntities.add(entity);

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Spawns impact particles at the given position and face.
     */
    private void spawnImpactParticles(Vec3 pos, Direction face) {
        if (!(this.level() instanceof ServerLevel server)) return;

        double offset = 0.05;
        double x = pos.x + face.getStepX() * offset + (server.random.nextDouble() - 0.5) * 0.02;
        double y = pos.y + face.getStepY() * offset + (server.random.nextDouble() - 0.5) * 0.02;
        double z = pos.z + face.getStepZ() * offset + (server.random.nextDouble() - 0.5) * 0.02;

        server.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 0.05, 0.05, 0.05, 0.0);
    }

    /**
     * Spawns ricochet particles and plays a sound.
     */
    private void spawnRicochetParticles(Vec3 pos, Direction face) {
        if (!(this.level() instanceof ServerLevel server)) return;

        /*double offset = 0.15;
        double x = pos.x + face.getStepX() * offset;
        double y = pos.y + face.getStepY() * offset;
        double z = pos.z + face.getStepZ() * offset;

        server.sendParticles(PigeTechWeaponsModParticleTypes.SPARK.get(), x, y, z, 1, 0.1, 0.1, 0.1, 0.05);*/
        server.playSound(null, BlockPos.containing(pos), RICOCHET_SOUND, SoundSource.NEUTRAL, 1.5f, 1.0f);
    }

    /**
     * Renders the bullet trail if applicable.
     */
    public void renderTrail(PoseStack poseStack, MultiBufferSource bufferSource) {
        if (this.getTrail().equals(Trail.NORMAL)) this.getTrailRenderer().renderTrail(poseStack, bufferSource);
    }

    private TrailRenderer getTrailRenderer() {

        if (trail == null) trail = new TrailRenderer();
        return trail;
    }

    static {
        Field leftOwner     = null;
		Field piercingField = null;
        Field killedField   = null;

        for (Field field : Projectile.class.getDeclaredFields()) {
            if (field.getType() == boolean.class) {

                leftOwner = field;
                leftOwner.setAccessible(true);
                break;
            }
        }
        if (leftOwner == null)
        throw new RuntimeException("Error occurred trying to access at 'leftOwner' field on class Bullet.java!");

        for (Field field : AbstractArrow.class.getDeclaredFields()) {
            if (field.getType() == IntOpenHashSet.class && piercingField == null) {

                piercingField = field;
                piercingField.setAccessible(true);
            }
            if (field.getType() == List.class && killedField == null) {

                killedField = field;
                killedField.setAccessible(true);
            }
        }
        if (piercingField == null || killedField == null)
        throw new RuntimeException("Error occurred trying to access at 'piercing' field on class Bullet.java!");

		LEFT_OWNER_FIELD = leftOwner;
        PIERCING_IGNORE_ENTITY_IDS_FIELD = piercingField;
        PIERCED_AND_KILLED_ENTITIES_FIELD = killedField;
    }
}