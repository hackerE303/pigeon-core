package software.hacker_E303.pigeon_core.util.misc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Shooting helpers for freeze trails, particle spawning, and animation cycling.
 */
@Mod.EventBusSubscriber
public class ShootUtils {

    private static final List<FreezeTrail> activeTrails = new ArrayList<>();

    /**
     * Spawns a freeze trail from the player's eye position toward their look vector.
     *
     * @param level      the server level
     * @param player     the player shooting
     * @param maxDistance the maximum ray-trace distance
     * @param tickDelay  the number of ticks between trail steps
     */
    public static void spawnFreezeTrail(ServerLevel level, Player player, double maxDistance, int tickDelay) {
        FreezeTrail trail = new FreezeTrail(level, player, maxDistance, 2.0, 2.6, tickDelay);
        activeTrails.add(trail);
    }

    /**
     * Spawns a freeze trail with a default tick delay of 1.
     *
     * @param level      the server level
     * @param player     the player shooting
     * @param maxDistance the maximum ray-trace distance
     */
    public static void spawnFreezeTrail(ServerLevel level, Player player, double maxDistance) {
        spawnFreezeTrail(level, player, maxDistance, 1);
    }

    /**
     * Advances all active freeze trails each server tick and removes completed ones.
     *
     * @param event the server tick event
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<FreezeTrail> it = activeTrails.iterator();
        while (it.hasNext()) {
            FreezeTrail trail = it.next();
            boolean alive = trail.tick();
            if (!alive) it.remove();
        }
    }

	private static class FreezeTrail {

        //private final ServerLevel level;
        private final Vec3 dir;
        private final double step;
        private final int totalSteps;
        private final int tickDelay;
        private Vec3 currentPos;
        private int stepsSpawned = 0;
        private int tickCounter = 0;

        public FreezeTrail(ServerLevel level, Player player, double maxDistance, double startOffset, double step, int tickDelay) {
            //this.level = level;

            Vec3 start = player.getEyePosition();
            Vec3 look = player.getLookAngle().normalize();
            Vec3 end = start.add(look.scale(maxDistance));

            ClipContext ctx = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
            BlockHitResult hit = level.clip(ctx);
            Vec3 hitPos = hit.getLocation();

            Vec3 totalDir = hitPos.subtract(start);
            double dist = totalDir.length();

            this.dir = totalDir.normalize();
            this.step = step;
            this.tickDelay = Math.max(1, tickDelay);
            this.currentPos = start.add(this.dir.scale(startOffset));
            this.totalSteps = (int) ((dist - startOffset) / step);
        }

        public boolean tick() {
            tickCounter++;
            if (tickCounter % tickDelay != 0) return true;

            if (stepsSpawned >= totalSteps) return false;

            //level.sendParticles(PigeTechWeaponsModParticleTypes.FREEZE_PARTICLE.get(), currentPos.x, currentPos.y, currentPos.z, 1, 0,  0, 0, 0);

            currentPos = currentPos.add(dir.scale(step));
            stepsSpawned++;
            return true;
        }
    }

    private static final String SHOOT_INT = "nextIntToShoot";
    private static final String EJECT_INT = "nextIntToEject";

	public static String getNextShootAnimation(ItemStack item, Level level, int shootVariations) {
		if (level.isClientSide || shootVariations == 0) return "shoot0";
		if (!item.getTag().contains(SHOOT_INT)) item.getOrCreateTag().putInt(SHOOT_INT, 1);

		int intBefore = item.getTag().getInt(SHOOT_INT);
		if (item.getTag().getInt(SHOOT_INT) < shootVariations) item.getTag().putInt(SHOOT_INT, intBefore + 1);
		else item.getTag().putInt(SHOOT_INT, 1);

		return "shoot" + intBefore;
	}

    public static String getNextEjectAnimation(ItemStack item, Level level, int ejectVariations) {

        if (level.isClientSide || ejectVariations <= 0) return "shell_ejection0";

        CompoundTag tag = item.getOrCreateTag();
        int current = tag.getInt(EJECT_INT);

        int next = (current + 1) % ejectVariations;
        tag.putInt(EJECT_INT, next);

        return "shell_ejection" + current;
    }
}