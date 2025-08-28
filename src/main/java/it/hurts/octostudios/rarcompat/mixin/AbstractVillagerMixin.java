package it.hurts.octostudios.rarcompat.mixin;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.hat.VillagerHatItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractVillager.class)
abstract class AbstractVillagerMixin {
    @Shadow
    private Player tradingPlayer;

    @Shadow
    protected MerchantOffers offers;

    @Inject(method = "notifyTrade ", at = @At(value = "HEAD"))
    private void notifyTrade(MerchantOffer offer, CallbackInfo ci) {
        ItemStack relicStack = EntityUtils.findEquippedCurio(tradingPlayer, ModItems.VILLAGER_HAT.get());

        if (!(relicStack.getItem() instanceof VillagerHatItem relic) || offers == null || !relic.canUseAbility(relicStack, "discount"))
            return;

        int newPrice = (int) Math.round(offer.getCostA().getCount() * relic.getAbilityValue(relicStack, "discount", "multiplier"));

        if (newPrice > 1)
            relic.spreadExperience(tradingPlayer, relicStack, 1 + tradingPlayer.getRandom().nextInt(newPrice) + 1);
    }

    @Inject(method = "getOffers", at = @At(value = "HEAD"))
    private void getOffers(CallbackInfoReturnable<MerchantOffers> cir) {
        ItemStack relicStack = EntityUtils.findEquippedCurio(tradingPlayer, ModItems.VILLAGER_HAT.get());

        if (!(relicStack.getItem() instanceof VillagerHatItem relic) || offers == null || !relic.canUseAbility(relicStack, "discount"))
            return;

        for (MerchantOffer offer : offers) {
            int newPrice = (int) Math.round(offer.getCostA().getCount() * relic.getAbilityValue(relicStack, "discount", "multiplier") / 100);

            offer.setSpecialPriceDiff(-newPrice);
        }
    }
}