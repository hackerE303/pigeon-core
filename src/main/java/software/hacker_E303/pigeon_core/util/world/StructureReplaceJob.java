package software.hacker_E303.pigeon_core.util.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.hacker_E303.pigeon_core.PigeonCore;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Replaces every instance of one block with another within a volume, spread across
 * multiple server ticks so large areas don't stall the server.
 *
 * <p>Only one job runs at a time (queued jobs wait their turn) — self-contained,
 * mirrors {@link software.hacker_E303.pigeon_core.main.event.render.PerimeterRenderer}'s
 * static-queue + own event-subscriber style.
 */
@Mod.EventBusSubscriber(modid = PigeonCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StructureReplaceJob {

    private static final ConcurrentLinkedQueue<StructureReplaceJob> QUEUE = new ConcurrentLinkedQueue<>();

    /**
     * Blocks processed per server tick. Kept conservative: Minecraft's light engine
     * recalculates on every setBlock call that changes block opacity, and that cost is
     * NOT avoidable via setBlock flags — it's the real bottleneck on large areas, not
     * neighbor/redstone updates. Tune empirically in-game before raising this.
     */
    private static final int BLOCKS_PER_TICK = 1024;

    private static final int UPDATE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_KNOWN_SHAPE;

    private final ServerLevel level;
    private final BlockPos min;
    private final BlockPos max;
    private final Block source;
    private final BlockState targetState;
    private final Player feedbackPlayer;
    private final Runnable onComplete;

    private int cx, cy, cz;
    private int replaced = 0;
    private int skippedUnloaded = 0;

    private StructureReplaceJob(ServerLevel level, BlockPos corner1, BlockPos corner2,
                                 Block source, Block target, Player feedbackPlayer, Runnable onComplete) {
        this.level = level;
        this.min = new BlockPos(
                Math.min(corner1.getX(), corner2.getX()),
                Math.min(corner1.getY(), corner2.getY()),
                Math.min(corner1.getZ(), corner2.getZ()));
        this.max = new BlockPos(
                Math.max(corner1.getX(), corner2.getX()),
                Math.max(corner1.getY(), corner2.getY()),
                Math.max(corner1.getZ(), corner2.getZ()));
        this.source = source;
        this.targetState = target.defaultBlockState();
        this.feedbackPlayer = feedbackPlayer;
        this.onComplete = onComplete;
        this.cx = min.getX();
        this.cy = min.getY();
        this.cz = min.getZ();
    }

    /**
     * Queues a batched replace of {@code source} with {@code target} across the volume,
     * running a bounded number of blocks per tick. {@code onComplete} runs once the job
     * finishes (server-side) — use it to invalidate any cached block counts.
     */
    public static void enqueue(ServerLevel level, BlockPos corner1, BlockPos corner2,
                                Block source, Block target, Player feedbackPlayer, Runnable onComplete) {
        QUEUE.add(new StructureReplaceJob(level, corner1, corner2, source, target, feedbackPlayer, onComplete));
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUE.isEmpty()) return;
        StructureReplaceJob job = QUEUE.peek();
        if (job != null && job.tick()) QUEUE.poll();
    }

    /** Processes up to {@link #BLOCKS_PER_TICK} positions. Returns {@code true} once the job is complete. */
    private boolean tick() {
        for (int processed = 0; processed < BLOCKS_PER_TICK; processed++) {
            BlockPos pos = new BlockPos(cx, cy, cz);
            if (!level.isLoaded(pos)) {
                skippedUnloaded++;
            } else {
                BlockState state = level.getBlockState(pos);
                if (state.is(source)) {
                    level.setBlock(pos, targetState, UPDATE_FLAGS);
                    replaced++;
                }
            }

            if (!advance()) {
                complete();
                return true;
            }
        }
        return false;
    }

    /** Advances the cursor to the next position. Returns {@code false} once the volume is exhausted. */
    private boolean advance() {
        cz++;
        if (cz > max.getZ()) {
            cz = min.getZ();
            cy++;
            if (cy > max.getY()) {
                cy = min.getY();
                cx++;
                if (cx > max.getX()) return false;
            }
        }
        return true;
    }

    private void complete() {
        if (onComplete != null) onComplete.run();
        if (feedbackPlayer != null) {
            String messageKey = targetState.is(net.minecraft.world.level.block.Blocks.AIR)
                    ? "structure_tool.remove_done"
                    : "structure_tool.replace_done";
            software.hacker_E303.pigeon_core.util.world.PlayerUtils.with("pigeon_core", feedbackPlayer, ctx ->
                    ctx.debugMessage(messageKey, replaced, skippedUnloaded));
        }
    }
}
