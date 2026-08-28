package software.hacker_E303.pigeon_core.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import software.hacker_E303.pigeon_core.actions.IBasic;
import software.hacker_E303.pigeon_core.actions.IGratherEvent;
import software.hacker_E303.pigeon_core.init.PigeUtils;
import software.hacker_E303.pigeon_core.item.common.IEatResult;
import software.hacker_E303.pigeon_core.item.common.IItemModel;
import software.hacker_E303.pigeon_core.item.common.IItemTexture;
import software.hacker_E303.pigeon_core.util.BetterData;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;

public abstract class EItem extends Item implements IBasic, IItemTexture, IItemModel, IGratherEvent {

    protected final String modid = this.modid();
    protected final String pigeid = this.pigeid();

    private boolean food  = false;
    private int useDuration = 20;

    public EItem() {
        super(new Properties());
    }

    public EItem(int maxStackSize) {
        super(new Properties().stacksTo(maxStackSize));
    }

    public EItem(FoodProperties foodProperties, int useDuration) {
        super(new Properties().food(foodProperties));

        this.food = true;
        this.useDuration = useDuration;
    }

    public EItem(Rarity rarity, int durability) {
        super(new Properties().durability(durability).rarity(rarity));
    }

    @Override
    public Path getTexturePath(ItemStack stack) {
        return Path.TEXTURE.ITEMS;
    }


    @Override
    public String getTexture(ItemStack stack) {
        return BetterData.getData(stack, "Texture", "none");
    }

    @Override
    public void setTexture(ItemStack stack, String name) {
        BetterData.setData(stack, "Texture", name);
    }

    @Override
    public boolean gatherEvent(ItemStack stack, Level level, Player player) {
        return true;
    }

    public void instantiatingEvent(ItemStack stack) {
    }

    /**
     * Second overload, invoked whenever the stack enters an inventory slot with a
     * {@link Level} available (e.g. the player's inventory) — this can be more
     * than once per stack (see {@link software.hacker_E303.pigeon_core.item.util.SlotInitializer}
     * for why it isn't gated to exactly-once). Overrides that need the level
     * (e.g. {@code GeoItem.getOrAssignId}) MUST be idempotent themselves — check
     * their own NBT before acting, same as that example does. The default is a
     * no-op: it deliberately does NOT delegate to {@link #instantiatingEvent(ItemStack)},
     * which already fires exactly once (guarded) at construction.
     */
    public void instantiatingEvent(ItemStack stack, Level level) {
    }

    /**
     * Resolves the texture for a given stack. Items are singletons, so this MUST
     * not cache anything on the item instance (doing so would make every stack share
     * one texture and a {@code setTexture} on one stack would leak to all). The
     * location is recomputed from the stack's stored texture each call. {@code null}
     * stack (e.g. some renderer queries) falls back to the item's default texture.
     */
    @Override
    public ResourceLocation getTextureLocation(ItemStack stack) {
        String currentTexture = (stack != null) ? this.getTexture(stack) : "none";
        // The returned location is the SPRITE id inside the items atlas
        // (e.g. "<modid>:items/foo"), NOT a textures/ path. It must match the
        // sprite stitched by PigeonItemAtlas.stitchNow, which scans
        // textures/items/*.png + textures/misc/none.png for every framework
        // modid, so EItemModelHandler.resolve can look it up directly with
        // atlas.getSprite(...). Do NOT silently fall back to the placeholder:
        // a missing texture is intentionally surfaced as the missing sprite + a
        // warning by EItemModelHandler.
        ResourceLocation loc = Location.create(Path.create("items/", ""),
            currentTexture.replace("texture_", "")).from(this.modid);
        return loc;
    }

    // ---- IItemModel (parallel to IItemTexture, but for the model) ----

    @Override
    public Path getModelPath(ItemStack stack) {
        return Path.MODEL.ITEMS;
    }

    @Override
    public String getModel(ItemStack stack) {
        return BetterData.getData(stack, "Model", "none");
    }

    @Override
    public void setModel(ItemStack stack, String name) {
        BetterData.setData(stack, "Model", name);
    }

    @Override
    public ResourceLocation getModelLocation(ItemStack stack) {
        String currentModel = this.getModel(stack);
        // "none" (default) means "use the generated item/generated model for this id".
        if ("none".equals(currentModel))
            return new ResourceLocation(this.modid, "item/" + this.pigeid);
        // Bare model id ("<modid>:items/<name>"), matching PigeonItemModelRegistry's
        // key for models/items/<name>.json (NOT Path.MODEL.ITEMS, which carries the
        // "models/" prefix and ".json" suffix of the raw resource file, not a model id).
        return Location.create(Path.create("items/", ""),
            currentModel.replace("model_", "")).from(this.modid);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        if (this.isFood()) return useDuration; 
        return super.getUseDuration(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        ItemStack resultStack = super.finishUsingItem(stack, level, living);

        Effect effect = this.whenEaten();
        
        if (effect.effect() != null && !level.isClientSide())
            living.addEffect(new MobEffectInstance(effect.effect(), effect.duration(), effect.intensity()));

        if (this instanceof IEatResult result && result.getResult() != Items.AIR) {
            ItemStack drop = new ItemStack(result.getResult());
            
            if (living instanceof ServerPlayer player && player.gameMode.getGameModeForPlayer() != GameType.CREATIVE) {
                
                if (resultStack.isEmpty()) return drop; 
                else if (!player.getInventory().add(drop)) player.drop(drop, false);
            }
        }
        return resultStack;
    }

    @Override
    public final InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Interaction result = this.useEvent(stack, level, player);

        if (!result.equals(Interaction.DEFAULT))
            return result.equals(Interaction.SUCCESS) ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
        else return super.use(level, player, hand);
    }

    public Interaction useEvent(ItemStack stack, Level level, Player player) {
        return Interaction.DEFAULT;
    }

    protected enum Interaction {
        DEFAULT,
        FAIL,
        SUCCESS,
    }

    @Override
    public final void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        this.addTooltip(stack, tooltip);
    }

    public void addTooltip(ItemStack stack, List<Component> tooltip) {
    }

    public MutableComponent getTooltipLine(int index) {
        return Component.translatable("item." + PigeUtils.modidFrom(this) + "." + this.pigeid + ".tooltip" + index);
    }

    public boolean isFood() {
        return this.food;
    }

    public record Effect(MobEffect effect, int duration, int intensity) {

        public static final Effect NONE = new Effect(null, 0, 0);
    }

    public Effect whenEaten() {
        return Effect.NONE;
    }
}