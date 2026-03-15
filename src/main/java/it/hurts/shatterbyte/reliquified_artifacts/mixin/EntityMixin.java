package it.hurts.shatterbyte.reliquified_artifacts.mixin;

import it.hurts.shatterbyte.reliquified_artifacts.items.feet.SteadfastSpikesItem;
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
    private Vec3 reliquified_artifacts$preFluidPushDelta;

    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void reliquified_artifacts$preventCollisionPush(double x, double y, double z, CallbackInfo ci) {
        var entity = (Entity) (Object) this;

        if (entity instanceof Player player && SteadfastSpikesItem.isAnchorActive(player))
            ci.cancel();
    }

    @Inject(method = "updateFluidHeightAndDoFluidPushing()V", at = @At("HEAD"))
    private void reliquified_artifacts$capturePreFluidPush(CallbackInfo ci) {
        var entity = (Entity) (Object) this;

        if (entity instanceof Player player && SteadfastSpikesItem.isAnchorActive(player))
            reliquified_artifacts$preFluidPushDelta = entity.getDeltaMovement();
        else
            reliquified_artifacts$preFluidPushDelta = null;
    }

    @Inject(method = "updateFluidHeightAndDoFluidPushing()V", at = @At("TAIL"))
    private void reliquified_artifacts$restorePreFluidPush(CallbackInfo ci) {
        if (reliquified_artifacts$preFluidPushDelta == null)
            return;

        var entity = (Entity) (Object) this;
        entity.setDeltaMovement(reliquified_artifacts$preFluidPushDelta);
        reliquified_artifacts$preFluidPushDelta = null;
    }
}