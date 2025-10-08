package it.hurts.octostudios.rarcompat.mixin;

import artifacts.registry.ModGameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModGameRules.IntegerValue.class)
class ModGameRulesIntegerMixin {
    @Inject(method = "get*", at = @At("HEAD"), cancellable = true, remap = false)
    private void disableAllIntegerGameRules(CallbackInfoReturnable<Integer> cir) {
        ModGameRules.IntegerValue instance = (ModGameRules.IntegerValue) (Object) this;

        if (instance.equals(ModGameRules.ETERNAL_STEAK_COOLDOWN) ||
                instance.equals(ModGameRules.EVERLASTING_BEEF_COOLDOWN)) {
            return;
        }

        cir.setReturnValue(0);
    }
}