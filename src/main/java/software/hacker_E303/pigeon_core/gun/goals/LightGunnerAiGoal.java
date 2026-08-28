package software.hacker_E303.pigeon_core.gun.goals;

import java.util.EnumSet;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoItem;
import software.hacker_E303.pigeon_core.actions.IReload;
import software.hacker_E303.pigeon_core.actions.IShoot;
import software.hacker_E303.pigeon_core.entity.EBullet;
import software.hacker_E303.pigeon_core.geo.item.gun.EGun;
import software.hacker_E303.pigeon_core.main.event.network.RouterUtils;
import software.hacker_E303.pigeon_core.util.BetterData;
import software.hacker_E303.pigeon_core.util.misc.ShootUtils;

/**
 * AI goal that makes a mob wield a gun and engage targets at range.
 */
public class LightGunnerAiGoal extends Goal {

    public static final String DISTANCE = "gun_data_" + 1024;

    private final Mob mob;
    private final double affinity;
    private final double speed;
    private final double range;
    
    private LivingEntity target;
    private ItemStack stack = ItemStack.EMPTY;

    private int cooldown = 0;
    private int seeTime = 0;
    private int contactTime = 0;

    private float inaccuracy = 0;
    
    /**
     * Creates a new light gunner AI goal.
     * 
     * @param mob the mob
     * @param affinity the accuracy affinity (0-1)
     * @param speed the movement speed
     * @param range the engagement range
     */
    public LightGunnerAiGoal(Mob mob, double affinity, double speed, double range) {

        this.stack = mob.getMainHandItem();

        this.mob = mob;
        this.affinity = affinity;
        this.speed = speed;
        this.range = range;
        
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.mob.goalSelector.removeAllGoals(goal -> goal instanceof MeleeAttackGoal);

        if (mob.level() instanceof ServerLevel serverLevel)
            EGun.process(stack, gun -> {
                gun.setGeckoId(GeoItem.getOrAssignId(gun.readCurrentStack(), serverLevel));
                gun.setLevel((int) affinity * 10);
                gun.setAmmo(gun.getMaxAmmo());
            });
        BetterData.setData(stack, DISTANCE, range); 
    }

    /**
     * Determines whether this goal can start.
     * 
     * @return {@code true} if the mob has a valid target and holds a gun
     */
    @Override
    public boolean canUse() {

        LivingEntity living = this.mob.getTarget();
        if (living == null || !living.isAlive()) return false;
        
        this.target = living;
        return EGun.from(stack) != null;
    }

    /**
     * Determines whether this goal should continue running.
     * 
     * @return {@code true} if {@link #canUse()} is still true
     */
    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    /**
     * Called when the goal starts.
     */
    @Override
    public void start() {

        //GunTrackerPacket packet = new GunTrackerPacket(stack, mob.getId());
        //PigeTechWeaponsNetworking.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob), packet);

        this.mob.setAggressive(false);
    }

    /**
     * Called when the goal stops.
     */
    @Override
    public void stop() {

        //GunTrackerPacket packet = new GunTrackerPacket(stack);
        //PigeTechWeaponsNetworking.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> mob), packet);

        this.target = null;
        this.mob.setAggressive(false);
    }

    /**
     * Called every tick to update the goal.
     */
    @Override
    public void tick() {
        if (target == null) return;
        
        boolean canSee = mob.getSensing().hasLineOfSight(target);
		mob.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(target.getX(), target.getEyeY(), target.getZ()));

