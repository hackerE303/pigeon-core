package software.hacker_E303.pigeon_core.util.world;

import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import software.hacker_E303.pigeon_core.PigeonCore;
import software.hacker_E303.pigeon_core.util.BetterTexts;

/**
 * Player interaction and feedback utilities using a fluent {@link PlayerUtilsContext}.
 */
public final class PlayerUtils {

    /**
     * Opens a {@link PlayerUtilsContext} for the given player and executes the provided action.
     *
     * @param modid  the mod id used for translation keys
     * @param player the target player
     * @param ctx    the action to run with the context
     */
    public static void with(String modid, Player player, Consumer<PlayerUtilsContext> ctx) {
        ctx.accept(new PlayerUtilsContext(modid, player));
    }

    /**
     * Opens a {@link PlayerUtilsContext} for the given player and executes the provided function.
     *
     * @param <T>          the return type
     * @param modid        the mod id used for translation keys
     * @param player       the target player
     * @param ctx          the function to apply
     * @param defaultValue value returned if the context yields no result
     * @return the function result, or {@code defaultValue}
     */
    public static <T> T with(String modid, Player player, Function<PlayerUtilsContext, T> ctx, T defaultValue) {
        return ctx.apply(new PlayerUtilsContext(modid, player));
    }

    /**
     * Fluent context for issuing feedback to a single {@link Player}.
     */
    public static final class PlayerUtilsContext {

        private final Player player;
        private final String modid;
        
        /**
         * Creates a new context for the given player.
         *
         * @param modid  the mod id used for translation keys
         * @param player the target player
         */
        private PlayerUtilsContext(String modid, Player player) {
            this.modid = modid;
            this.player = player;
        }

        /**
         * Sends a raw text system message to the player.
         *
         * @param text the message text
         * @return this context for chaining
         */
        public PlayerUtilsContext literalMessage(String text) {
            player.sendSystemMessage(Component.literal(text));
            return this;
        }

        /**
         * Sends a translatable system message to the player.
         *
         * @param label the translation key suffix
         * @param in    the translation arguments
         * @return this context for chaining
         */
        public PlayerUtilsContext translatableMessage(String label, Object... in) {
            player.sendSystemMessage(Component.translatable("message." + modid + "." + label, in));
            return this;
        }

        /**
         * Sends a debug system message with the mod name prefixed in yellow and
         * the message body in gray.
         *
         * @param label the translation key suffix
         * @param in    the translation arguments
         * @return this context for chaining
         */
        public PlayerUtilsContext debugMessage(String label, Object... in) {
            Object[] coloredArgs = new Object[in.length];
            
            for (int i = 0; i < in.length; i++) {
                if (in[i] instanceof Component component) coloredArgs[i] = component.copy().withStyle(ChatFormatting.RED);
                else coloredArgs[i] = Component.literal(String.valueOf(in[i])).withStyle(ChatFormatting.RED);
            }
            player.sendSystemMessage(
                Component.literal("§e[" + BetterTexts.titleCase(modid) + "] ").append(
                    Component.translatable("message." + modid + "." + label, coloredArgs).withStyle(ChatFormatting.GRAY))
            );
            return this;
        }

        /**
         * Plays a notify sound by resource id.
         *
         * @param id the resource id suffix
         * @return this context for chaining
         */
        public PlayerUtilsContext playNotifySound(String id) {
            this.playNotifySound(PigeonCore.getSound(modid, id));
            return this;
        }

        /**
         * Plays a notify sound.
         *
         * @param sound the sound event to play
         * @return this context for chaining
         */
        public PlayerUtilsContext playNotifySound(SoundEvent sound) {
            player.playNotifySound(sound, SoundSource.MASTER, 1.0f, 1.0f);
            return this;
        }

        /**
         * Checks whether there is a solid block within the player's line of sight.
         *
         * @param reach the maximum ray-trace distance
         * @return {@code true} if a solid block was hit
         */
        public boolean hasSolidBlockInSight(double reach) {

            Vec3 eyePos = player.getEyePosition();
            Vec3 lookVec = player.getViewVector(1.0F);
            
            Vec3 endPos = eyePos.add(lookVec.x * reach, lookVec.y * reach, lookVec.z * reach);
            ClipContext context = new ClipContext(
                eyePos, 
                endPos, 
                ClipContext.Block.COLLIDER, 
                ClipContext.Fluid.NONE, 
                player
            );
            BlockHitResult hitResult = player.level().clip(context);
            return hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK;
        }
    }
}