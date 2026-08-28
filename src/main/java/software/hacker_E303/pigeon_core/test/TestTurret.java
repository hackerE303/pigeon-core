package software.hacker_E303.pigeon_core.test;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.hacker_E303.pigeon_core.entity.EBullet;
import software.hacker_E303.pigeon_core.entity.ETurret;
import software.hacker_E303.pigeon_core.main.AutoRegister;

/**
 * Test turret entity registered as {@code test_turret}.
 */
@AutoRegister("test_turret")
public class TestTurret extends ETurret {

    /**
     * Constructs a new TestTurret.
     *
     * @param turret the entity type
     * @param level  the level
     */
    public TestTurret(EntityType<? extends ETurret> turret, Level level) {
        super(turret, level);
    }

    /**
     * Defines the shoot behavior for this turret.
     */
    @Override
    public ShootAction handleShoot(ItemStack stack, Entity entity) {
        return ShootAction.create(1, 1, 1, 1, 1, 1);
    }

    /**
     * Returns the animation definitions for this turret.
     */
    @Override
    public Animations getAnimations() {
        return new Animations();
    }

    private static final AnimationManagers MANAGERS = new AnimationManagers(TestTurret.class, 1);

    /**
     * Returns the animation managers for this turret.
     */
    @Override
    public AnimationManagers getAnimationManagers() {
        return MANAGERS;
    }

    /**
     * Returns the bullet fired by this turret.
     */
    @Override
    public EBullet getBullet() {
        return null;
    }

    /**
     * Returns the turret type classification.
     */
    @Override
    public Type getTurretType() {
        return ETurret.Type.MACHINE_GUN;
    }

    /**
     * Adds custom AI goals to this turret.
     */
    @Override
    public void addCustomGoals() {
    }
}
