package software.hacker_E303.pigeon_core.main.event;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import software.hacker_E303.pigeon_core.main.event.network.PigeNetworking;

/**
 * Mod event bus subscribers for setup and tick processing.
 */
public final class PigeEvents {

    /**
     * Mod lifecycle events (common setup).
     */
    @Mod.EventBusSubscriber(modid = "pigeon_core", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        
        /**
         * Initializes networking on the common setup event.
         *
         * @param event the common setup event
         */
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> PigeNetworking.init());
        }
    }

    /**
     * Game-tick event handlers for delayed work queues.
     */
    @Mod.EventBusSubscriber(modid = "pigeon_core", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class LoaderEvents {

        /**
         * Drains the server work queue at the end of each server tick.
         *
         * @param event the server tick event
         */
        @SubscribeEvent
        public static void serverTick(TickEvent.ServerTickEvent event) {

            if (event.phase == TickEvent.Phase.END) {
                List<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
                
                WorkEnqueuer.serverWork.forEach(work -> {
                    
                    work.setValue(work.getValue() - 1);
                    if (work.getValue() == 0) actions.add(work);
                });
                actions.forEach(e -> e.getKey().run());
                WorkEnqueuer.serverWork.removeAll(actions);
            }
        }

        /**
         * Drains the client work queue at the end of each client tick.
         *
         * @param event the client tick event
         */
        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {

            if (event.phase == TickEvent.Phase.END) {
                List<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
                
                WorkEnqueuer.clientWork.forEach(work -> {
                    
                    work.setValue(work.getValue() - 1);
                    if (work.getValue() == 0) actions.add(work);
                });
                actions.forEach(e -> e.getKey().run());
                WorkEnqueuer.clientWork.removeAll(actions);
            }
        }
    }
}