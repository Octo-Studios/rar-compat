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
        ModGameRules.DoubleValue instance = (ModGameRules.DoubleValue) (Object) this;

        if (instance.integerValue().equals(ModGameRules.CHORUS_TOTEM_TELEPORTATION_CHANCE.integerValue()) ||
                instance.integerValue().equals(ModGameRules.CLOUD_IN_A_BOTTLE_SPRINT_JUMP_VERTICAL_VELOCITY.integerValue()) ||
                instance.integerValue().equals(ModGameRules.CLOUD_IN_A_BOTTLE_SPRINT_JUMP_HORIZONTAL_VELOCITY.integerValue())) {
            return;
        }

        cir.setReturnValue(0.0);
    }
}
