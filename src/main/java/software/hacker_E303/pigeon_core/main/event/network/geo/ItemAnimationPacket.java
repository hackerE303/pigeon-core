package software.hacker_E303.pigeon_core.main.event.network.geo;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.hacker_E303.pigeon_core.geo.item.gun.EGun;

/**
 * Server-to-client packet that triggers a GeckoLib animation on an entity-held item.
 */
public class ItemAnimationPacket {

    private final ItemStack stack;
    private final int entityId;
    private final String controllerName;
    private final String animationName;

    /**
     * Creates a new animation packet.
     *
     * @param stack          the animated item stack
     * @param entityId       the holder entity's id
     * @param controllerName the GeckoLib controller name
     * @param animationName  the animation to trigger
     */
    public ItemAnimationPacket(ItemStack stack, int entityId, String controllerName, String animationName) {

        this.stack = stack;
        this.entityId = entityId;
        this.controllerName = controllerName;
        this.animationName = animationName;
    }

    /**
     * Reads a packet from the network buffer.
     *
     * @param buffer the source buffer
     */
    public ItemAnimationPacket(FriendlyByteBuf buffer) {

        this.stack = buffer.readItem();
        this.entityId = buffer.readInt();
        this.controllerName = buffer.readUtf();
        this.animationName = buffer.readUtf();
    }

    /**
     * Writes this packet to the network buffer.
     *
     * @param msg    the packet to write
     * @param buffer the destination buffer
     */
    public static void encode(ItemAnimationPacket msg, FriendlyByteBuf buffer) {

        buffer.writeItemStack(msg.stack, false);
        buffer.writeInt(msg.entityId);
        buffer.writeUtf(msg.controllerName);
        buffer.writeUtf(msg.animationName);
    }

    /**
     * Reads a packet from the network buffer.
     *
     * @param buffer the source buffer
     * @return the decoded packet
     */
    public static ItemAnimationPacket decode(FriendlyByteBuf buffer) {
        return new ItemAnimationPacket(buffer.readItem(), buffer.readInt(), buffer.readUtf(), buffer.readUtf());
    }

    /**
     * Handles the packet on the client.
     *
     * @param msg            the received packet
     * @param contextSupplier the network context supplier
     */
    public static void handle(ItemAnimationPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {

        contextSupplier.get().enqueueWork(() -> {

            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {

                Entity entity = level.getEntity(msg.entityId);
                ItemStack stack = msg.stack;

                if (entity != null && stack != null) {
                    EGun.process(stack, gun -> {

                        long instanceId = gun.getGeckoId();

                        AnimatableInstanceCache cache = gun.getAnimatableInstanceCache();
                        AnimatableManager<?> animatableManager = cache.getManagerForId(instanceId);

                        String currentAnim = getCurrentAnim(animatableManager);

                        if (!currentAnim.isEmpty()) gun.stopTriggeredAnim(entity, instanceId, msg.controllerName, currentAnim);
                        gun.triggerAnim(entity, instanceId, msg.controllerName, msg.animationName);
                    });
                }
            }
        });
        contextSupplier.get().setPacketHandled(true);
    }

    /**
     * Returns the name of the currently playing animation, or empty string if none.
     *
     * @param animatableManager the GeckoLib animation manager
     * @return the current animation name, or empty string
     */
    private static String getCurrentAnim(AnimatableManager<?> animatableManager) {
        return animatableManager.getAnimationControllers().values().stream()

            .filter(controller -> controller.getCurrentAnimation() != null).findFirst()
            .map(controller -> controller.getCurrentAnimation().animation().name()).orElse("");
    }
}