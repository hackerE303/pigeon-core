package software.hacker_E303.pigeon_core.item.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.hacker_E303.pigeon_core.item.EItem;
import software.hacker_E303.pigeon_core.util.BetterData;

/**
 * Initializes {@link EItem} instances when an ItemStack is created.
 *
 * <p>Every real {@code ItemStack} of an {@link EItem} receives its essential NBT
 * ({@code Texture}/{@code Model}) and the {@link EItem#instantiatingEvent(ItemStack)}
 * event as soon as it is constructed. This happens on <b>both sides</b>:
 * <ul>
 *   <li>On the <b>server</b> side the event is "authoritative": the user may write
 *       custom NBT in the {@code instantiatingEvent} override, which is then synced
 *       to the client along with the stack;</li>
 *   <li>On the <b>client</b> side the essential NBT must still be present because it
 *       is needed for rendering (e.g. creative-tab previews are client-only stacks
 *       never synced from the server).</li>
 * </ul>
 * As a result {@code instantiatingEvent} may fire once per side: this is the intended
 * and unavoidable behavior given the client-side rendering requirement.</p>
 *
 * <p>When the stack enters a context with a {@link Level} (e.g. a player inventory),
 * the overload {@link EItem#instantiatingEvent(ItemStack, Level)} is also invoked —
 * but, unlike the no-level event, NOT gated by a persisted "already fired" NBT
 * flag. An early version used one ({@code EventWithLevelFired}); it broke vanilla
 * stack merging: a stack already sitting in the inventory had already picked up
 * the flag from a previous slot-entry, while a freshly obtained one (e.g. clicked
 * from the creative tab) hadn't yet, so the two compared as having different tags
 * and vanilla refused to merge them — through more call paths (menu clicks,
 * hopper transfers, etc.) than can practically be pre-empted one by one. So this
 * overload may fire more than once per stack; overrides must be idempotent
 * (checking their own NBT before acting), same as the framework's own
 * {@code GeoItem.getOrAssignId} example.</p>
 *
 * <p>The only creation paths excluded from initialization are those that do not
 * represent a brand-new object: {@link ItemStack#copy()} (the copy inherits the
 * original's tag, including flags), suppressed via {@link #enterCopy()}/
 * {@link #exitCopy()}.</p>
 *
 * <p>Per-side idempotency: {@code Initialized} protects the essential NBT writes
 * (without overwriting custom values); {@code EventFired} prevents re-running
 * {@code instantiatingEvent(ItemStack)} on the same stack and its copies (which
 * inherit the flag). Both are written once, at construction, so — unlike the
 * removed with-level flag — every stack of a given item has them from the start
 * and they never create a merge-blocking asymmetry.</p>
 */
public final class SlotInitializer {

    private SlotInitializer() {}

    private static final String KEY_INITIALIZED = "Initialized";
    private static final String KEY_EVENT_FIRED = "EventFired";

    // True while the current thread is executing ItemStack.copy(): the copy
    // already inherits the original's tag, so the internal constructor must not initialize.
    private static final ThreadLocal<Boolean> IN_COPY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Entry point from the base {@link ItemStack} constructor. Initializes the stack
     * as soon as it is created (not during a {@code copy()}), on both sides.
     */
    public static void initializeFromConstructor(ItemStack stack) {
        if (IN_COPY.get()) return;
        initialize(stack);
    }

    /** Marks the beginning of {@code ItemStack.copy()} on the current thread. */
    public static void enterCopy() {
        IN_COPY.set(Boolean.TRUE);
    }

    /** Marks the end of {@code ItemStack.copy()} on the current thread. */
    public static void exitCopy() {
        IN_COPY.set(Boolean.FALSE);
    }

    /**
     * Called when the stack enters an inventory slot where a {@link Level} is
     * available (e.g. the player inventory). Ensures essential NBT, propagates
     * {@code instantiatingEvent(ItemStack, Level)} (see the class doc — NOT
     * gated to "exactly once", deliberately: gating it broke stack merging), and —
     * server-side only, once per (player, pigeid) — checks the stack's texture
     * and model for missing resources (see {@link ItemResourceChecker}).
     */
    public static void onEnterSlotWithLevel(ItemStack stack, Player player, Level level) {
        if (stack == null || stack.isEmpty()) return;
        if (!(stack.getItem() instanceof EItem event)) return;

        writeEssentialData(stack);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
            ItemResourceChecker.checkOnce(serverPlayer, stack);

        event.instantiatingEvent(stack, level);
    }

    /**
     * Writes essential NBT (only if missing) and propagates {@code instantiatingEvent}
     * exactly once per stack (per side). Idempotent via NBT flags.
     */
    private static void initialize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (!(stack.getItem() instanceof EItem event)) return;

        writeEssentialData(stack);

        if (BetterData.hasData(stack, KEY_EVENT_FIRED)) return;
        BetterData.setData(stack, KEY_EVENT_FIRED, true);

        event.instantiatingEvent(stack);
    }

    private static void writeEssentialData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (!(stack.getItem() instanceof EItem event)) return;
        if (BetterData.hasData(stack, KEY_INITIALIZED)) return;

        String pigeid = event.pigeid();
        if (!BetterData.hasData(stack, "Texture")) {
            // Bare sprite name, no ".png" — EItem#getTextureLocation resolves this
            // directly against the atlas sprite id (items/<name>, see
            // PigeonItemAtlas#toSpriteId), which never carries the file suffix.
            BetterData.setData(stack, "Texture", pigeid);
        }
        if (!BetterData.hasData(stack, "Model")) {
            BetterData.setData(stack, "Model", pigeid);
        }
        BetterData.setData(stack, KEY_INITIALIZED, true);
    }
}
