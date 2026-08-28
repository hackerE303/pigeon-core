package software.hacker_E303.pigeon_core.test;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import software.hacker_E303.pigeon_core.entity.EMob;
import software.hacker_E303.pigeon_core.main.AutoRegister;

/**
 * Test mob entity registered as {@code test_mob}.
 */
@AutoRegister("test_mob")
public class TestMob extends EMob {

    /**
     * Constructs a new TestMob.
     *
     * @param mob   the entity type
     * @param level the level
     */
    public TestMob(EntityType<? extends EMob> mob, Level level) {
        super(mob, level);
    }

    /**
     * Returns default sound behavior for this mob.
     */
    @Override
    public BehaviorSounds getSounds() {
        return BehaviorSounds.create(null, null, null, null, 0.0f, 0.0f);
    } // Params: ambient, death, hurt, step, volume, pitch

    /**
     * Adds custom AI goals to this mob.
     */
    @Override
    public void addCustomGoals() {
    }
}
