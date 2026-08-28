package software.hacker_E303.pigeon_core.main.event.network.debug;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import software.hacker_E303.pigeon_core.main.event.network.RouterUtils;

/**
 * Server-to-client packet that asks the client to verify a resource is present
 * and, if missing, send a {@link ResourceWarningPacket} back to the server.
 */
public class CheckTexturePacket {

    private final ResourceLocation location;
    private final boolean everyone;

    /**
     * Creates a new texture-check packet.
     *
     * @param location the resource location to verify
     * @param everyone true to forward the warning to all players if missing
     */
    public CheckTexturePacket(ResourceLocation location, boolean everyone) {
        this.location = location;
        this.everyone = everyone;
    }

    /**
     * Writes this packet to the network buffer.
     *
     * @param packet the packet to write
     * @param buf    the destination buffer
     */
    public static void encode(CheckTexturePacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.location);
        buf.writeBoolean(packet.everyone);
    }

    /**
     * Reads a packet from the network buffer.
     *
     * @param buf the source buffer
     * @return the decoded packet
     */
    public static CheckTexturePacket decode(FriendlyByteBuf buf) {
        return new CheckTexturePacket(buf.readResourceLocation(), buf.readBoolean());
    }

    /**
     * Handles the packet on the client.
     *
     * @param packet  the received packet
     * @param supplier the network context supplier
     */
    public static void handle(CheckTexturePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {

            if (!Minecraft.getInstance().getResourceManager().getResource(packet.location).isPresent())
                RouterUtils.Debug.resourceWarning(packet.location, packet.everyone);
        });
        context.setPacketHandled(true);
    }
}