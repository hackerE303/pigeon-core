package software.hacker_E303.pigeon_core.entity.common.stats;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Mutable mob-specific stats, extending {@link LivingStats}.
 */
public class MobStats extends LivingStats {

    /**
     * Constructs mob stats for the given modid and key.
     *
     * @param modid the mod id
     * @param key   the stats identifier
     */
    public MobStats(String modid, String key) {
        super(modid, key);
    }

    /**
     * @return the attack damage attribute value
     */
    public double getDamage() {
        return this.get("attack.damage");
    }

    /**
     * Sets the attack damage attribute value.
     *
     * @param value the new damage value
     */
    public void setDamage(double value) {
        this.set("attack.damage", value);
    }

    /**
     * Injects the living entity owner into this stats instance.
     *
     * @param living the owning living entity
     * @return this stats instance for chaining
     */
    @Override
    protected MobStats injectOwner(LivingEntity living) {
        this.living = living;
        return this;
    }

    /**
     * Creates mob stats for the given mob.
     *
     * @param mob    the mob
     * @param modid  the mod id
     * @param key    the stats identifier
     * @return a new {@link MobStats} instance bound to the mob
     */
    public static MobStats create(Mob mob, String modid, String key) {
        return new MobStats(modid, key).injectOwner(mob);
    }
}
