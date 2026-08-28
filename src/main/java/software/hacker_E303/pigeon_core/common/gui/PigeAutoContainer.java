package software.hacker_E303.pigeon_core.common.gui;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.hacker_E303.pigeon_core.common.PigeGui;

/**
 * Auto-generated container for every {@link PigeGui} subclass.
 * Created by the framework — do not extend or instantiate manually.
 */
public final class PigeAutoContainer extends AbstractContainerMenu {

    private final PigeGui gui;
    private final byte    sourceType; // SRC_* constants from PigeGui

    // Entity source
    @Nullable private final UUID senderEntityUUID;
    private final int            senderEntityNetId;

    // Item source
    @Nullable private final ItemStack       senderItem;
    @Nullable private final InteractionHand senderHand;

    private final double x, y, z;
    private int     customSlotCount         = 0;
    private boolean hasPlayerInventorySlots = false;

    // Persistent backing for hasSenderInventory()
    @Nullable private SimpleContainer entityBacking    = null;
    @Nullable private Entity          entityBacker     = null; // held for write-back in removed()
    @Nullable private String          entityBackingKey = null;
    @Nullable private SimpleContainer itemBacking      = null;

    /**
     * Server-side constructor for entity/block/player sources.
     *
     * @param menuType          the menu type
     * @param containerId       the container id
     * @param playerInventory   the player's inventory
     * @param gui               the GUI definition
     * @param sourceType        the source type byte (SRC_*)
     * @param senderEntityNetId the sender entity network id
     * @param senderEntityUUID  the sender entity UUID, or null
     * @param x                 world x
     * @param y                 world y
     * @param z                 world z
     */
    /** Entity / block / player-only source. */
    public PigeAutoContainer(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory,
                             PigeGui gui, byte sourceType,
                             int senderEntityNetId, @Nullable UUID senderEntityUUID,
                             double x, double y, double z) {
        super(menuType, containerId);
        this.gui               = gui;
        this.sourceType        = sourceType;
        this.senderEntityNetId = senderEntityNetId;
        this.senderEntityUUID  = senderEntityUUID;
        this.senderItem        = null;
        this.senderHand        = null;
        this.x = x; this.y = y; this.z = z;
        setupSlots(playerInventory);
    }

    /**
     * Server-side constructor for item sources.
     *
     * @param menuType        the menu type
     * @param containerId     the container id
     * @param playerInventory the player's inventory
     * @param gui             the GUI definition
     * @param hand            the interaction hand
     * @param item            the source item stack
     * @param x               world x
     * @param y               world y
     * @param z               world z
     */
    /** Item source. */
    public PigeAutoContainer(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory,
                             PigeGui gui, InteractionHand hand, ItemStack item,
                             double x, double y, double z) {
        super(menuType, containerId);
        this.gui               = gui;
        this.sourceType        = PigeGui.SRC_ITEM;
        this.senderEntityNetId = -1;
        this.senderEntityUUID  = null;
        this.senderItem        = item;
        this.senderHand        = hand;
        this.x = x; this.y = y; this.z = z;
        setupSlots(playerInventory);
    }

    /**
     * Client-side constructor that reads the GUI state from the network buffer.
     *
     * @param menuType        the menu type
     * @param containerId     the container id
     * @param playerInventory the player's inventory
     * @param buf             the network buffer
     */
    public PigeAutoContainer(@Nullable MenuType<?> menuType, int containerId,
                             Inventory playerInventory, FriendlyByteBuf buf) {
        super(menuType, containerId);
        String modId   = buf.readUtf();
        String guiId   = buf.readUtf();
        byte   srcType = buf.readByte();
        this.sourceType = srcType;

        if (srcType == PigeGui.SRC_ENTITY) {
            boolean hasUUID        = buf.readBoolean();
            this.senderEntityUUID  = hasUUID ? buf.readUUID() : null;
            this.senderEntityNetId = buf.readInt();
            this.x = buf.readDouble(); this.y = buf.readDouble(); this.z = buf.readDouble();
            this.senderItem = null;
            this.senderHand = null;
        } else if (srcType == PigeGui.SRC_ITEM) {
            this.senderHand        = InteractionHand.values()[buf.readInt()];
            this.senderItem        = buf.readItem();
            this.senderEntityUUID  = null;
            this.senderEntityNetId = -1;
            this.x = 0; this.y = 0; this.z = 0;
        } else if (srcType == PigeGui.SRC_BLOCK) {
            this.x = buf.readDouble(); this.y = buf.readDouble(); this.z = buf.readDouble();
            this.senderEntityUUID  = null;
            this.senderEntityNetId = -1;
            this.senderItem        = null;
            this.senderHand        = null;
        } else { // SRC_PLAYER
            this.senderEntityUUID  = null;
            this.senderEntityNetId = -1;
            this.senderItem        = null;
            this.senderHand        = null;
            this.x = 0; this.y = 0; this.z = 0;
        }

        this.gui = PigeGui.getById(modId, guiId);
        setupSlots(playerInventory);
    }

