package it.hurts.octostudios.rarcompat.mixin;

import it.hurts.octostudios.rarcompat.items.feet.KittySlippersItem;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public class CreeperMixin {
    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void rarcompat$preventExplosionFromFelineAura(CallbackInfo ci) {
        var creeper = (Creeper) (Object) this;

        if (!KittySlippersItem.shouldSuppressCreeperExplosion(creeper))
            return;

        creeper.setSwellDir(-1);
        ci.cancel();
    }
}