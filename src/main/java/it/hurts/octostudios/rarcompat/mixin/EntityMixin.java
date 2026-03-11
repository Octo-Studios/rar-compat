package it.hurts.octostudios.rarcompat.mixin;

import it.hurts.octostudios.rarcompat.items.feet.SteadfastSpikesItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Unique
    private Vec3 rarcompat$preFluidPushDelta;

    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void rarcompat$preventCollisionPush(double x, double y, double z, CallbackInfo ci) {
        var entity = (Entity) (Object) this;

        if (entity instanceof Player player && SteadfastSpikesItem.isAnchorActive(player))
            ci.cancel();
    }

    @Inject(method = "updateFluidHeightAndDoFluidPushing()V", at = @At("HEAD"))
    private void rarcompat$capturePreFluidPush(CallbackInfo ci) {
        var entity = (Entity) (Object) this;

        if (entity instanceof Player player && SteadfastSpikesItem.isAnchorActive(player))
            rarcompat$preFluidPushDelta = entity.getDeltaMovement();
        else
            rarcompat$preFluidPushDelta = null;
    }

    @Inject(method = "updateFluidHeightAndDoFluidPushing()V", at = @At("TAIL"))
    private void rarcompat$restorePreFluidPush(CallbackInfo ci) {
        if (rarcompat$preFluidPushDelta == null)
            return;

        var entity = (Entity) (Object) this;
        entity.setDeltaMovement(rarcompat$preFluidPushDelta);
        rarcompat$preFluidPushDelta = null;
    }
}