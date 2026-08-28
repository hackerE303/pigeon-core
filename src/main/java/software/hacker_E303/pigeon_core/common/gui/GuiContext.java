package software.hacker_E303.pigeon_core.common.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import software.hacker_E303.pigeon_core.util.locator.Location;

/**
 * Describes the UI of a {@link software.hacker_E303.pigeon_core.common.PigeGui}.
 * Fill it inside the appropriate {@code renderInterface()} overload — the framework
 * handles rendering and networking.
 *
 * <p>Method parameter order: String/label → Location → LayoutBounds → predicates/action → visible.
 */
public final class GuiContext {

    // ── Element records ───────────────────────────────────────────────────────

    public record ImageElement  (Location texture,   @Nullable LayoutBounds bounds, BooleanSupplier visible) {}
    public record TextElement   (Component text,     @Nullable LayoutBounds bounds, BooleanSupplier visible) {}
    public record TooltipElement(Component label, Component tip,
                                 @Nullable LayoutBounds bounds, BooleanSupplier visible) {}
    public record ButtonElement (Component label,    @Nullable Location texture,
                                 @Nullable LayoutBounds bounds,
                                 Consumer<PressAction> action, BooleanSupplier visible) {}
    public record SlotElement   (int index, @Nullable LayoutBounds bounds,
                                 Predicate<ItemStack> canInsert, Predicate<ItemStack> canExtract,
                                 BooleanSupplier visible) {}
    public record EntityElement (Entity entity,      @Nullable LayoutBounds bounds,
                                 Consumer<DisplayAction> displayAction, BooleanSupplier visible) {}
    public record ItemDisplayElement(ItemStack item, @Nullable LayoutBounds bounds,
                                     Consumer<DisplayAction> displayAction, BooleanSupplier visible) {}

    // ── Internal state ────────────────────────────────────────────────────────

    /**
     * Intern pool for fixed-key translatable components built by the render API.
     * Same {@code key} (no arguments) returns a shared {@link net.minecraft.network.chat.Component},
     * removing the per-frame allocation for labels/buttons/texts that use a constant key.
     */
    private static final int                                          KEY_POOL_MASK = 0x3FF; // 1024 buckets
    private static final net.minecraft.network.chat.Component[]        KEY_POOL      = new net.minecraft.network.chat.Component[KEY_POOL_MASK + 1];

    private static net.minecraft.network.chat.Component translatableKey(String key) {
        int idx = Math.floorMod(key.hashCode(), KEY_POOL_MASK + 1);
        net.minecraft.network.chat.Component existing = KEY_POOL[idx];
        if (existing != null && existing.getString().equals(net.minecraft.network.chat.Component.translatable(key).getString())
                && existing.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                && tc.getKey().equals(key)) {
            return existing;
        }
        net.minecraft.network.chat.Component created = net.minecraft.network.chat.Component.translatable(key);
        KEY_POOL[idx] = created;
        return created;
    }

    private final List<ImageElement>       images   = new ArrayList<>();
    private final List<TextElement>        texts    = new ArrayList<>();
    private final List<TooltipElement>     tooltips = new ArrayList<>();
    private final List<ButtonElement>      buttons  = new ArrayList<>();
    private final List<SlotElement>        slots    = new ArrayList<>();
    private final List<EntityElement>      entities = new ArrayList<>();
    private final List<ItemDisplayElement> itemDisplays = new ArrayList<>();

    // ── API ───────────────────────────────────────────────────────────────────

    /** Displays a texture. */
    public GuiContext renderImage(Location texture, @Nullable LayoutBounds bounds,
                                        BooleanSupplier visible) {
        images.add(new ImageElement(texture, bounds, visible));
        return this;
    }

    /** Displays a translatable text label. */
    public GuiContext renderText(String textKey, @Nullable LayoutBounds bounds,
                                        BooleanSupplier visible) {
        texts.add(new TextElement(translatableKey(textKey), bounds, visible));
        return this;
    }

    /** Displays a literal (non-translatable) text string. */
    public GuiContext renderLiteralText(String text, @Nullable LayoutBounds bounds,
                                               BooleanSupplier visible) {
        texts.add(new TextElement(Component.literal(text), bounds, visible));
        return this;
    }

    /**
     * Displays a label with a hoverable tooltip tip.
     * The label is shown normally; hovering it reveals the tip text as a tooltip.
     *
     * @param label translation key for the visible label
     * @param tip   translation key for the tooltip shown on hover
     * @param bounds   position, or {@code null} for auto-layout
     * @param visible  visibility condition
     */
    public GuiContext renderInfo(String label, String tip,
                                      @Nullable LayoutBounds bounds, BooleanSupplier visible) {
        tooltips.add(new TooltipElement(translatableKey(label), translatableKey(tip), bounds, visible));
        return this;
    }

