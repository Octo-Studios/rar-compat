package it.hurts.octostudios.rarcompat.items.hat;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.AbilityMetricTemplate;
import it.hurts.sskirillss.relics.api.relics.AbilityStatisticTemplate;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.VisibilityState;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourceTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourcesTemplate;
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
                        .ability(AbilityTemplate.builder("trade_surge")
                                .rankModifier(3, "preserve")
                                .rankModifier(5, "multicast")
                                .stat(AbilityStatTemplate.builder("discount")
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("multicast")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("preserve_chance")
                                        .initialValue(0.1D, 0.3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("trade").build())
                                        .source(ExperienceSourceTemplate.builder("multicast_bonus")
                                                .rankModifierVisibilityState("multicast", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("trades_done")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("trades_preserved")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("preserve", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("multicast_bonus_results")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("multicast", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .ability(AbilityTemplate.builder("golem_guard")
                                .requiredLevel(5)
                                .rankModifier(1, "protection")
                                .stat(AbilityStatTemplate.builder("damage_reduction_per_golem")
                                        .initialValue(0.05D, 0.15D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.019D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("radius")
                                        .initialValue(5D, 10D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1143D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("max_damage_reduction")
                                        .initialValue(0.15D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0571D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("golem_damage_reduced")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("protection", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.VILLAGE)
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

    public static void onTradePreserved(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof VillagerHatItem relic) || player.level().isClientSide())
            return;

        var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("trade_surge");

        if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("preserve"))
            return;

        ability.getStatisticData().getMetricData("trades_preserved").addValue(1D);
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

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("golem_guard");

            if (!ability.canPlayerUse(player))
                return;

            if (ability.isRankModifierUnlocked("protection")) {
                var reductionPerGolem = Math.max(0D, Math.min(1D, ability.getStatData("damage_reduction_per_golem").getValue()));
                var radius = Math.max(0D, ability.getStatData("radius").getValue());
                var maxReduction = Math.max(0D, Math.min(1D, ability.getStatData("max_damage_reduction").getValue()));

                if (reductionPerGolem > 0D && radius > 0D && maxReduction > 0D) {
                    var golems = player.level().getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(radius), golem -> golem.isAlive()).size();
                    var reduction = Math.min(maxReduction, reductionPerGolem * golems);

                    if (reduction > 0D) {
                        var previous = event.getAmount();
                        var reduced = (float) Math.max(0D, previous * (1D - reduction));

                        event.setAmount(reduced);

                        var prevented = Math.max(0F, previous - reduced);

                        if (prevented > 0F)
                            ability.getStatisticData().getMetricData("golem_damage_reduced").addValue(prevented);
                    }
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

            var relicData = relic.getRelicData(player, stack);
            var ability = relicData.getAbilitiesData().getAbilityData("trade_surge");

            if (!ability.canPlayerUse(player))
                return;

            ability.getStatisticData().getMetricData("trades_done").addValue(1D);
            relicData.getLevelingData().addExperience("trade_surge", "trade", 1D);

            if (!ability.isRankModifierUnlocked("multicast"))
                return;

            var multicast = Math.max(0, (int) MathUtils.round(ability.getStatData("multicast").getValue(), 0));

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

            ability.getStatisticData().getMetricData("multicast_bonus_results").addValue(casts);
            relicData.getLevelingData().addExperience("trade_surge", "multicast_bonus", casts);
        }
    }
}
