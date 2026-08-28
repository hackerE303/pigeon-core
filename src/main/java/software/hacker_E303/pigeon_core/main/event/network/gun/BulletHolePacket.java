package software.hacker_E303.pigeon_core.main.event.network.gun;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import software.hacker_E303.pigeon_core.main.event.render.BulletHoleRender;

/**
 * Network packet for rendering bullet hole decals on the client.
 */
public class BulletHolePacket {

    private final double x, y, z;
    private final Direction face;

    /**
     * Creates a new bullet hole packet.
     *
     * @param x    the world x position
     * @param y    the world y position
     * @param z    the world z position
     * @param face the hit face direction
     */
    public BulletHolePacket(double x, double y, double z, Direction face) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
    }

    /**
     * Encodes the packet data into the buffer.
     *
     * @param msg the packet to encode
     * @param buf the destination buffer
     */
    public static void encode(BulletHolePacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeEnum(msg.face);
    }

    /**
     * Decodes the packet data from the buffer.
     *
     * @param buf the source buffer
     * @return a new {@link BulletHolePacket}
     */
    public static BulletHolePacket decode(FriendlyByteBuf buf) {
        return new BulletHolePacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readEnum(Direction.class));
    }

    /**
     * Handles the packet on the client side.
     *
     * @param msg the decoded packet
     * @param ctx the network event context supplier
     */
    public static void handle(BulletHolePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            
            if (Minecraft.getInstance().level != null)
                BulletHoleRender.add(msg.x, msg.y, msg.z, msg.face);
        });
        ctx.get().setPacketHandled(true);
    }
}