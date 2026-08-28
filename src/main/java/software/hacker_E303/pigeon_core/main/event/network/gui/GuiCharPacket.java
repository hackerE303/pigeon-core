package software.hacker_E303.pigeon_core.main.event.network.gui;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import software.hacker_E303.pigeon_core.common.PigeGui;
import software.hacker_E303.pigeon_core.common.gui.PigeAutoContainer;

/**
 * Sent client → server when the player types a character (charTyped)
 * or presses a consumed special key (keyPressed) while a PigeGui is open.
 *
 * <p>If {@code c != 0} it is a charTyped event; otherwise {@code keyCode} carries
 * the GLFW key code (e.g. 259 = Backspace).
 */
public class GuiCharPacket {

    private final char c;
    private final int  keyCode;

    /**
     * Creates a charTyped packet.
     *
     * @param c the typed character
     */
    /** For charTyped events. */
    public GuiCharPacket(char c) {
        this.c       = c;
        this.keyCode = -1;
    }

    /**
     * Creates a keyPressed packet (sent when {@code c == 0}).
     *
     * @param keyCode the GLFW key code (e.g. 259 = Backspace)
     */
    /** For keyPressed events (c == 0 sentinel). */
    public GuiCharPacket(int keyCode) {
        this.c       = (char) 0;
        this.keyCode = keyCode;
    }

    /**
     * Writes this packet to the network buffer.
     *
     * @param msg the packet to write
     * @param buf the destination buffer
     */
    public static void encode(GuiCharPacket msg, FriendlyByteBuf buf) {
        buf.writeInt((int) msg.c);
        buf.writeInt(msg.keyCode);
    }

    /**
     * Reads a packet from the network buffer.
     *
     * @param buf the source buffer
     * @return the decoded packet
     */
    public static GuiCharPacket decode(FriendlyByteBuf buf) {
        char c       = (char) buf.readInt();
        int  keyCode = buf.readInt();
        return c != 0 ? new GuiCharPacket(c) : new GuiCharPacket(keyCode);
    }

    /**
     * Handles the packet on the server.
     *
     * @param msg the received packet
     * @param ctx the network context supplier
     */
    public static void handle(GuiCharPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.containerMenu instanceof PigeAutoContainer pac)) return;
            PigeGui gui = pac.getGui();
            if (gui == null) return;
            gui.handleCharInput(msg.c, msg.keyCode, pac.getSenderEntity(player.level()), player);
        });
        ctx.get().setPacketHandled(true);
    }
}
