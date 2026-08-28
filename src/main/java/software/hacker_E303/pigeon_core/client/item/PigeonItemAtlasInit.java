package software.hacker_E303.pigeon_core.client.item;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import software.hacker_E303.pigeon_core.PigeonCore;

/**
 * Bootstraps the framework item atlas on the client once Minecraft is set up.
 */
@Mod.EventBusSubscriber(modid = PigeonCore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PigeonItemAtlasInit {

    private PigeonItemAtlasInit() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        PigeonItemAtlas.init();
    }
}
