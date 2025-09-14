package it.hurts.octostudios.rarcompat.mixin;

import artifacts.registry.ModGameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModGameRules.DoubleValue.class)
public class ModGameRulesMixin {
    @Inject(method = "get*", at = @At("HEAD"), cancellable = true, remap = false)
    private void disableFlippersSwimSpeed(CallbackInfoReturnable<Double> cir) {
        ModGameRules.DoubleValue instance = (ModGameRules.DoubleValue) (Object) this;
        if (instance.equals(ModGameRules.FLIPPERS_SWIM_SPEED_BONUS)) {
            cir.setReturnValue(0.0);
        }
    }
}

