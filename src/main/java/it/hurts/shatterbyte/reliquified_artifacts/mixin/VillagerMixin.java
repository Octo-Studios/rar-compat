package it.hurts.shatterbyte.reliquified_artifacts.mixin;

import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.items.hat.VillagerHatItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerMixin {
    @Inject(method = "updateSpecialPrices", at = @At("TAIL"))
    private void reliquified_artifacts$applyVillagerHatDiscount(Player player, CallbackInfo ci) {
        var villager = (Villager) (Object) this;

        if (player == null || villager.level().isClientSide())
            return;

        var stack = EntityUtils.findEquippedCurio(player, ModItems.VILLAGER_HAT.value());

        if (!(stack.getItem() instanceof VillagerHatItem relic))
            return;

        if (!relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("trade_surge").canPlayerUse(player))
            return;

        var discount = Math.max(0D, Math.min(1D, relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("trade_surge").getStatData("discount").getValue()));

        if (discount <= 0D)
            return;

        for (var offer : villager.getOffers()) {
            var price = offer.getBaseCostA().getCount();
            var reduction = Math.max(0, (int) MathUtils.round(price * discount, 0));

            if (reduction > 0)
                offer.addToSpecialPriceDiff(-reduction);
        }
    }
}
