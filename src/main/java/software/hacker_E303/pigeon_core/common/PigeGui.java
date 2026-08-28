package software.hacker_E303.pigeon_core.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;
import software.hacker_E303.pigeon_core.common.gui.GuiContext;
import software.hacker_E303.pigeon_core.common.gui.PigeAutoContainer;
import software.hacker_E303.pigeon_core.common.gui.LayoutBounds;
import software.hacker_E303.pigeon_core.PigeonCore;

/**
 * Abstract base class for auto-registered GUIs.
 * Annotate subclasses with {@link software.hacker_E303.pigeon_core.main.AutoRegister}
 * and override exactly ONE {@code renderInterface} variant:
 *
 * <ul>
 *   <li>{@link #renderInterface(GuiContext, Player)} — player-only GUI</li>
 *   <li>{@link #renderInterface(GuiContext, Player, Entity)} — entity GUI</li>
 *   <li>{@link #renderInterface(GuiContext, Player, double, double, double)} — block GUI</li>
 *   <li>{@link #renderInterface(GuiContext, Player, ItemStack)} — item GUI</li>
 * </ul>
 *
 * Open the GUI via:
 * <pre>{@code
 * PigeGui.get(MyGui.class).open(player, this); // from an entity
 * }</pre>
 */
public abstract class PigeGui implements MenuProvider {

    // Source-type constants (written into the network buffer)
    public static final byte SRC_PLAYER = 0;
    public static final byte SRC_ENTITY = 1;
    public static final byte SRC_ITEM   = 2;
    public static final byte SRC_BLOCK  = 3;

    // ── Static registry ──────────────────────────────────────────────────────

    private static final Map<Class<? extends PigeGui>, PigeGui> BY_CLASS = new HashMap<>();
    private static final Map<String, PigeGui>                   BY_ID    = new HashMap<>();

    public static void registerInstance(PigeGui instance) {
        BY_CLASS.put(instance.getClass(), instance);
        if (instance.modid != null && instance.id != null)
            BY_ID.put(instance.modid + ":" + instance.id, instance);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends PigeGui> T get(Class<T> clazz) {
        return (T) BY_CLASS.get(clazz);
    }

    @Nullable
    public static PigeGui getById(String modId, String guiId) {
        return BY_ID.get(modId + ":" + guiId);
    }

    // ── Instance fields ───────────────────────────────────────────────────────

    private String id;
    private String modid;
    @Nullable private MenuType<?> menuType;

    @OnlyIn(Dist.CLIENT)
    public int mouseX = 0;
    
    @OnlyIn(Dist.CLIENT)
    public int mouseY = 0;

    // ── Framework setters ─────────────────────────────────────────────────────

    public void setId(String id)              { this.id = id; }
    public String getId()                     { return id; }
    public void setModid(String modid)        { this.modid = modid; }
    public String modid()                  { return modid; }
    public void setMenuType(MenuType<?> type) { this.menuType = type; }
    @Nullable public MenuType<?> getMenuType(){ return menuType; }

    // ── Render interface — override exactly one ───────────────────────────────

    /** GUI opened directly on the player (no entity / block / item source). */
    public void renderInterface(GuiContext ctx, Player player) {}

    /**
     * GUI opened from an entity. {@code sender} is always non-null here.
     * The framework dispatches to this when the GUI was opened via
     * {@link #open(Player, Entity)}.
     */
    public void renderInterface(GuiContext ctx, Player receiver, Entity sender) {}

    /**
     * GUI opened from a block position.
     * Dispatched when opened via {@link #open(Player, double, double, double)}.
     */
    public void renderInterface(GuiContext ctx, Player receiver,
                                double x, double y, double z) {}

    /**
     * GUI opened from an item.
     * Dispatched when opened via {@link #open(Player, ItemStack)}.
     * If {@link #hasSenderInventory()} is true, slot contents are automatically
     * loaded from / saved back to the item's NBT.
     */
    public void renderInterface(GuiContext ctx, Player receiver, ItemStack sender) {}

    // ── Behaviour flags ───────────────────────────────────────────────────────

    /** Whether to draw the background texture/panel. Default {@code true}. */
    @Nonnull
    public Background getBackground(LayoutBounds bounds) { return Background.create(bounds, true); }

    @Nonnull
    public LayoutBounds getInventoryPosition(LayoutBounds bounds) { return bounds; }

    /**
     * Whether the player's inventory is shown at the bottom of this GUI.
     * Default {@code false}. Set to {@code false} for GUIs without an inventory section.
     */
    public boolean hasReceiverInventory() { return false; }

    @Nullable
    public SoundEvent getButtonSound() { return PigeonCore.getSound("pigeon_core", "button"); }

    /**
     * Whether the custom slots are backed by the sender's persistent storage.
     * <ul>
     *   <li>Entity: items are stored in {@code entity.getPersistentData()} under a
     *       per-GUI key and survive server restarts.</li>
     *   <li>Item: items are stored in / loaded from the item's NBT tag.</li>
     * </ul>
     * Default {@code false} — slots are ephemeral (lost on GUI close).
     */
    public boolean hasSenderInventory() { return false; }

    /**
     * Whether opening this GUI pauses single-player game time.
     * Default {@code false}.
     */
    public boolean shouldPauseGame() { return false; }

    /**
     * Called when the player presses a key while this GUI is open.
     * <p>
     * Return {@code true} (default) to let the normal key handling proceed.
     * Return {@code false} to consume the event and cancel any default action
     * (e.g. prevent Escape from closing the GUI, or intercept hotbar keys).
     *
     * @param keyCode  GLFW key code
     * @param scanCode hardware scan code
     * @param modifiers GLFW modifier flags (Shift / Ctrl / Alt)
     * @return {@code true} to allow default handling, {@code false} to cancel it
     */
    public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) { return true; }

