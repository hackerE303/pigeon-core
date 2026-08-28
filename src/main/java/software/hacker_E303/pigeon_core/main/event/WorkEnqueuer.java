package software.hacker_E303.pigeon_core.main.event;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;

/**
 * Holds delayed work items that are executed after a configurable number of ticks.
 * Designed to be called from mod setup/loading threads, then drained by
 * {@link PigeEvents} on the game thread.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class WorkEnqueuer {
    
    protected static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> serverWork = new ConcurrentLinkedQueue<>();
    protected static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> clientWork = new ConcurrentLinkedQueue<>();

    /**
     * Enqueues work to run on the server after {@code ticks} ticks.
     * No-op if called from a non-server thread.
     *
     * @param ticks  delay in game ticks
     * @param action the runnable to execute
     */
    public static void addToServer(int ticks, Runnable action) {
        
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
            serverWork.add(new AbstractMap.SimpleEntry<>(action, ticks));
    }

    /**
     * Enqueues work to run on the client after {@code ticks} ticks.
     * No-op if called from a non-client thread.
     *
     * @param ticks  delay in game ticks
     * @param action the runnable to execute
     */
    public static void addToClient(int ticks, Runnable action) {
        
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.CLIENT)
            clientWork.add(new AbstractMap.SimpleEntry<>(action, ticks));
    }
}