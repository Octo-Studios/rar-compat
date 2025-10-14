package it.hurts.octostudios.rarcompat.mixin;

import artifacts.registry.ModGameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModGameRules.BooleanValue.class)
class ModGameRulesBooleanMixin {
    @Inject(method = "get*", at = @At("HEAD"), cancellable = true, remap = false)
    private void disableAllBooleanGameRules(CallbackInfoReturnable<Boolean> cir) {
        ModGameRules.BooleanValue instance = (ModGameRules.BooleanValue) (Object) this;

        if (instance.equals(ModGameRules.AQUA_DASHERS_ENABLED) ||
                instance.equals(ModGameRules.ETERNAL_STEAK_ENABLED) ||
                instance.equals(ModGameRules.EVERLASTING_BEEF_ENABLED)) {
            return;
        }

        cir.setReturnValue(false);
    }
}