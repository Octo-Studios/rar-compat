package it.hurts.shatterbyte.reliquified_artifacts.items.hands;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.init.RADataComponent;
import it.hurts.shatterbyte.reliquified_artifacts.items.base.RAWearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.AbilityMetricTemplate;
import it.hurts.sskirillss.relics.api.relics.AbilityStatisticTemplate;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourceTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourcesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import top.theillusivec4.curios.api.SlotContext;

public class DiggingClawsItem extends RAWearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("digging")
                                .rankModifier(1, "bare_hands")
                                .rankModifier(3, "momentum")
                                .rankModifier(5, "mastery")
                                .stat(AbilityStatTemplate.builder("speed")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1755D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("bare_hands_speed")
                                        .initialValue(0.35D, 0.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1143D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("momentum_per_block")
                                        .initialValue(0.01D, 0.05D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("momentum_window")
                                        .thresholdValue(0.1D, Double.MAX_VALUE)
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("mined_block").build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("mined_blocks")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.MINESHAFT, LootEntries.CAVE)
                        .build())
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        setStreakCount(stack, 0);
        setLastBreakTick(stack, -1L);
    }

    private int getStreakCount(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(RADataComponent.DIGGING_CLAWS_STREAK_COUNT.get(), 0));
    }

    private void setStreakCount(ItemStack stack, int count) {
        stack.set(RADataComponent.DIGGING_CLAWS_STREAK_COUNT.get(), Math.max(0, count));
    }

    private long getLastBreakTick(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.DIGGING_CLAWS_LAST_BREAK_TICK.get(), -1L);
    }

    private void setLastBreakTick(ItemStack stack, long tick) {
        stack.set(RADataComponent.DIGGING_CLAWS_LAST_BREAK_TICK.get(), Math.max(-1L, tick));
    }

    private static long secondsToTicks(double seconds) {
        return Math.max(0L, Math.round(Math.max(0D, seconds) * 20D));
    }

    private static int getToolTierLevel(ItemStack stack) {
        if (!(stack.getItem() instanceof TieredItem tiered))
            return -1;

        var tier = tiered.getTier();

        if (tier == Tiers.NETHERITE || tier == Tiers.DIAMOND)
            return 3;

        if (tier == Tiers.IRON)
            return 2;

        if (tier == Tiers.STONE)
            return 1;

        if (tier == Tiers.WOOD || tier == Tiers.GOLD)
            return 0;

        return -1;
    }

    private static int getRequiredTierLevel(BlockState state) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL))
            return 3;

        if (state.is(BlockTags.NEEDS_IRON_TOOL))
            return 2;

        if (state.is(BlockTags.NEEDS_STONE_TOOL))
            return 1;

        return 0;
    }

    private static boolean canHarvestWithTierBonus(ItemStack heldItem, BlockState state) {
        var toolLevel = 0;

        if (heldItem.has(DataComponents.TOOL)) {
            toolLevel = getToolTierLevel(heldItem);

            if (toolLevel < 0)
                return false;
        }

        return toolLevel + 1 >= getRequiredTierLevel(state);
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
            var player = event.getEntity();
            var relicStack = EntityUtils.findEquippedCurio(player, ModItems.DIGGING_CLAWS.value());

            if (!(relicStack.getItem() instanceof DiggingClawsItem relic))
                return;

            var ability = relic.getRelicData(player, relicStack).getAbilitiesData().getAbilityData("digging");

            if (!ability.canPlayerUse(player)) {
                relic.setStreakCount(relicStack, 0);
                relic.setLastBreakTick(relicStack, -1L);

                return;
            }

            var gameTime = player.level().getGameTime();
            var lastBreakTick = relic.getLastBreakTick(relicStack);
            var windowTicks = secondsToTicks(ability.getStatData("momentum_window").getValue());

            if (lastBreakTick < 0L || gameTime - lastBreakTick > windowTicks)
                relic.setStreakCount(relicStack, 0);

            var speedMultiplier = 1D + Math.max(0D, ability.getStatData("speed").getValue());

            if (ability.isRankModifierUnlocked("bare_hands")) {
                var heldItem = player.getMainHandItem();

                if (!heldItem.has(DataComponents.TOOL))
                    speedMultiplier += Math.max(0D, ability.getStatData("bare_hands_speed").getValue());
            }

            if (ability.isRankModifierUnlocked("momentum")) {
                var streak = relic.getStreakCount(relicStack);

                if (streak > 0)
                    speedMultiplier += Math.max(0D, ability.getStatData("momentum_per_block").getValue()) * streak;
            }

            event.setNewSpeed((float) Math.max(0D, event.getNewSpeed() * speedMultiplier));
        }

        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            var player = event.getPlayer();

            if (player.level().isClientSide() || event.isCanceled())
                return;

            if (event.getState().getDestroySpeed(player.level(), event.getPos()) <= 0F)
                return;

            var relicStack = EntityUtils.findEquippedCurio(player, ModItems.DIGGING_CLAWS.value());

            if (!(relicStack.getItem() instanceof DiggingClawsItem relic))
                return;

            var relicData = relic.getRelicData(player, relicStack);
            var ability = relicData.getAbilitiesData().getAbilityData("digging");

            if (!ability.canPlayerUse(player)) {
                relic.setStreakCount(relicStack, 0);
                relic.setLastBreakTick(relicStack, -1L);

                return;
            }

            var gameTime = player.level().getGameTime();
            var lastBreakTick = relic.getLastBreakTick(relicStack);
            var windowTicks = secondsToTicks(ability.getStatData("momentum_window").getValue());
            var streak = lastBreakTick >= 0L && gameTime - lastBreakTick <= windowTicks ? relic.getStreakCount(relicStack) + 1 : 1;

            relic.setStreakCount(relicStack, streak);
            relic.setLastBreakTick(relicStack, gameTime);

            ability.getStatisticData().getMetricData("mined_blocks").addValue(1D);

            if (player.getRandom().nextFloat() <= 0.1F)
                relicData.getLevelingData().addExperience("digging", "mined_block", 1D);
        }

        @SubscribeEvent
        public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
            if (event.canHarvest())
                return;

            var player = event.getEntity();
            var relicStack = EntityUtils.findEquippedCurio(player, ModItems.DIGGING_CLAWS.value());

            if (!(relicStack.getItem() instanceof DiggingClawsItem relic))
                return;

            var ability = relic.getRelicData(player, relicStack).getAbilitiesData().getAbilityData("digging");

            if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("mastery"))
                return;

            if (canHarvestWithTierBonus(player.getMainHandItem(), event.getTargetBlock()))
                event.setCanHarvest(true);
        }
    }
}
