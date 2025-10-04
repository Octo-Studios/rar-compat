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
                instance.equals(ModGameRules.EVERLASTING_BEEF_COOLDOWN) ||
                instance.equals(ModGameRules.CRYSTAL_HEART_HEALTH_BONUS) ||
                instance.equals(ModGameRules.CHORUS_TOTEM_HEALTH_RESTORED) ||
                instance.equals(ModGameRules.CHORUS_TOTEM_COOLDOWN) ||
                instance.equals(ModGameRules.ANTIDOTE_VESSEL_MAX_EFFECT_DURATION) ||
                instance.equals(ModGameRules.HELIUM_FLAMINGO_FLIGHT_DURATION) ||
                instance.equals(ModGameRules.HELIUM_FLAMINGO_RECHARGE_DURATION) ||
                instance.equals(ModGameRules.OBSIDIAN_SKULL_FIRE_RESISTANCE_COOLDOWN) ||
                instance.equals(ModGameRules.OBSIDIAN_SKULL_FIRE_RESISTANCE_DURATION) ||
                instance.equals(ModGameRules.ONION_RING_HASTE_DURATION_PER_FOOD_POINT) ||
                instance.equals(ModGameRules.ONION_RING_HASTE_LEVEL) ||
                instance.equals(ModGameRules.CHORUS_TOTEM_TELEPORTATION_CHANCE.integerValue()) ||
                instance.equals(ModGameRules.CLOUD_IN_A_BOTTLE_SPRINT_JUMP_VERTICAL_VELOCITY.integerValue()) ||
                instance.equals(ModGameRules.CLOUD_IN_A_BOTTLE_SPRINT_JUMP_HORIZONTAL_VELOCITY.integerValue())) {
            return;
        }

        cir.setReturnValue(0);
    }
}