        boolean shouldShoot = false;
        if (canSee) {
            seeTime++;

            double distanceSq = mob.distanceToSqr(target);
            double distance = Math.sqrt(distanceSq);

            shouldShoot = distance < range * 0.4;

            if (!shouldShoot && (contactTime == 0 || contactTime > (1.0f - affinity) * 100)) {
                mob.getNavigation().moveTo(target, speed);
                contactTime = 0;
            } else {
                mob.getNavigation().stop();
                contactTime++;
            }
        } else {
            seeTime = 0;
            mob.getNavigation().moveTo(target, speed);
        }
        final boolean canShoot = shouldShoot;
        EGun.process(stack, gun -> {

            Level level = mob.level();
            ItemStack stack = gun.readCurrentStack();

            gun.tick();
            if (cooldown > 0) cooldown--;
            inaccuracy *= 0.8;
                    
            if (gun.getAmmo() == 0 && cooldown == 0) {

                IReload.ReloadAction reloadAction = gun.handleReload(stack, mob);
                RouterUtils.Geckolib.playAnimation(stack, level, mob, EGun.MAIN_CONTROLLER, "reload" + (gun.getShellsCount() > 0 ? (gun.getAmmo() > 0 ? "_full" : "_empty") : ""));

				BlockPos pos = BlockPos.containing(mob.getX(), mob.getY(), mob.getZ());
				SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("pigeon_core", gun.ID + "_reload"));

				level.playSound(null, pos, sound, SoundSource.PLAYERS, 0.32f + (float) range * 0.01f, 1);

                cooldown = reloadAction.cooldown();
                gun.setAmmo(gun.getMaxAmmo());
                return;
            }
            if (gun.getAmmo() > 0 && cooldown == 0 && seeTime > (35 - affinity * 25) && (canShoot || (contactTime < (1.0f - affinity) * 100) && contactTime != 0)) {

                IShoot.ShootAction action = gun.handleShoot(stack, mob);
                        
                if (gun.getDelay() < action.delay() + 1) {
                    gun.increaseDelay();
                            
                    if (gun.getDelay() == action.delay()) gun.setDelay(0);
                    else return;
                }
                EBullet bullet = gun.getBullet(level).initLevel(level);
                float multiplier = 0.3f + ((float) affinity * 0.7f);

                bullet.shoot(mob, gun.getTooltip()[3], gun.getTotalDamage() * multiplier, action.speed(), 
                    gun.getProperties().baseInaccuracy() + inaccuracy, action.power(), action.power() * 2.4 * multiplier);

                gun.decreaseAmmo();
                
                cooldown = action.cooldown();
                inaccuracy += action.inaccuracy() * 2.4f + 1.0f - multiplier;

                RouterUtils.Geckolib.playAnimation(stack, level, mob, EGun.MAIN_CONTROLLER, "shoot");

		        BlockPos pos = BlockPos.containing(mob.getX() + mob.getLookAngle().x, mob.getY(), mob.getZ() + mob.getLookAngle().z);
		        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("pigeon_core", gun.ID + "_shoot"));

		        float pitch = 0.9f + level.random.nextFloat() * 0.2f;
		        level.playSound(null, pos, sound, SoundSource.PLAYERS, 0.45f + (float) range * 0.01f, pitch);
                        
                if (gun.getShellsCount() > 0) {

                    String ejectAnim = ShootUtils.getNextEjectAnimation(stack, level, gun.getShellsCount());
                    RouterUtils.Geckolib.playAnimation(stack, level, mob, EGun.EJECTION_CONTROLLER + ejectAnim.replace("shell_ejection", ""), ejectAnim);
                }
            }
        });
    }

    /**
     * This goal needs to tick every game tick.
     * 
     * @return {@code true}
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * @return the accuracy affinity
     */
    public double getAffinity() {
        return this.affinity;
    }
    
    /**
     * @return the movement speed
     */
    public double getMovementSpeed() {
        return this.speed;
    }
    
    /**
     * @return the engagement range
     */
    public double getRange() {
        return this.range;
    }
    
    /**
     * @return the mob
     */
    public Mob getMob() {
        return this.mob;
    }
    
    /**
     * @return the gun stack
     */
    public ItemStack getGunStack() {
        return this.stack;
    }

	/**
	 * Finds the {@link LightGunnerAiGoal} attached to a mob, if any.
	 * 
	 * @param mob the mob
	 * @return the goal, or {@code null}
	 */
	public static LightGunnerAiGoal getFrom(Mob mob) {
    	for (WrappedGoal wrappedGoal : mob.goalSelector.getAvailableGoals()) {
    	    Goal goal = wrappedGoal.getGoal();
    	    if (goal instanceof LightGunnerAiGoal shooterGoal) return shooterGoal;
    	}
    	return null;
	}
}