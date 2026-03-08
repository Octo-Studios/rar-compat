package it.hurts.octostudios.rarcompat.mixin.render;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.hat.NightVisionGogglesItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void getNightVisionScale(LivingEntity entity, float gamma, CallbackInfoReturnable<Float> cir) {
        if (!(entity instanceof Player player))
            return;

        var stack = EntityUtils.findEquippedCurio(player, ModItems.NIGHT_VISION_GOGGLES.value());

        if (!(stack.getItem() instanceof NightVisionGogglesItem relic))
            return;

        var abilityData = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("vision");

        if (!abilityData.getMode().equals("enabled") || !abilityData.isMaxLevel() && !relic.isNightVision(player.getActiveEffects()))
            return;

        cir.setReturnValue((float) abilityData.getStatData("amount").getValue());
    }
}