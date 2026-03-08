package it.hurts.octostudios.rarcompat.mixin.config;

import artifacts.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConfigManager.class)
public class ConfigManagerMixin<T, C> {

    @Inject(method = "readValuesFromConfig", at = @At("HEAD"), cancellable = true)
    public void readValuesFromConfig(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "setup", at = @At("HEAD"), cancellable = true)
    private void setCi(CallbackInfo ci) {
        ci.cancel();
    }
}
