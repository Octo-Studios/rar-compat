package it.hurts.octostudios.rarcompat.mixin;

import artifacts.registry.ModGameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModGameRules.DoubleValue.class)
public class ModGameRulesDoubleMixin {
    @Inject(method = "get*", at = @At("HEAD"), cancellable = true, remap = false)
    private void disableAllDoubleGameRules(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(0.0);
    }
}
