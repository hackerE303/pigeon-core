package software.hacker_E303.pigeon_core.init;

import software.hacker_E303.pigeon_core.main.EResources;
import software.hacker_E303.pigeon_core.util.locator.MultiLocation;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Registers mod resources: creative tabs, sounds, entity stats, spawns, and attributes.
 */
public final class PigeResources extends EResources {

    /**
     * Registers creative-mode tabs.
     *
     * @param ctx the tab context
     */
    @Override
    public void registerTabs(TabContext ctx) {
    }

    /**
     * Registers mod sound events grouped by category.
     *
     * @param ctx the sound context
     */
    @Override
    public void registerSounds(SoundContext ctx) {

        ctx.add(MultiLocation.create(Path.SOUND.GUNS,
                "gun.prev_hold", "gun.trigger", "gun.shell_ejection", 
                "gun.step.light", "gun.step.heavy", "gun.step.fall"))

            .add(MultiLocation.create(Path.SOUND.TURRETS,
                "turret.hurt", "turret.interaction",
                "turret.rotating.yaw", "turret.rotating.pitch_up", "turret.rotating.pitch_down"))

            .add(MultiLocation.create(Path.SOUND.MISC,
                "ricochet", "button", "notify_1", "notify_2"));
    }

    /**
     * Registers entity stats definitions.
     *
     * @param ctx the entity stats context
     */
    @Override
    public void registerEntityStats(EntityStatsContext ctx) {
    }

    /**
     * Registers natural spawn definitions.
     *
     * @param ctx the entity spawns context
     */
    @Override
    public void registerEntitySpawns(EntitySpawnsContext ctx) {
    }

    /**
     * Registers entity attributes.
     *
     * @param ctx the attribute context
     */
    @Override
    public void registerAttributes(AttributeContext ctx) {

        ctx.add("shoot_range", 20.0, 0, 1024)
            .add("power_duration", 1200, 1, Integer.MAX_VALUE);
    }
}