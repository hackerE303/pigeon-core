package software.hacker_E303.pigeon_core.main.event.network.misc;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import software.hacker_E303.pigeon_core.main.event.network.RouterUtils;

/**
 * Client-to-server packet reporting the current left/right mouse held state.
 */
public class MouseHeldPacket {

    private final boolean left;
    private final boolean right;
    private final boolean pressed;

    /**
     * Creates a new mouse-held packet.
     *
     * @param left    true if this packet concerns the left button
     * @param right   true if this packet concerns the right button
     * @param pressed true if the button was pressed, false if released
     */
    public MouseHeldPacket(boolean left, boolean right, boolean pressed) {
        this.left = left;
        this.right = right;
        this.pressed = pressed;
    }

    /**
     * Writes this packet to the network buffer.
     *
     * @param msg the packet to write
     * @param buf the destination buffer
     */
    public static void encode(MouseHeldPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.left);
        buf.writeBoolean(msg.right);
        buf.writeBoolean(msg.pressed);
    }

    /**
     * Reads a packet from the network buffer.
     *
     * @param buf the source buffer
     * @return the decoded packet
     */
    public static MouseHeldPacket decode(FriendlyByteBuf buf) {
        return new MouseHeldPacket(buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    /**
     * Handles the packet on the server.
     *
     * @param msg the received packet
     * @param ctx the network context supplier
     */
    public static void handle(MouseHeldPacket msg, Supplier<NetworkEvent.Context> ctx) {

        ctx.get().enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            RouterUtils.Mouse.MouseHeldData data =
                RouterUtils.Mouse.MouseHeldData.get(player);

            if (msg.left) data.setLeftHeld(msg.pressed);
            if (msg.right) data.setRightHeld(msg.pressed);
        });
        ctx.get().setPacketHandled(true);
    }
}