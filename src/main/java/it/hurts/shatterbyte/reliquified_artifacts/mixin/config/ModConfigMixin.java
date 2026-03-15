package it.hurts.shatterbyte.reliquified_artifacts.mixin.config;

import artifacts.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModConfig.class)
public class ModConfigMixin {

    @Inject(method = "setup", at = @At("HEAD"), cancellable = true)
    private void getNightVisionScale(CallbackInfo ci) {
        ci.cancel();
    }

}
