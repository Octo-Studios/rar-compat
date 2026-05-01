package it.hurts.shatterbyte.reliquified_artifacts.mixin;

import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.items.hat.VillagerHatItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.world.entity.npc.AbstractVillager;
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
    private void reliquified_artifacts$preserveTradeUse(MerchantOffer offer) {
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

        var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("trade_surge");

        if (!ability.canPlayerUse(player) || !ability.getRankModifierData("preserve").isUnlocked()) {
            offer.increaseUses();
            return;
        }

        var chance = Math.max(0D, Math.min(1D, ability.getStatData("preserve_chance").getValue()));

        if (chance <= 0D || player.getRandom().nextDouble() >= chance) {
            offer.increaseUses();
            return;
        }

        VillagerHatItem.onTradePreserved(player, stack);
    }
}

