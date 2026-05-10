package it.hurts.shatterbyte.reliquified_artifacts.items.bracelet;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.init.RADataComponent;
import it.hurts.shatterbyte.reliquified_artifacts.items.base.RAWearableRelicItem;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import top.theillusivec4.curios.api.SlotContext;

public class OnionRingItem extends RAWearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("hunger_mining")
                                .rankModifier(1, "food_rush")
                                .rankModifier(3, "sustenance")
                                .rankModifier(5, "perfect_focus")
                                .stat(AbilityStatTemplate.builder("speed_per_hunger")
                                        .initialValue(0.01D, 0.05D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.10005D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("food_boost")
                                        .initialValue(0.1D, 0.25D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.99988D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("food_boost_duration")
                                        .initialValue(3D, 5D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 10.005D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("hunger_restore_chance")
                                        .initialValue(0.05D, 0.1D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.50005D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("mined_block").build())
                                        .source(ExperienceSourceTemplate.builder("sustenance_hunger")
                                                .rankModifierVisibilityState("sustenance", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("mined_blocks")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("sustenance_hunger_restored")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("sustenance", VisibilityState.OBFUSCATED)
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

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("hunger_mining");

        if (!ability.canPlayerUse(player)) {
            setFoodBoostTicks(stack, 0);
            return;
        }

        var ticks = getFoodBoostTicks(stack);

        if (ticks > 0)
            setFoodBoostTicks(stack, ticks - 1);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        setFoodBoostTicks(stack, 0);
    }

    private int getFoodBoostTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(RADataComponent.ONION_RING_FOOD_BOOST_TICKS.get(), 0));
    }

    private void setFoodBoostTicks(ItemStack stack, int ticks) {
        stack.set(RADataComponent.ONION_RING_FOOD_BOOST_TICKS.get(), Math.max(0, ticks));
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(0, (int) Math.round(Math.max(0D, seconds) * 20D));
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
            var player = event.getEntity();

            if (event.getNewSpeed() <= 0F)
                return;

            var hunger = Math.max(0, player.getFoodData().getFoodLevel());
            var speedPerHunger = 0D;
            var foodRushBonus = 0D;
            var instantBreak = false;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.ONION_RING.value())) {
                if (!(stack.getItem() instanceof OnionRingItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("hunger_mining");

                if (!ability.canPlayerUse(player)) {
                    relic.setFoodBoostTicks(stack, 0);
                    continue;
                }

                speedPerHunger = Math.max(speedPerHunger, Math.max(0D, ability.getStatData("speed_per_hunger").getValue()));

                if (ability.getRankModifierData("food_rush").isEnabled() && relic.getFoodBoostTicks(stack) > 0)
                    foodRushBonus = Math.max(foodRushBonus, Math.max(0D, ability.getStatData("food_boost").getValue()));

                if (ability.getRankModifierData("perfect_focus").isEnabled() && hunger >= 20)
                    instantBreak = true;
            }

            var multiplier = 1D + speedPerHunger * hunger + foodRushBonus;

            if (multiplier > 1D)
                event.setNewSpeed((float) Math.max(0D, event.getNewSpeed() * multiplier));

            if (!instantBreak)
                return;

            var pos = event.getPosition().orElse(null);

            if (pos == null)
                return;

            if (event.getState().getDestroySpeed(player.level(), pos) < 0F)
                return;

            event.setNewSpeed(Float.MAX_VALUE);
        }

        @SubscribeEvent
        public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide())
                return;

            if (event.getItem().getUseAnimation() != UseAnim.EAT)
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.ONION_RING.value())) {
                if (!(stack.getItem() instanceof OnionRingItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("hunger_mining");

                if (!ability.canPlayerUse(player) || !ability.getRankModifierData("food_rush").isEnabled()) {
                    relic.setFoodBoostTicks(stack, 0);
                    continue;
                }

                var durationTicks = secondsToTicks(ability.getStatData("food_boost_duration").getValue());

                if (durationTicks <= 0)
                    continue;

                relic.setFoodBoostTicks(stack, Math.max(relic.getFoodBoostTicks(stack), durationTicks));
            }
        }

        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            var player = event.getPlayer();

            if (player.level().isClientSide() || event.isCanceled())
                return;

            var instantBreakBlock = event.getState().getDestroySpeed(player.level(), event.getPos()) <= 0F;
            var minedRelic = (OnionRingItem) null;
            var minedStack = ItemStack.EMPTY;
            var bestMiningWeight = -1D;
            var chance = 0D;
            var sustenanceRelic = (OnionRingItem) null;
            var sustenanceStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.ONION_RING.value())) {
                if (!(stack.getItem() instanceof OnionRingItem relic))
                    continue;

                var relicData = relic.getRelicData(player, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("hunger_mining");

                if (!ability.canPlayerUse(player))
                    continue;

                var miningWeight = Math.max(0D, ability.getStatData("speed_per_hunger").getValue());

                if (minedRelic == null || miningWeight > bestMiningWeight) {
                    minedRelic = relic;
                    minedStack = stack;
                    bestMiningWeight = miningWeight;
                }

                if (!ability.getRankModifierData("sustenance").isEnabled())
                    continue;

                var value = Math.max(0D, Math.min(1D, ability.getStatData("hunger_restore_chance").getValue()));

                if (sustenanceRelic == null || value > chance) {
                    chance = value;
                    sustenanceRelic = relic;
                    sustenanceStack = stack;
                }
            }

            if (!instantBreakBlock && minedRelic != null) {
                var relicData = minedRelic.getRelicData(player, minedStack);
                var ability = relicData.getAbilitiesData().getAbilityData("hunger_mining");

                ability.getStatisticData().getMetricData("mined_blocks").addValue(1D);

                if (player.getRandom().nextFloat() <= 0.1F)
                    relicData.getLevelingData().addExperience("hunger_mining", "mined_block", 1D);
            }

            if (chance <= 0D || sustenanceRelic == null || player.getRandom().nextDouble() > chance)
                return;

            var foodData = player.getFoodData();
            var beforeHunger = foodData.getFoodLevel();

            foodData.eat(1, 0F);

            var restoredHunger = Math.max(0, foodData.getFoodLevel() - beforeHunger);

            if (restoredHunger <= 0)
                return;

            var relicData = sustenanceRelic.getRelicData(player, sustenanceStack);

            relicData.getLevelingData().addExperience("hunger_mining", "sustenance_hunger", restoredHunger);
            relicData.getAbilitiesData().getAbilityData("hunger_mining").getStatisticData().getMetricData("sustenance_hunger_restored").addValue(restoredHunger);
        }
    }
}

