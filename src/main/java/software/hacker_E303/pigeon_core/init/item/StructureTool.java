package software.hacker_E303.pigeon_core.init.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.hacker_E303.pigeon_core.common.PigeGui;
import software.hacker_E303.pigeon_core.init.gui.StructureToolGui;
import software.hacker_E303.pigeon_core.item.EItem;
import software.hacker_E303.pigeon_core.main.AutoRegister;
import software.hacker_E303.pigeon_core.main.event.network.PigeNetworking;
import software.hacker_E303.pigeon_core.main.event.network.misc.StructureCornersPacket;
import software.hacker_E303.pigeon_core.main.event.render.PerimeterRenderer;
import software.hacker_E303.pigeon_core.util.BetterData;
import software.hacker_E303.pigeon_core.util.world.PlayerUtils;

import javax.annotation.Nullable;
import java.util.UUID;

@AutoRegister("structure_tool")
public final class StructureTool extends EItem {

    private static final String CORNER1 = "first_corner";
    private static final String CORNER2 = "second_corner";
    private static final String CORNER1_SELECTED = "first_corner_selected";
    private static final String TOOL_UUID = "tool_uuid";

    private static final double REACH_DISTANCE = 24.0;

    /**
     * Returns a persistent UUID for this StructureTool ItemStack.
     * The UUID is generated once and stays the same across NBT syncs.
     * Only call this when you know the stack is a StructureTool.
     */
    public static UUID getOrCreateUUID(ItemStack stack) {
        if (BetterData.hasData(stack, TOOL_UUID)) {
            return UUID.fromString(BetterData.getData(stack, TOOL_UUID, ""));
        }
        UUID uuid = UUID.randomUUID();
        BetterData.setData(stack, TOOL_UUID, uuid.toString());
        return uuid;
    }

    /**
     * Returns the UUID if it already exists, null otherwise.
     * Does NOT create a new UUID.
     */
    public static UUID getExistingUUID(ItemStack stack) {
        if (!BetterData.hasData(stack, TOOL_UUID)) return null;
        return UUID.fromString(BetterData.getData(stack, TOOL_UUID, ""));
    }

    public StructureTool() {
        super(1);
    }

    @Override
    public Interaction useEvent(ItemStack stack, Level level, Player player) {
        if (!level.isClientSide() && player.isShiftKeyDown()) 
            PigeGui.get(StructureToolGui.class).open(player);

        if (!PlayerUtils.with("pigeon_core", player, ctx ->
            ctx.hasSolidBlockInSight(REACH_DISTANCE), false)) return Interaction.FAIL;

        if (level.isClientSide() && !player.isShiftKeyDown()) 
            handleClientInput(stack, level, player);
        return Interaction.SUCCESS;
    }

    private void handleClientInput(ItemStack stack, Level level, Player player) {
        if (player.getCooldowns().isOnCooldown(this)) return;
        
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        double reach = REACH_DISTANCE; 
        
        Vec3 endPos = eyePos.add(lookVec.x * reach, lookVec.y * reach, lookVec.z * reach);
        BlockHitResult hitResult = level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hitResult.getType() == HitResult.Type.BLOCK) selectCorner(stack, hitResult.getBlockPos(), player);
    }

    private void selectCorner(ItemStack stack, BlockPos blockPos, Player player) {
        boolean selectingFirst = BetterData.getData(stack, CORNER1_SELECTED, true);

        PlayerUtils.with("pigeon_core", player, ctx -> {
            if (selectingFirst) {

                BetterData.setData(stack, CORNER1, blockPos.asLong());
                BetterData.setData(stack, CORNER1_SELECTED, false);

                ctx.debugMessage("structure_tool.first_corner", blockPos.getX(), blockPos.getY(), blockPos.getZ());
                ctx.playNotifySound("notify_1");
            } else {

                BetterData.setData(stack, CORNER2, blockPos.asLong());
                BetterData.setData(stack, CORNER1_SELECTED, true);

                ctx.debugMessage("structure_tool.second_corner", blockPos.getX(), blockPos.getY(), blockPos.getZ()).literalMessage("");
                ctx.playNotifySound("notify_2");

                // Show the perimeter box
                BlockPos first = getFirstCorner(stack);
                BlockPos second = getSecondCorner(stack);
                if (first != null && second != null) {
                    PerimeterRenderer.addPerimeter(getOrCreateUUID(stack), first, second);
                }
            }
        });

        syncCornersToServer(stack);
    }

    public static BlockPos getFirstCorner(ItemStack stack) {
        return BetterData.hasData(stack, CORNER1) ? BlockPos.of(BetterData.getData(stack, CORNER1, -1L)) : null;
    }

    public static BlockPos getSecondCorner(ItemStack stack) {
        return BetterData.hasData(stack, CORNER2) ? BlockPos.of(BetterData.getData(stack, CORNER2, -1L)) : null;
    }

    public static void resetSelection(ItemStack stack) {
        BetterData.removeData(stack, CORNER1);
        BetterData.removeData(stack, CORNER2);
        BetterData.setData(stack, CORNER1_SELECTED, true);
        UUID uuid = getExistingUUID(stack);
        if (uuid != null) PerimeterRenderer.removePerimeter(uuid);
        syncCornersToServer(stack);
    }

    /**
     * Sends the currently selected corners to the server, so its own copy of the held
     * item stays in sync (corner selection itself only ever runs client-side).
     */
    private static void syncCornersToServer(ItemStack stack) {
        BlockPos first = getFirstCorner(stack);
        BlockPos second = getSecondCorner(stack);
        boolean selectingFirst = BetterData.getData(stack, CORNER1_SELECTED, true);
        UUID uuid = getExistingUUID(stack);
        PigeNetworking.sendToServer(new StructureCornersPacket(
                first != null, first != null ? first.asLong() : 0L,
                second != null, second != null ? second.asLong() : 0L,
                selectingFirst,
                uuid != null, uuid));
    }

    /**
     * Server-side: applies corner data received from {@link StructureCornersPacket}.
     */
    public static void applySyncedCorners(ItemStack stack, @Nullable BlockPos corner1, @Nullable BlockPos corner2,
                                           boolean selectingFirst) {
        if (corner1 != null) BetterData.setData(stack, CORNER1, corner1.asLong());
        else BetterData.removeData(stack, CORNER1);

        if (corner2 != null) BetterData.setData(stack, CORNER2, corner2.asLong());
        else BetterData.removeData(stack, CORNER2);

        BetterData.setData(stack, CORNER1_SELECTED, selectingFirst);
    }

    /**
     * Server-side: applies the tool UUID received from {@link StructureCornersPacket}.
     * The server must never generate its own UUID for this item — only ever adopt the
     * client's, otherwise a cache keyed by this UUID (e.g. in the GUI) would silently
     * diverge between client and server.
     */
    public static void applySyncedUUID(ItemStack stack, UUID uuid) {
        BetterData.setData(stack, TOOL_UUID, uuid.toString());
    }
}