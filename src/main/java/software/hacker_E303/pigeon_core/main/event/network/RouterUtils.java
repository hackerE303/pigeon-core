package software.hacker_E303.pigeon_core.main.event.network;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import software.hacker_E303.pigeon_core.main.event.network.geo.ItemAnimationPacket;
import software.hacker_E303.pigeon_core.main.event.network.gun.*;
import software.hacker_E303.pigeon_core.main.event.network.debug.*;

/**
 * Helper methods for sending packets and tracking per-player state.
 */
public final class RouterUtils {

    /**
     * Server-side packet senders.
     */
    public static class Internal {

        /**
         * Notifies the client that the entity is now holding a gun.
         *
         * @param stack  the held gun ItemStack
         * @param entity the holding entity (must be a {@link ServerPlayer})
         */
        // CALL FROM SERVER
        public static void startHoldingGun(ItemStack stack, Entity entity) {

            PigeNetworking.CHANNEL.sendTo(new GunHoldPacket(stack, entity.getId()),
                ((ServerPlayer) entity).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }

        /**
         * Requests a gun reload from the client.
         */
        // CALL FROM CLIENT
        public static void startReloadingGun() {
            PigeNetworking.CHANNEL.sendToServer(new GunReloadPacket());
        }

        /**
         * Spawns a bullet-hole effect around all players tracking the given chunk.
         *
         * @param pos   world-space position
         * @param level the level
         * @param face  the hit face
         */
        // CALL FROM SERVER
        public static void spawnBulletHole(Vec3 pos, Level level, Direction face) {

            PigeNetworking.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() ->
                level.getChunkAt(BlockPos.containing(pos))), new BulletHolePacket(pos.x, pos.y, pos.z, face));
        }
    }

    /**
     * GeckoLib animation senders.
     */
    public static class Geckolib {

        /**
         * Sends an animation trigger to all players within 32 blocks.
         *
         * @param stack      the animated item stack
         * @param level      the level
         * @param entity     the animation target entity
         * @param controller the GeckoLib controller name
         * @param animation  the animation name
         */
        // CALL FROM SERVER
        public static void playAnimation(ItemStack stack, Level level, Entity entity, String controller, String animation) {
            if (level.isClientSide) return;

            for (Player player : level.players())
                if (player.distanceToSqr(entity) <= 1024.0) {
                    
                    ItemAnimationPacket packet = new ItemAnimationPacket(stack, entity.getId(), controller, animation);
                    PigeNetworking.CHANNEL.sendTo(packet, ((ServerPlayer) player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
        }

        /**
         * Sends an animation trigger only to the specified entity's controller.
         *
         * @param stack      the animated item stack
         * @param level      the level
         * @param entity     the animation target entity
         * @param controller the GeckoLib controller name
         * @param animation  the animation name
         */
        // CALL FROM SERVER
        public static void playLocalAnimation(ItemStack stack, Level level, Entity entity, String controller, String animation) {
            if (level.isClientSide || !(entity instanceof ServerPlayer serverPlayer)) return;

            ItemAnimationPacket packet = new ItemAnimationPacket(stack, serverPlayer.getId(), controller, animation);
            PigeNetworking.CHANNEL.sendTo(packet, serverPlayer.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    /**
     * Debug packet senders.
     */
    public static class Debug {

        /**
         * Sends a texture-check request to the client(s).
         *
         * @param entity   the reference entity (used for dimension scoping)
         * @param location the resource location to check
         * @param everyone true to send to every player in the dimension
         */
        // CALL FROM SERVER
        public static void ensureTexture(Entity entity, ResourceLocation location, boolean everyone) {
            PigeNetworking.CHANNEL.send(PacketDistributor.DIMENSION.with(() -> entity.level().dimension()), new CheckTexturePacket(location, everyone));
        }

        /**
         * Sends a resource-check request to a single client — unlike
         * {@link #ensureTexture}, which broadcasts to a whole dimension. Used for
         * per-player, one-shot checks (e.g. the first time a player receives an
         * item of a given type).
         *
         * @param player   the player whose client should verify the resource
         * @param location the resource location to check
         * @param everyone true to forward the resulting warning to every player
         */
        // CALL FROM SERVER
        public static void ensureResource(ServerPlayer player, ResourceLocation location, boolean everyone) {
            PigeNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CheckTexturePacket(location, everyone));
        }

        /**
         * Requests a missing-resource warning from the server.
         *
         * @param location the resource location that failed to load
         * @param everyone true to warn all players, false to warn only the sender
         */
        // CALL FROM CLIENT
        public static void resourceWarning(ResourceLocation location, boolean everyone) {
            PigeNetworking.CHANNEL.sendToServer(new ResourceWarningPacket(location, everyone));
        }
    }

    /**
     * Tracks per-player mouse button state across the server.
     */
    public static class Mouse {

        /**
         * Returns whether the left mouse button is held by the given player.
         *
         * @param entity the entity to check (must be a {@link ServerPlayer})
         * @return true if the left button is held
         */
        // CALL FROM SERVER
        public static boolean isLeftHeld(Entity entity) {

            if (entity instanceof ServerPlayer serverPlayer) {
                return MouseHeldData.get(serverPlayer).isLeftHeld();
            }
            return false;
        }

        /**
         * Returns whether the right mouse button is held by the given player.
         *
         * @param entity the entity to check (must be a {@link ServerPlayer})
         * @return true if the right button is held
         */
        // CALL FROM SERVER
        public static boolean isRightHeld(Entity entity) {

            if (entity instanceof ServerPlayer serverPlayer) {
                return MouseHeldData.get(serverPlayer).isRightHeld();
            }
            return false;
        }

        /**
         * Per-player held-button cache.
         */
        public static class MouseHeldData {

            private static final Map<ServerPlayer, MouseHeldData> DATA = new HashMap<>();

            private boolean left;
            private boolean right;

            /**
             * Returns the held-button data for the given player.
             *
             * @param player the player
             * @return the cached (or newly created) data instance
             */
            public static MouseHeldData get(ServerPlayer player) {
                return DATA.computeIfAbsent(player, p -> new MouseHeldData());
            }

            /**
             * @return true if the left button is held
             */
            public boolean isLeftHeld() {
                return left;
            }

            /**
             * @return true if the right button is held
             */
            public boolean isRightHeld() {
                return right;
            }

            /**
             * Sets the left-button held state.
             *
             * @param value true if held
             */
            public void setLeftHeld(boolean value) {
                this.left = value;
            }

            /**
             * Sets the right-button held state.
             *
             * @param value true if held
             */
            public void setRightHeld(boolean value) {
                this.right = value;
            }
        }
    }
}