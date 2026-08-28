package software.hacker_E303.pigeon_core.main.event.network.gun;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import software.hacker_E303.pigeon_core.client.gun.animation.AnimationManager;
import software.hacker_E303.pigeon_core.geo.item.gun.EGun;
import software.hacker_E303.pigeon_core.gun.gear.GunTracker;

/**
 * Client-bound packet that notifies the client when a player starts holding a gun.
 */
public class GunHoldPacket {

    private final ItemStack stack;
    private final int playerId;

    /**
     * Creates a new hold packet.
     *
     * @param stack    the held gun ItemStack
     * @param playerId the holder's entity id
     */
    public GunHoldPacket(ItemStack stack, int playerId) {
        this.stack = stack;
        this.playerId = playerId;
    }

    /**
     * Reads a packet from the network buffer.
     *
     * @param buffer the source buffer
     */
    public GunHoldPacket(FriendlyByteBuf buffer) {
        this.stack = buffer.readItem();
        this.playerId = buffer.readInt();
    }

    /**
     * Writes this packet to the network buffer.
     *
     * @param msg    the packet to write
     * @param buffer the destination buffer
     */
    public static void encode(GunHoldPacket msg, FriendlyByteBuf buffer) {
        buffer.writeItemStack(msg.stack, false);
        buffer.writeInt(msg.playerId);
    }

    /**
     * Reads a packet from the network buffer.
     *
     * @param buffer the source buffer
     * @return the decoded packet
     */
    public static GunHoldPacket decode(FriendlyByteBuf buffer) {
        return new GunHoldPacket(buffer.readItem(), buffer.readInt());
    }

    /**
     * Handles the packet on the client.
     *
     * @param msg            the received packet
     * @param contextSupplier the network context supplier
     */
    public static void handle(GunHoldPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {

        contextSupplier.get().enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;

            if (level != null) {

                Entity entity = level.getEntity(msg.playerId);
                if (entity instanceof Player player) {

                    boolean isHoldingGun = EGun.from(msg.stack) != null;
                    AnimationManager.IS_CLIENT_HOLDING_GUN = isHoldingGun;

                    GunTracker.setGunHolder(msg.stack, player);
                    if (isHoldingGun) AnimationManager.resetValues();

                    entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                        EGun.Sounds.HOLD, SoundSource.MASTER, 1.1f, 1.0f, false);
                }
            }
        });
        contextSupplier.get().setPacketHandled(true);
    }
}