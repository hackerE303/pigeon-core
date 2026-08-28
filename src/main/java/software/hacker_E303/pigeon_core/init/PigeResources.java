package software.hacker_E303.pigeon_core.init;

import net.minecraft.world.entity.MobCategory;
import software.hacker_E303.pigeon_core.common.Tab;
import software.hacker_E303.pigeon_core.entity.common.stats.InitStats;
import software.hacker_E303.pigeon_core.entity.common.spawn.SpawnPlace;
import software.hacker_E303.pigeon_core.entity.common.spawn.SpawnDefinition;
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

        ctx.add(Tab.create("pigeon_core", "test_item", "test_turret").isCreative("test_item"));
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
        
        ctx.add(
            InitStats.create("test_turret").turret().health(10)
                .boundingBox(2, 10, 5, 7).shootRange(400));
    }

    /**
     * Registers natural spawn definitions.
     *
     * @param ctx the entity spawns context
     */
    @Override
    public void registerEntitySpawns(EntitySpawnsContext ctx) {
        ctx.add(SpawnPlace.create("test_turret").define(
            SpawnDefinition.create("plains").min(1).max(2).weight(5000000),
            SpawnDefinition.create("#minecraft:underground").min(6).max(7).weight(3).category(MobCategory.MISC)
        ));
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