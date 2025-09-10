package it.hurts.octostudios.rarcompat.mixin;

import artifacts.item.wearable.necklace.CharmOfSinkingItem;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CharmOfSinkingItem.class)
public class CharmOfSinkingMixin {

    @Inject(method = "shouldSink(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void disableShouldSink(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}


