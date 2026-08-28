package software.hacker_E303.pigeon_core.test;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.hacker_E303.pigeon_core.geo.controller.AnimUseEvent;
import software.hacker_E303.pigeon_core.geo.controller.GeoController;
import software.hacker_E303.pigeon_core.geo.controller.GeoControllerContext;
import software.hacker_E303.pigeon_core.geo.entity.GeoEMob;

/**
 * Test geo entity registered as {@code test_geo}.
 */
public class TestGeo extends GeoEMob {

    /**
     * Constructs a new TestGeo.
     *
     * @param mob   the entity type
     * @param level the level
     */
    public TestGeo(EntityType<? extends GeoEMob> mob, Level level) {
        super(mob, level);
    }

    /**
     * Returns default sound behavior for this geo entity.
     */
    @Override
    public BehaviorSounds getSounds() {
        return null;
    }

    /**
     * Registers animation controllers for this geo entity.
     */
    @Override
    public void registerControllers(GeoControllerContext ctx) {
        ctx.add(GeoController.create("test", "idle", 1, this)
            .anim("sad", false, AnimUseEvent.OTHER).trigger("no", false)
        );
    }

    /**
     * Adds custom AI goals to this geo entity.
     */
    @Override
    public void addCustomGoals() {
    }
}
