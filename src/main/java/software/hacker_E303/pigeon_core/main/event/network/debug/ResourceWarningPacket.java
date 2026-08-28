package software.hacker_E303.pigeon_core.main.event.network.debug;

import software.hacker_E303.pigeon_core.init.PigeUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Client-to-server packet requesting a missing-resource warning.
 */
public class ResourceWarningPacket {

    private final ResourceLocation location;
    private final boolean everyone;

    /**
     * Creates a new resource warning packet.
     *
     * @param location the resource location that failed to load
     * @param everyone true to warn all players, false to warn only the sender
     */
    public ResourceWarningPacket(ResourceLocation location, boolean everyone) {
        this.location = location;
        this.everyone = everyone;
    }

    /**
     * Writes this packet to the network buffer.
     *
     * @param packet the packet to write
     * @param buf    the destination buffer
     */
    public static void encode(ResourceWarningPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.location);
        buf.writeBoolean(packet.everyone);
    }

    /**
     * Reads a packet from the network buffer.
     *
     * @param buf the source buffer
     * @return the decoded packet
     */
    public static ResourceWarningPacket decode(FriendlyByteBuf buf) {
        return new ResourceWarningPacket(buf.readResourceLocation(), buf.readBoolean());
    }

    /**
     * Handles the packet on the server.
     *
     * @param packet  the received packet
     * @param supplier the network context supplier
     * @return true if the packet was handled
     */
    public static boolean handle(ResourceWarningPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {

            ServerPlayer player = context.getSender();
            if (player == null) return;
             if (!packet.everyone) PigeUtils.missingResourceWarning(player.level(), packet.location);
            else PigeUtils.missingResourceWarning(player, packet.location);
        });
        return true;
    }
}