package software.hacker_E303.pigeon_core.entity.common.stats;

import net.minecraft.world.entity.LivingEntity;
import software.hacker_E303.pigeon_core.entity.ETurret;

/**
 * Mutable turret-specific stats, extending {@link MobStats}.
 */
public final class TurretStats extends MobStats {

    /**
     * Constructs turret stats for the given modid and key.
     *
     * @param modid the mod id
     * @param key   the stats identifier
     */
    public TurretStats(String modid, String key) {
        super(modid, key);
    }

    /**
     * @return the shoot range attribute value
     */
    public double getShootRange() {
        return this.get("shoot_range");
    }

    /**
     * Sets the shoot range attribute value.
     *
     * @param value the new shoot range
     */
    public void setShootRange(double value) {
        this.set("shoot_range", value);
    }

    /**
     * @return the power duration attribute value in ticks
     */
    public long getPowerDuration() {
        return (long) this.get("power_duration");
    }

    /**
     * Sets the power duration attribute value.
     *
     * @param value the new power duration in ticks
     */
    public void setPowerDuration(long value) {
        this.set("power_duration", value);
    }

    /**
     * Gets an attribute value, overriding speed to 0.0 for turrets.
     *
     * @param attribute the attribute key
     * @return the attribute base value
     */
    @Override
    public double get(String attribute) {
        if (attribute.equals("speed")) return 0.0;
        return super.get(attribute);
    }

    /**
     * Injects the living entity owner into this stats instance.
     *
     * @param living the owning living entity
     * @return this stats instance for chaining
     */
    @Override
    protected TurretStats injectOwner(LivingEntity living) {
        this.living = living;
        return this;
    }

    /**
     * Creates turret stats for the given turret.
     *
     * @param turret the turret
     * @param modid  the mod id
     * @param key    the stats identifier
     * @return a new {@link TurretStats} instance bound to the turret
     */
    public static TurretStats create(ETurret turret, String modid, String key) {
        return new TurretStats(modid, key).injectOwner(turret);
    }
}
