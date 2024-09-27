package it.hurts.octostudios.rarcompat.mixin;

import artifacts.registry.ModItems;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import it.hurts.octostudios.rarcompat.items.bunch.NightVisionGogglesItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @ModifyReturnValue(method = "getNightVisionScale", at = @At("RETURN"))
    private static float getNightVisionScale(float scale, LivingEntity entity, float p_109110_) {
        if (entity instanceof Player player) {
            if (player.hasEffect(MobEffects.NIGHT_VISION)) {
                ItemStack relicStack = EntityUtils.findEquippedCurio(player, ModItems.NIGHT_VISION_GOGGLES.value());
                if (relicStack != null && relicStack.getItem() instanceof NightVisionGogglesItem goggles) {
                    double gamma = goggles.getStatValue(relicStack, "night_vision", "brightness_amount");
                    return (float) Mth.lerp(gamma, 0.005D, 5D);
                }
            }
        }
        return scale;
    }
}
