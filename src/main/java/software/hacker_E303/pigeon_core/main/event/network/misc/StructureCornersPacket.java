package software.hacker_E303.pigeon_core.main.event.network.misc;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import software.hacker_E303.pigeon_core.init.item.StructureTool;

/**
 * Client-to-server packet reporting the structure tool's currently selected corners,
 * which corner is selected next, and its persistent tool UUID.
 *
 * <p>Corner selection (and UUID generation) only ever happens client-side (a
 * client-only raycast, to stay responsive), so without this packet the server's copy
 * of the held item never learns the selected area or UUID — any server-authoritative
 * action (block scan, remove, replace, save) would silently see no selection at all.
 *
 * <p>Every field that this item's NBT can hold and that only the client ever writes
 * must be included here: vanilla periodically re-broadcasts a player's own held item
 * to keep client and server in sync, and if the server's copy differs from the
 * client's for ANY reason (a field we forgot to sync), that broadcast can silently
 * overwrite the client's copy with a version missing that field — e.g. omitting
 * "which corner is selected next" made every click re-select the first corner,
 * since the server's copy always defaulted it back to true.
 */
public class StructureCornersPacket {

    private final boolean hasCorner1;
    private final long corner1;
    private final boolean hasCorner2;
    private final long corner2;
    private final boolean selectingFirst;
    private final boolean hasUuid;
    private final UUID uuid;

    public StructureCornersPacket(boolean hasCorner1, long corner1, boolean hasCorner2, long corner2,
                                   boolean selectingFirst, boolean hasUuid, UUID uuid) {
        this.hasCorner1 = hasCorner1;
        this.corner1 = corner1;
        this.hasCorner2 = hasCorner2;
        this.corner2 = corner2;
        this.selectingFirst = selectingFirst;
        this.hasUuid = hasUuid;
        this.uuid = uuid;
    }

    public static void encode(StructureCornersPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.hasCorner1);
        buf.writeLong(msg.corner1);
        buf.writeBoolean(msg.hasCorner2);
        buf.writeLong(msg.corner2);
        buf.writeBoolean(msg.selectingFirst);
        buf.writeBoolean(msg.hasUuid);
        if (msg.hasUuid) buf.writeUUID(msg.uuid);
    }

    public static StructureCornersPacket decode(FriendlyByteBuf buf) {
        boolean hasC1 = buf.readBoolean();
        long c1 = buf.readLong();
        boolean hasC2 = buf.readBoolean();
        long c2 = buf.readLong();
        boolean selectingFirst = buf.readBoolean();
        boolean hasUuid = buf.readBoolean();
        UUID uuid = hasUuid ? buf.readUUID() : null;
        return new StructureCornersPacket(hasC1, c1, hasC2, c2, selectingFirst, hasUuid, uuid);
    }

    public static void handle(StructureCornersPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof StructureTool)) return;

            BlockPos c1 = msg.hasCorner1 ? BlockPos.of(msg.corner1) : null;
            BlockPos c2 = msg.hasCorner2 ? BlockPos.of(msg.corner2) : null;
            StructureTool.applySyncedCorners(stack, c1, c2, msg.selectingFirst);
            if (msg.hasUuid) StructureTool.applySyncedUUID(stack, msg.uuid);
        });
        ctx.get().setPacketHandled(true);
    }
}
