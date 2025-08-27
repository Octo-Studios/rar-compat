package it.hurts.octostudios.rarcompat.mixin;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.hat.CowboyHatItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pig.class)
abstract class PigMixin {

    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
    private void getControllingPassenger(CallbackInfoReturnable<LivingEntity> cir) {
        Entity rider = ((Pig) (Object) this).getFirstPassenger();

        if (rider instanceof Player player) {
            ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.COWBOY_HAT.get());

            if (stack.getItem() instanceof CowboyHatItem relic && relic.getToggled(stack))
                cir.setReturnValue(player);
        }
    }
}