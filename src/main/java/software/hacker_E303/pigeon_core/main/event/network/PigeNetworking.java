package software.hacker_E303.pigeon_core.main.event.network;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import software.hacker_E303.pigeon_core.PigeonCore;
import software.hacker_E303.pigeon_core.main.event.network.geo.ItemAnimationPacket;
import software.hacker_E303.pigeon_core.main.event.network.gui.GuiButtonPacket;
import software.hacker_E303.pigeon_core.main.event.network.gui.GuiCharPacket;
import software.hacker_E303.pigeon_core.main.event.network.gun.BulletHolePacket;
import software.hacker_E303.pigeon_core.main.event.network.gun.GunHoldPacket;
import software.hacker_E303.pigeon_core.main.event.network.gun.GunReloadPacket;
import software.hacker_E303.pigeon_core.main.event.network.misc.MouseHeldPacket;
import software.hacker_E303.pigeon_core.main.event.network.misc.StructureCornersPacket;
import software.hacker_E303.pigeon_core.main.event.network.debug.*;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Registers and owns the mod's simple network channel.
 */
public final class PigeNetworking {

    private static final String PROTOCOL_VERSION = "1";

    private static int packetId = 0;

    /**
     * Returns a new unique packet id.
     *
     * @return the next packet id
     */
    private static int newKey() {
        return packetId++;
    }

    protected static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(

            Location.create(Path.NONE, "network").from(PigeonCore.MOD_ID),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    /**
     * Sends a message from the client to the server.
     *
     * @param msg the packet to send
     */
    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    /**
     * Registers all mod packet handlers on the network channel.
     */
    public static void init() {

        // GUI PACKETS
        CHANNEL.messageBuilder(GuiButtonPacket.class, newKey(), NetworkDirection.PLAY_TO_SERVER)
            .encoder(GuiButtonPacket::encode)
            .decoder(GuiButtonPacket::decode)
            .consumerMainThread(GuiButtonPacket::handle)
            .add();

        CHANNEL.messageBuilder(GuiCharPacket.class, newKey(), NetworkDirection.PLAY_TO_SERVER)
            .encoder(GuiCharPacket::encode)
            .decoder(GuiCharPacket::decode)
            .consumerMainThread(GuiCharPacket::handle)
            .add();

        // GUN PACKETS
        CHANNEL.messageBuilder(GunHoldPacket.class, newKey(), NetworkDirection.PLAY_TO_CLIENT)
            .encoder(GunHoldPacket::encode)
            .decoder(GunHoldPacket::decode)
            .consumerMainThread(GunHoldPacket::handle)
            .add();

        CHANNEL.messageBuilder(GunReloadPacket.class, newKey(), NetworkDirection.PLAY_TO_SERVER)
            .encoder(GunReloadPacket::encode)
            .decoder(GunReloadPacket::decode)
            .consumerMainThread(GunReloadPacket::handle)
            .add();

        CHANNEL.messageBuilder(ItemAnimationPacket.class, newKey(), NetworkDirection.PLAY_TO_CLIENT)
            .encoder(ItemAnimationPacket::encode)
            .decoder(ItemAnimationPacket::decode)
            .consumerMainThread(ItemAnimationPacket::handle)
            .add();

        CHANNEL.messageBuilder(BulletHolePacket.class, newKey(), NetworkDirection.PLAY_TO_CLIENT)
            .encoder(BulletHolePacket::encode)
            .decoder(BulletHolePacket::decode)
            .consumerMainThread(BulletHolePacket::handle)
            .add();

        // MISC PACKETS
        CHANNEL.messageBuilder(MouseHeldPacket.class, newKey(), NetworkDirection.PLAY_TO_SERVER)
            .encoder(MouseHeldPacket::encode)
            .decoder(MouseHeldPacket::decode)
            .consumerMainThread(MouseHeldPacket::handle)
            .add();

        CHANNEL.messageBuilder(StructureCornersPacket.class, newKey(), NetworkDirection.PLAY_TO_SERVER)
            .encoder(StructureCornersPacket::encode)
            .decoder(StructureCornersPacket::decode)
            .consumerMainThread(StructureCornersPacket::handle)
            .add();

        CHANNEL.messageBuilder(ResourceWarningPacket.class, newKey(), NetworkDirection.PLAY_TO_SERVER)
            .encoder(ResourceWarningPacket::encode)
            .decoder(ResourceWarningPacket::decode)
            .consumerMainThread(ResourceWarningPacket::handle)
            .add();

        // DEBUG PACKETS
        CHANNEL.messageBuilder(CheckTexturePacket.class, newKey(), NetworkDirection.PLAY_TO_CLIENT)
            .encoder(CheckTexturePacket::encode)
            .decoder(CheckTexturePacket::decode)
            .consumerMainThread(CheckTexturePacket::handle)
            .add();
    }
}