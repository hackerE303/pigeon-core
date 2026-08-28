package software.hacker_E303.pigeon_core.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.hacker_E303.pigeon_core.entity.EMob;
import software.hacker_E303.pigeon_core.entity.ETurret;

/**
 * Applies extra behaviour when an {@code EMob} spawns from a spawn egg:
 * assigns ownership for {@link ETurret} instances and runs the entity
 * generation event, discarding the entity if it is refused.
 *
 * <environment_details>
 *     <env>CLIENT</env>
 *     <env>DEDICATED_SERVER</env>
 * </environment_details>
 */
@Mixin(SpawnEggItem.class)
public class SpawnEggItemMixin {

    @Inject(method = "useOn", at = @At("RETURN"), remap = false)
    private void whenSpawned(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {

        Level level = context.getLevel();
        if (level.isClientSide() || context.getPlayer() == null) return;

        if (cir.getReturnValue() == InteractionResult.CONSUME) {

            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            List<EMob> mobs = level.getEntitiesOfClass(EMob.class, new AABB(pos).inflate(2));

            for (EMob mob : mobs) {

                if (mob.tickCount != 0) continue;
                Player player = context.getPlayer();

                if (mob instanceof ETurret turret) turret.setOwnerName(player.getScoreboardName());
                if (!mob.generationEvent(player)) mob.discard();
            }
        }
    }
}