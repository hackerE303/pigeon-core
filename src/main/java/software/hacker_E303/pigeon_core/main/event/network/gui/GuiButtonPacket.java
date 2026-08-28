package software.hacker_E303.pigeon_core.main.event.network.gui;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import software.hacker_E303.pigeon_core.common.gui.GuiContext;
import software.hacker_E303.pigeon_core.common.gui.PigeAutoContainer;
import software.hacker_E303.pigeon_core.common.gui.PressAction;

/**
 * Client-to-server packet for a GUI button press.
 */
public class GuiButtonPacket {

    private final int buttonIndex;

    /**
     * Creates a new button press packet.
     *
     * @param buttonIndex the zero-based button index in the current GUI
     */
    public GuiButtonPacket(int buttonIndex) {
        this.buttonIndex = buttonIndex;
    }

    /**
     * Writes this packet to the network buffer.
     *
     * @param msg the packet to write
     * @param buf the destination buffer
     */
    public static void encode(GuiButtonPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.buttonIndex);
    }

    /**
     * Reads a packet from the network buffer.
     *
     * @param buf the source buffer
     * @return the decoded packet
     */
    public static GuiButtonPacket decode(FriendlyByteBuf buf) {
        return new GuiButtonPacket(buf.readInt());
    }

    /**
     * Handles the packet on the server.
     *
     * @param msg the received packet
     * @param ctx the network context supplier
     */
    public static void handle(GuiButtonPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (!(player.containerMenu instanceof PigeAutoContainer autoContainer)) return;
            if (autoContainer.getGui() == null) return;

            GuiContext serverCtx = new GuiContext();
            autoContainer.callRenderInterface(serverCtx, player);

            List<GuiContext.ButtonElement> buttons = serverCtx.data().buttons();
            if (msg.buttonIndex < 0 || msg.buttonIndex >= buttons.size()) return;

            PressAction action = new PressAction(false, player);
            try {
                buttons.get(msg.buttonIndex).action().accept(action);
            } catch (PressAction.Abort ignored) {}
        });
        ctx.get().setPacketHandled(true);
    }
}