    /**
     * Called CLIENT-SIDE when the player types a printable character while this GUI is open.
     * Return {@code false} to consume the event (prevents default screen handling) and forward
     * the character to the server via {@link #handleCharInput}.
     * Return {@code true} (default) to let normal handling proceed.
     */
    @OnlyIn(Dist.CLIENT)
    public boolean onCharTyped(char c) { return true; }

    /**
     * Called SERVER-SIDE when a char-input event forwarded by {@link #onCharTyped} or
     * a key-press event forwarded by {@link #onKeyPressed} arrives from the client.
     *
     * @param c       the typed character, or {@code (char) 0} for a key-press event
     * @param keyCode GLFW key code when {@code c == 0}, otherwise {@code -1}
     * @param sender  the entity the GUI was opened on (may be {@code null} for player/item GUIs)
     * @param player  the player who triggered the event
     */
    public void handleCharInput(char c, int keyCode,
                                @Nullable Entity sender, Player player) {}

    // ── MenuProvider (internal) ───────────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        String key = "gui" + (modid != null ? "." + modid : "") + "." + (id != null ? id : "unknown");
        return Component.translatable(key);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PigeAutoContainer(menuType, containerId, playerInventory,
                this, SRC_PLAYER, -1, null, 0, 0, 0);
    }

    // ── Public open() API ─────────────────────────────────────────────────────

    /** Opens this GUI for {@code player} with no entity/block context. */
    public void open(Player player) {
        openInternal(player, null, SRC_PLAYER, 0, 0, 0);
    }

    /** Opens this GUI for {@code player} from an entity. */
    public void open(Player player, Entity sender) {
        openInternal(player, sender, SRC_ENTITY, sender.getX(), sender.getY(), sender.getZ());
    }

    /** Opens this GUI for {@code player} from a block at {@code (x, y, z)}. */
    public void open(Player player, double x, double y, double z) {
        openInternal(player, null, SRC_BLOCK, x, y, z);
    }

    /** Opens this GUI for {@code player} from an item held in the main hand. */
    public void open(Player player, ItemStack item) {
        openInternalItem(player, item, InteractionHand.MAIN_HAND);
    }

    /** Opens this GUI for {@code player} from an item held in the specified hand. */
    public void open(Player player, ItemStack item, InteractionHand hand) {
        openInternalItem(player, item, hand);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void openInternal(Player player, @Nullable Entity sender, byte sourceType,
                              double x, double y, double z) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        int    entityNetId = sender != null ? sender.getId()   : -1;
        UUID   entityUUID  = sender != null ? sender.getUUID() : null;
        String guiId       = this.id;
        String modId       = this.modid;

        MenuProvider provider = new MenuProvider() {
            @Override public Component getDisplayName() { return PigeGui.this.getDisplayName(); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new PigeAutoContainer(menuType, id, inv,
                        PigeGui.this, sourceType, entityNetId, entityUUID, x, y, z);
            }
        };

        NetworkHooks.openScreen(serverPlayer, provider, buf -> {
            buf.writeUtf(modId != null ? modId : "");
            buf.writeUtf(guiId != null ? guiId : "");
            buf.writeByte(sourceType);
            if (sourceType == SRC_ENTITY) {
                buf.writeBoolean(entityUUID != null);
                if (entityUUID != null) buf.writeUUID(entityUUID);
                buf.writeInt(entityNetId);
                buf.writeDouble(x); buf.writeDouble(y); buf.writeDouble(z);
            } else if (sourceType == SRC_BLOCK) {
                buf.writeDouble(x); buf.writeDouble(y); buf.writeDouble(z);
            }
        });
    }

    private void openInternalItem(Player player, ItemStack item, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        String    guiId        = this.id;
        String    modId        = this.modid;
        ItemStack itemSnapshot = item.copy();

        MenuProvider provider = new MenuProvider() {
            @Override public Component getDisplayName() { return PigeGui.this.getDisplayName(); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new PigeAutoContainer(menuType, id, inv,
                        PigeGui.this, hand, itemSnapshot, p.getX(), p.getY(), p.getZ());
            }
        };

        NetworkHooks.openScreen(serverPlayer, provider, buf -> {
            buf.writeUtf(modId != null ? modId : "");
            buf.writeUtf(guiId != null ? guiId : "");
            buf.writeByte(SRC_ITEM);
            buf.writeInt(hand.ordinal());
            buf.writeItem(itemSnapshot);
        });
    }

    // ── External slot access ──────────────────────────────────────────────────

    /**
     * Returns an {@link InteractionContext} targeting the open GUI for {@code player}.
     * Works only while the GUI is open for that player; slot access on closed GUIs returns
     * {@link ItemStack#EMPTY} / does nothing (player-source GUIs have no persistent backing).
     *
     * <pre>{@code
     * PigeGui.get(MyGui.class).interact(player).getItem(0);
     * }</pre>
     */
    public InteractionContext interact(Player player) {
        return new InteractionContext(this, player, null, null, null);
    }

    /**
     * Returns an {@link InteractionContext} targeting the slots backed by {@code entity}.
     * If the GUI is currently open with this entity as source the live container is used;
     * otherwise the entity's persistent NBT ({@code PigeGui_<id>}) is read/written directly,
     * so this works even when the GUI is closed.
     */
    public InteractionContext interact(Entity entity) {
        return new InteractionContext(this, null, entity, null, null);
    }

    /**
     * Returns an {@link InteractionContext} targeting the slots stored in {@code item}'s NBT.
     * If the GUI is currently open with this item as source the live container is used;
     * otherwise the item's tag is read/written directly.
     */
    public InteractionContext interact(ItemStack item) {
        return new InteractionContext(this, null, null, item, null);
    }

    /**
     * Returns an {@link InteractionContext} targeting the open GUI whose block source
     * matches {@code pos}. Works only while a player has the GUI open at that position.
     * Block-source GUIs have no persistent backing (slots are ephemeral).
     */
    public InteractionContext interact(net.minecraft.core.BlockPos pos) {
        return new InteractionContext(this, null, null, null, pos);
    }

    /**
     * Bound context for external slot access.
     * Obtain via one of the {@link PigeGui#interact} overloads.
     */
    public static final class InteractionContext {

        private final PigeGui gui;
        @Nullable private final Player                    player;
        @Nullable private final Entity                    entity;
        @Nullable private final ItemStack                 item;
        @Nullable private final net.minecraft.core.BlockPos blockPos;

        InteractionContext(PigeGui gui,
                           @Nullable Player player, @Nullable Entity entity,
                           @Nullable ItemStack item,
                           @Nullable net.minecraft.core.BlockPos blockPos) {
            this.gui      = gui;
            this.player   = player;
            this.entity   = entity;
            this.item     = item;
            this.blockPos = blockPos;
        }

        // ── Live container lookup ─────────────────────────────────────────────

        @Nullable
        private PigeAutoContainer findOpenContainer() {
            if (player != null) {
                if (player.containerMenu instanceof PigeAutoContainer pac && pac.getGui() == gui)
                    return pac;
                return null;
            }
            if (entity != null && entity.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                for (net.minecraft.server.level.ServerPlayer sp : sl.players()) {
                    if (!(sp.containerMenu instanceof PigeAutoContainer pac) || pac.getGui() != gui) continue;
                    Entity src = pac.getSenderEntity(sl);
                    if (src != null && src.getUUID().equals(entity.getUUID())) return pac;
                }
                return null;
            }
            if (blockPos != null) {
                var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    for (net.minecraft.server.level.ServerPlayer sp : server.getPlayerList().getPlayers()) {
                        if (!(sp.containerMenu instanceof PigeAutoContainer pac) || pac.getGui() != gui) continue;
                        if (pac.getSourceType() == PigeGui.SRC_BLOCK
                                && net.minecraft.core.BlockPos.containing(pac.getX(), pac.getY(), pac.getZ()).equals(blockPos))
                            return pac;
                    }
                }
            }
            return null;
        }

        // ── Public API ────────────────────────────────────────────────────────

        /**
         * Places {@code stack} into slot {@code slot}.
         * Pass {@link ItemStack#EMPTY} or {@code null} to clear the slot.
         */
        public void setItem(ItemStack stack, int slot) {
            if (stack == null) stack = ItemStack.EMPTY;
            PigeAutoContainer pac = findOpenContainer();
            if (pac != null) {
                if (slot >= 0 && slot < pac.getCustomSlotCount())
                    pac.slots.get(slot).set(stack);
                return;
            }
            if (entity != null) entityNbtSet(stack, slot);
            else if (item != null) itemNbtSet(stack, slot);
        }

        /**
         * Adds {@code stack} into slot {@code slot}.
         */
        public void addItem(ItemStack stack, int slot) {
            if (stack == null || stack.isEmpty()) return;
            
            PigeAutoContainer pac = findOpenContainer();
            if (pac != null) {
                if (slot >= 0 && slot < pac.getCustomSlotCount()) {
                    Slot targetSlot = pac.slots.get(slot);
                    ItemStack currentStack = targetSlot.getItem();
                    
                    if (currentStack.isEmpty()) {
                        targetSlot.set(stack);

                    } else if (ItemStack.isSameItem(currentStack, stack)) {
                        int maxStackSize = Math.min(targetSlot.getMaxStackSize(stack), stack.getMaxStackSize());
                        int countToMove = Math.min(stack.getCount(), maxStackSize - currentStack.getCount());
                        
                        if (countToMove > 0) {
                            currentStack.grow(countToMove);
                            stack.shrink(countToMove);
                            targetSlot.setChanged();
                        }
                    }
                }
                return;
            }
            if (entity != null) entityNbtSet(stack, slot);
            else if (item != null) itemNbtSet(stack, slot);
        }

        /** Returns the item at slot {@code slot}, or {@link ItemStack#EMPTY} if unavailable. */
        public ItemStack getItem(int slot) {
            PigeAutoContainer pac = findOpenContainer();
            if (pac != null) {
                if (slot >= 0 && slot < pac.getCustomSlotCount())
                    return pac.slots.get(slot).getItem();
                return ItemStack.EMPTY;
            }
            if (entity != null) return entityNbtGet(slot);   // guarded client-side internally
            if (item != null)   return itemNbtGet(slot);
            return ItemStack.EMPTY;
        }

        /**
         * Removes up to {@code count} items from slot {@code slot} and returns them.
         * If the slot holds fewer than {@code count} items the whole stack is removed.
         * Returns {@link ItemStack#EMPTY} if the slot is already empty or unavailable.
         *
         * @param slot  slot index
         * @param count how many items to remove; use {@link Integer#MAX_VALUE} to remove the whole stack
         */
        public ItemStack removeItem(int slot, int count) {
            if (count <= 0) return ItemStack.EMPTY;

            PigeAutoContainer pac = findOpenContainer();
            if (pac != null) {
                if (slot < 0 || slot >= pac.getCustomSlotCount()) return ItemStack.EMPTY;
                net.minecraft.world.inventory.Slot s = pac.slots.get(slot);
                ItemStack existing = s.getItem();
                if (existing.isEmpty()) return ItemStack.EMPTY;
                if (existing.getCount() <= count) {
                    s.set(ItemStack.EMPTY);
                    return existing;
                }
                ItemStack removed = existing.copy();
                removed.setCount(count);
                existing.shrink(count);
                s.setChanged();
                return removed;
            }

            if (entity != null) return entityNbtRemove(slot, count);
            if (item != null)   return itemNbtRemove(slot, count);
            return ItemStack.EMPTY;
        }

        // ── Entity NBT helpers ────────────────────────────────────────────────

        private net.minecraft.nbt.CompoundTag entityGuiTag() {
            if (entity.level().isClientSide()) return new net.minecraft.nbt.CompoundTag();
            net.minecraft.nbt.CompoundTag data = entity.getPersistentData();
            String key = "PigeGui_" + gui.getId();
            return data.contains(key) ? data.getCompound(key) : new net.minecraft.nbt.CompoundTag();
        }

        private void entityNbtSet(ItemStack stack, int slot) {
            if (entity.level().isClientSide()) return;
            net.minecraft.nbt.CompoundTag guiTag = entityGuiTag();
            int size = Math.max(guiTag.getInt("Size"), slot + 1);
            net.minecraft.core.NonNullList<ItemStack> items =
                    net.minecraft.core.NonNullList.withSize(size, ItemStack.EMPTY);
            net.minecraft.world.ContainerHelper.loadAllItems(guiTag, items);
            items.set(slot, stack);
            net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
            saved.putInt("Size", size);
            net.minecraft.world.ContainerHelper.saveAllItems(saved, items, true);
            entity.getPersistentData().put("PigeGui_" + gui.getId(), saved);
        }

        private ItemStack entityNbtGet(int slot) {
            if (entity.level().isClientSide()) return ItemStack.EMPTY;
            net.minecraft.nbt.CompoundTag guiTag = entityGuiTag();
            int size = guiTag.getInt("Size");
            if (slot < 0 || slot >= size) return ItemStack.EMPTY;
            net.minecraft.core.NonNullList<ItemStack> items =
                    net.minecraft.core.NonNullList.withSize(size, ItemStack.EMPTY);
            net.minecraft.world.ContainerHelper.loadAllItems(guiTag, items);
            return items.get(slot);
        }

        private ItemStack entityNbtRemove(int slot, int count) {
            if (entity.level().isClientSide()) return ItemStack.EMPTY;
            ItemStack existing = entityNbtGet(slot);
            if (existing.isEmpty()) return ItemStack.EMPTY;
            if (existing.getCount() <= count) {
                entityNbtSet(ItemStack.EMPTY, slot);
                return existing;
            }
            ItemStack removed = existing.copy();
            removed.setCount(count);
            ItemStack remaining = existing.copy();
            remaining.shrink(count);
            entityNbtSet(remaining, slot);
            return removed;
        }

        // ── Item NBT helpers ──────────────────────────────────────────────────

        private void itemNbtSet(ItemStack stack, int slot) {
            net.minecraft.nbt.CompoundTag tag = item.getOrCreateTag();
            int size = Math.max(tag.getInt("Size"), slot + 1);
            net.minecraft.core.NonNullList<ItemStack> items =
                    net.minecraft.core.NonNullList.withSize(size, ItemStack.EMPTY);
            net.minecraft.world.ContainerHelper.loadAllItems(tag, items);
            items.set(slot, stack);
            net.minecraft.world.ContainerHelper.saveAllItems(tag, items, true);
        }

        private ItemStack itemNbtGet(int slot) {
            net.minecraft.nbt.CompoundTag tag = item.getTag();
            if (tag == null) return ItemStack.EMPTY;
            int size = tag.getInt("Size");
            if (slot < 0 || slot >= size) return ItemStack.EMPTY;
            net.minecraft.core.NonNullList<ItemStack> items =
                    net.minecraft.core.NonNullList.withSize(size, ItemStack.EMPTY);
            net.minecraft.world.ContainerHelper.loadAllItems(tag, items);
            return items.get(slot);
        }

        private ItemStack itemNbtRemove(int slot, int count) {
            ItemStack existing = itemNbtGet(slot);
            if (existing.isEmpty()) return ItemStack.EMPTY;
            if (existing.getCount() <= count) {
                itemNbtSet(ItemStack.EMPTY, slot);
                return existing;
            }
            ItemStack removed = existing.copy();
            removed.setCount(count);
            ItemStack remaining = existing.copy();
            remaining.shrink(count);
            itemNbtSet(remaining, slot);
            return removed;
        }
    }

    public static final class Background {

        private final LayoutBounds bounds;
        private final boolean texture;

        private Background(LayoutBounds bounds, boolean texture) {
            this.bounds = bounds;
            this.texture = texture;
        }

        public LayoutBounds getBounds() {
            return this.bounds;
        }

        public boolean hasTexture() {
            return this.texture;
        }

        public static Background create(LayoutBounds bounds, boolean texture) {
            return new Background(bounds, texture);
        }
    }
}