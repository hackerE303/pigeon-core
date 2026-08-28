package software.hacker_E303.pigeon_core.main.event.network.gun;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import software.hacker_E303.pigeon_core.geo.item.gun.EGun;

/**
 * Client-to-server packet requesting a gun reload.
 */
public class GunReloadPacket {

    /**
     * Creates an empty reload packet (no payload needed).
     */
    public GunReloadPacket() {
    }

    /**
     * Writes this packet to the network buffer (no payload).
     *
     * @param msg    the packet to write
     * @param buffer the destination buffer
     */
    public static void encode(GunReloadPacket msg, FriendlyByteBuf buffer) {
    }

    /**
     * Reads a packet from the network buffer.
     *
     * @param buffer the source buffer
     * @return the decoded packet
     */
    public static GunReloadPacket decode(FriendlyByteBuf buffer) {
        return new GunReloadPacket();
    }

    /**
     * Handles the packet on the server.
     *
     * @param msg the received packet
     * @param ctx the network context supplier
     */
    public static void handle(GunReloadPacket msg, Supplier<NetworkEvent.Context> ctx) {

        ctx.get().enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            EGun.process(stack, gun -> gun.reload(player));
        });
        ctx.get().setPacketHandled(true);
    }
}