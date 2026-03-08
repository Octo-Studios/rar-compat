package it.hurts.octostudios.rarcompat.items.hat;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import top.theillusivec4.curios.api.SlotContext;

public class VillagerHatItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("golem_guard")
                                .rankModifier(1, "protection")
                                .stat(AbilityStatTemplate.builder("damage_reduction_per_golem")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.02D, 0.05D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("radius")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(8D, 16D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("max_damage_reduction")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.15D, 0.4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .build())
                        .ability(AbilityTemplate.builder("trade_surge")
                                .rankModifier(3, "preserve")
                                .rankModifier(5, "multicast")
                                .stat(AbilityStatTemplate.builder("discount")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("multicast")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("preserve_chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        if (!this.getRelicData(player, stack).getAbilitiesData().getAbilityData("golem_guard").canPlayerUse(player))
            return;

        if (player.tickCount % 20 != 0)
            return;

        for (var golem : player.level().getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(24D), golem -> golem.getTarget() == player))
            golem.setTarget(null);
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
            if (!(event.getEntity() instanceof IronGolem golem) || golem.level().isClientSide() || !(event.getNewAboutToBeSetTarget() instanceof Player player))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.VILLAGER_HAT.value());

            if (!(stack.getItem() instanceof VillagerHatItem relic))
                return;

            if (!relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("golem_guard").canPlayerUse(player))
                return;

            event.setNewAboutToBeSetTarget(null);
        }

        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.VILLAGER_HAT.value());

            if (!(stack.getItem() instanceof VillagerHatItem relic))
                return;

            if (!relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("golem_guard").canPlayerUse(player))
                return;

            if (relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("golem_guard").isRankModifierUnlocked("protection")) {
                var reductionPerGolem = Math.max(0D, Math.min(1D, relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("golem_guard").getStatData("damage_reduction_per_golem").getValue()));
                var radius = Math.max(0D, relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("golem_guard").getStatData("radius").getValue());
                var maxReduction = Math.max(0D, Math.min(1D, relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("golem_guard").getStatData("max_damage_reduction").getValue()));

                if (reductionPerGolem > 0D && radius > 0D && maxReduction > 0D) {
                    var golems = player.level().getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(radius), golem -> golem.isAlive()).size();
                    var reduction = Math.min(maxReduction, reductionPerGolem * golems);

                    if (reduction > 0D)
                        event.setAmount((float) Math.max(0D, event.getAmount() * (1D - reduction)));
                }
            }

            if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == player)
                return;

            for (var golem : player.level().getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(32D), golem -> golem.isAlive() && golem != attacker))
                golem.setTarget(attacker);
        }

        @SubscribeEvent
        public static void onTradeWithVillager(TradeWithVillagerEvent event) {
            var player = event.getEntity();

            if (player.level().isClientSide())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.VILLAGER_HAT.value());

            if (!(stack.getItem() instanceof VillagerHatItem relic))
                return;

            if (!relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("trade_surge").canPlayerUse(player)
                    || !relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("trade_surge").isRankModifierUnlocked("multicast"))
                return;

            var multicast = Math.max(0, (int) MathUtils.round(relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("trade_surge").getStatData("multicast").getValue(), 0));

            if (multicast <= 0)
                return;

            var casts = MathUtils.multicast(player.getRandom(), 0.5D, multicast);

            if (casts <= 0)
                return;

            var result = event.getMerchantOffer().getResult();

            if (result.isEmpty())
                return;

            for (var i = 0; i < casts; i++) {
                var bonus = result.copy();

                if (!player.addItem(bonus))
                    player.drop(bonus, false);
            }
        }
    }
}