    // ── Slot setup ────────────────────────────────────────────────────────────

    /**
     * Builds custom slots and (optionally) the player inventory from the GUI definition.
     *
     * @param playerInventory the player's inventory
     */
    private void setupSlots(Inventory playerInventory) {
        if (gui == null) return;

        Player player = playerInventory.player;

        // Build element list (slots, buttons, etc.)
        GuiContext ctx = new GuiContext();
        callRenderInterface(ctx, player);

        // ── Custom slots ──────────────────────────────────────────────────────
        if (!ctx.data().slots().isEmpty()) {
            customSlotCount = ctx.data().slots().size();

            Container backing = buildBacking(player);

            for (GuiContext.SlotElement slotDef : ctx.data().slots()) {
                LayoutBounds b = slotDef.bounds();
                int sx = b != null ? b.getX() : (8 + slotDef.index() * 18);
                int sy = b != null ? b.getY() : 30;
                addSlot(new PigeSlot(backing, slotDef.index(), sx, sy,
                        slotDef.canInsert(), slotDef.canExtract(), slotDef.visible()));
            }
        }

        // ── Player inventory (always check, regardless of custom slot count) ──
        if (gui.hasReceiverInventory()) {

            LayoutBounds defaultBounds = LayoutBounds.create(-1, -1, 200, hasPlayerInventorySlots() ? 180 : 160);
            PigeGui.Background background = gui.getBackground(defaultBounds);
            
            LayoutBounds finalBounds = background != null ? background.getBounds() : defaultBounds;
            int guiWidth = finalBounds.getWidth();
            int guiHeight = finalBounds.getHeight();

            int defaultX = (guiWidth - 162) / 2 + 1;
            int defaultY = guiHeight - 53 - 8;

            LayoutBounds invPosition = gui.getInventoryPosition(LayoutBounds.create(defaultX, defaultY));
            
            int startX = (invPosition.getX() != -1) ? invPosition.getX() : defaultX;
            int startY = (invPosition.getY() != -1) ? invPosition.getY() : defaultY;

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + (col * 18), startY + (row * 18)));
                }
            }
            int hotbarY = startY + (3 * 18) + 4;
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col, startX + (col * 18), hotbarY));
            }
            hasPlayerInventorySlots = true;
        }
    }

    /**
     * Creates the slot backing container for custom GUI slots.
     *
     * @param player the viewing player
     * @return the backing container
     */
    private Container buildBacking(Player player) {
        if (!gui.hasSenderInventory()) return new SimpleContainer(customSlotCount);

        // Entity backing
        Entity sender = getSenderEntity(player.level());
        if (sender != null) {
            if (sender instanceof Container entityContainer) {
                // Entity already manages its own container
                return entityContainer;
            }
            // Use entity's persistent data as backing — survives restarts
            SimpleContainer eb   = new SimpleContainer(customSlotCount);
            String          key  = "PigeGui_" + gui.getId();
            CompoundTag     data = sender.getPersistentData();
            if (data.contains(key)) {
                NonNullList<ItemStack> items = NonNullList.withSize(customSlotCount, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(data.getCompound(key), items);
                for (int i = 0; i < customSlotCount; i++) eb.setItem(i, items.get(i));
            }
            entityBacking    = eb;
            entityBacker     = sender;
            entityBackingKey = key;
            return eb;
        }

        // Item backing
        if (senderItem != null && !senderItem.isEmpty()) {
            SimpleContainer ib    = new SimpleContainer(customSlotCount);
            NonNullList<ItemStack> items = NonNullList.withSize(customSlotCount, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(senderItem.getOrCreateTag(), items);
            for (int i = 0; i < customSlotCount; i++) ib.setItem(i, items.get(i));
            itemBacking = ib;
            return ib;
        }

        return new SimpleContainer(customSlotCount);
    }

    // ── Render dispatch ───────────────────────────────────────────────────────

    /**
     * Calls the correct {@code renderInterface} overload based on the source type
     * used when the GUI was opened. Falls back to the player-only overload if the
     * source entity/item is unavailable (e.g. entity unloaded client-side).
     */
    public void callRenderInterface(GuiContext ctx, Player player) {
        if (gui == null) return;
        switch (sourceType) {
            case PigeGui.SRC_ENTITY -> {
                Entity e = getSenderEntity(player.level());
                if (e != null) { gui.renderInterface(ctx, player, e); return; }
            }
            case PigeGui.SRC_ITEM -> {
                if (senderItem != null && !senderItem.isEmpty()) {
                    gui.renderInterface(ctx, player, senderItem);
                    return;
                }
            }
            case PigeGui.SRC_BLOCK -> {
                gui.renderInterface(ctx, player, x, y, z);
                return;
            }
        }
        gui.renderInterface(ctx, player);
    }

    // ── Slot subclass ─────────────────────────────────────────────────────────

    private static final class PigeSlot extends Slot {
        private final Predicate<ItemStack> canInsert;
        private final Predicate<ItemStack> canExtract;
        private final BooleanSupplier      visible;

        PigeSlot(Container container, int index, int x, int y,
                 Predicate<ItemStack> canInsert, Predicate<ItemStack> canExtract,
                 BooleanSupplier visible) {
            super(container, index, x, y);
            this.canInsert  = canInsert;
            this.canExtract = canExtract;
            this.visible    = visible;
        }

        @Override public boolean mayPlace(ItemStack stack) { return canInsert.test(stack); }
        @Override public boolean mayPickup(Player player)  { return canExtract.test(getItem()); }
        @Override public boolean isActive()                { return visible.getAsBoolean(); }
    }

    // ── AbstractContainerMenu ─────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean stillValid(Player player) { return true; }

    /**
     * Writes entity/item slot data back to persistent storage when the GUI closes.
     *
     * @param player the player who closed the GUI
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        // Only write-back on the server — client has no authority over persistent data
        if (!(player.level() instanceof ServerLevel)) return;

        // Entity persistent data write-back
        if (entityBacking != null && entityBacker != null && entityBackingKey != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(customSlotCount, ItemStack.EMPTY);
            for (int i = 0; i < customSlotCount; i++) items.set(i, entityBacking.getItem(i));
            CompoundTag tag = new CompoundTag();
            tag.putInt("Size", customSlotCount);
            ContainerHelper.saveAllItems(tag, items);
            entityBacker.getPersistentData().put(entityBackingKey, tag);
        }

        // Item NBT write-back
        if (itemBacking != null && senderHand != null) {
            ItemStack heldItem = player.getItemInHand(senderHand);
            if (!heldItem.isEmpty()) {
                NonNullList<ItemStack> items = NonNullList.withSize(customSlotCount, ItemStack.EMPTY);
                for (int i = 0; i < customSlotCount; i++) items.set(i, itemBacking.getItem(i));
                ContainerHelper.saveAllItems(heldItem.getOrCreateTag(), items);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (customSlotCount == 0 || !hasPlayerInventorySlots) return ItemStack.EMPTY;

        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        ItemStack result    = slotStack.copy();

        int playerInvStart = customSlotCount;
        int playerInvEnd   = this.slots.size();

        if (slotIndex < customSlotCount) {
            if (!this.moveItemStackTo(slotStack, playerInvStart, playerInvEnd, true))
                return ItemStack.EMPTY;
        } else {
            if (!this.moveItemStackTo(slotStack, 0, customSlotCount, false))
                return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        return result;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public PigeGui getGui()            { return gui;             }
    public int     getCustomSlotCount() { return customSlotCount; }
    public byte    getSourceType()     { return sourceType;      }

    /**
     * Returns the entity that opened this GUI, if any.
     *
     * @param level the current level
     * @return the sender entity, or null
     */
    @Nullable
    public Entity getSenderEntity(Level level) {
        if (senderEntityUUID != null && level instanceof ServerLevel serverLevel)
            return serverLevel.getEntity(senderEntityUUID);
        if (senderEntityNetId >= 0)
            return level.getEntity(senderEntityNetId);
        return null;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public boolean hasPlayerInventorySlots() { return hasPlayerInventorySlots; }
}
