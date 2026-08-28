package software.hacker_E303.pigeon_core.mixins;

import net.minecraft.network.syncher.SynchedEntityData;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Suppresses the {@code "Adding entity data accessor"} warning for
 * mod-defined classes while keeping the warning for vanilla/unknown classes.
 *
 * <environment_details>
 *     <env>CLIENT</env>
 *     <env>DEDICATED_SERVER</env>
 * </environment_details>
 */
@Mixin(SynchedEntityData.class)
public class SynchedEntityDataMixin {

    @Redirect(
        method = "defineId(Ljava/lang/Class;Lnet/minecraft/network/syncher/EntityDataSerializer;)Lnet/minecraft/network/syncher/EntityDataAccessor;",
        at = @At(
            value = "INVOKE", 
            target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"
        ),
        remap = false
    )
    private static void silenceDynamicRegistrationWarn(Logger logger, String message, Object p0, Object p1) {
        String callingClassName = p1.toString();

        if (callingClassName.contains("software.hacker_E303.pigeon_core")) return;
        logger.warn(message, p0, p1);
    }
}