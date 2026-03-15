package it.hurts.shatterbyte.reliquified_artifacts.mixin;

import it.hurts.shatterbyte.reliquified_artifacts.items.charm.ObsidianSkullItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @ModifyArg(
            method = "travel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V", ordinal = 1),
            index = 0
    )
    private float reliquified_artifacts$boostLavaMovement(float speed) {
        var entity = (LivingEntity) (Object) this;

        if (!(entity instanceof Player player))
            return speed;

        var bonus = ObsidianSkullItem.getHeatSurgeLavaSpeedBonus(player);

        if (bonus <= 0D)
            return speed;

        return (float) (speed * (1D + bonus));
    }
}