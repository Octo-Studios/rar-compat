package it.hurts.octostudios.rarcompat.mixin;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.hat.CowboyHatItem;
import it.hurts.octostudios.rarcompat.items.hat.VillagerHatItem;
import it.hurts.sskirillss.relics.init.DataComponentRegistry;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public class AbstractHorseMixin {
    @Inject(method = "canJump", at = @At("HEAD"), cancellable = true)
    private void disableJumpStrength(CallbackInfoReturnable<Boolean> cir) {
        Mob mob = (Mob) (Object) this;
        Entity rider = mob.getFirstPassenger();

        if (!(mob instanceof LivingEntity) || !(rider instanceof Player player))
            return;

        ItemStack stack = EntityUtils.findEquippedCurio(player, ModItems.COWBOY_HAT.value());

        if (!(stack.getItem() instanceof CowboyHatItem relic) || !relic.isAbilityUnlocked(stack, "overlord")
                || !relic.getToggled(stack))
            return;

        cir.setReturnValue(false);
    }
}