    /**
     * Displays a basic hoverable tooltip.
     *
     * @param label translation key for the tooltip shown
     * @param bounds   position, or {@code null} for auto-layout
     * @param visible  visibility condition
     */
    public GuiContext renderTooltip(String label,
                                      @Nullable LayoutBounds bounds, BooleanSupplier visible) {
        tooltips.add(new TooltipElement(null, translatableKey(label), bounds, visible));
        return this;
    }

    /**
     * Displays a clickable button.
     *
     * @param label  translation key for the button label
     * @param texture   background texture, or {@code null} for the default style
     * @param bounds    position/size, or {@code null} for auto-layout
     * @param action    callback — receives a {@link PressAction}; called on both client and server
     * @param visible   visibility condition
     */
    public GuiContext renderButton(String label, @Nullable Location texture,
                                          @Nullable LayoutBounds bounds,
                                          Consumer<PressAction> action, BooleanSupplier visible) {
        buttons.add(new ButtonElement(translatableKey(label), texture, bounds, action, visible));
        return this;
    }

    /**
     * Adds a single item slot.
     *
     * @param bounds     position, or {@code null} for auto-layout
     * @param canInsert  whether an item may be placed in this slot
     * @param canExtract whether the current item may be taken
     * @param visible    visibility/interaction condition
     */
    public GuiContext renderSlot(@Nullable LayoutBounds bounds,
                                       Predicate<ItemStack> canInsert,
                                       Predicate<ItemStack> canExtract,
                                       BooleanSupplier visible) {
        slots.add(new SlotElement(slots.size(), bounds, canInsert, canExtract, visible));
        return this;
    }

    /**
     * Adds a horizontal row of {@code count} item slots.
     * Each slot is placed 18 px to the right of the previous one.
     *
     * @param count       number of slots in the row
     * @param bounds top-left position of the first slot, or {@code null} for auto-layout
     * @param canrender   predicate applied to every slot in the row
     * @param canExtract  predicate applied to every slot in the row
     * @param visible     visibility/interaction condition for all slots
     */
    public GuiContext renderSlots(int count, @Nullable LayoutBounds bounds,
                                        Predicate<ItemStack> canrender,
                                        Predicate<ItemStack> canExtract,
                                        BooleanSupplier visible) {
        for (int i = 0; i < count; i++) {
            LayoutBounds b = bounds != null
                ? LayoutBounds.create(bounds.getX() + i * 18, bounds.getY())
                : null;
            renderSlot(b, canrender, canExtract, visible);
        }
        return this;
    }

    /**
     * Renders an entity preview inside the GUI.
     * Use {@link LayoutBounds#create(int, int, float)} to set position and scale
     * (scale = pixels per block, e.g. {@code 30f}).
     *
     * @param entity        the entity to render (must be a client-side entity)
     * @param bounds        position and scale, or {@code null} for auto-layout
     * @param displayAction configures rotation, mouse tracking, etc.
     * @param visible       visibility condition
     */
    public GuiContext renderEntity(Entity entity, @Nullable LayoutBounds bounds,
                                          Consumer<DisplayAction> displayAction,
                                          BooleanSupplier visible) {
        entities.add(new EntityElement(entity, bounds, displayAction, visible));
        return this;
    }

    /**
     * Renders an {@link ItemStack} as a display element (not an interactive slot).
     * Use {@link LayoutBounds#create(int, int, float)} to set position and scale
     * (scale = size multiplier on the standard 16 × 16 icon).
     *
     * @param stack         the ItemStack to display
     * @param bounds        position and scale, or {@code null} for auto-layout
     * @param displayAction configures rotation, mouse tracking, etc.
     * @param visible       visibility condition
     */
    public GuiContext renderItem(ItemStack stack, @Nullable LayoutBounds bounds,
                                       Consumer<DisplayAction> displayAction,
                                       BooleanSupplier visible) {
        itemDisplays.add(new ItemDisplayElement(stack, bounds, displayAction, visible));
        return this;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public static class Data {

        private final GuiContext ctx;

        private Data(GuiContext ctx) {
            this.ctx = ctx;
        }

        public List<ImageElement> images() { return ctx.images; }
        public List<TextElement> texts() { return ctx.texts; }
        public List<TooltipElement> tooltips() { return ctx.tooltips; }
        public List<ButtonElement> buttons() { return ctx.buttons; }
        public List<SlotElement> slots() { return ctx.slots; }
        public List<EntityElement> entities() { return ctx.entities; }
        public List<ItemDisplayElement> itemDisplays() { return ctx.itemDisplays; }
    }

    public Data data() {
        return new Data(this);
    }
}