package it.hurts.octostudios.rarcompat.mixin;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.items.hat.VillagerHatItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {
    @Shadow
    private Player tradingPlayer;

    @Redirect(method = "notifyTrade", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;increaseUses()V"))
    private void rarcompat$preserveTradeUse(MerchantOffer offer) {
        var player = this.tradingPlayer;

        if (player == null || player.level().isClientSide()) {
            offer.increaseUses();
            return;
        }

        var stack = EntityUtils.findEquippedCurio(player, ModItems.VILLAGER_HAT.value());

        if (!(stack.getItem() instanceof VillagerHatItem relic)) {
            offer.increaseUses();
            return;
        }

        if (!relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("trade_surge").canPlayerUse(player)
                || !relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("trade_surge").isRankModifierUnlocked("preserve")) {
            offer.increaseUses();
            return;
        }

        var chance = Math.max(0D, Math.min(1D, relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("trade_surge").getStatData("preserve_chance").getValue()));

        if (chance <= 0D || player.getRandom().nextDouble() >= chance)
            offer.increaseUses();
    }
}