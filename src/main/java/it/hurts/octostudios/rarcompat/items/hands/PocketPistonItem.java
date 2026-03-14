package it.hurts.octostudios.rarcompat.items.hands;

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
import it.hurts.sskirillss.relics.init.RelicsMobEffects;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import top.theillusivec4.curios.api.SlotContext;

public class PocketPistonItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("piston")
                                .rankModifier(1, "repulse")
                                .rankModifier(3, "distance_power")
                                .rankModifier(5, "long_reach_stun")
                                .stat(AbilityStatTemplate.builder("range_bonus")
                                        .initialValue(0.25D, 0.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1143D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("empty_hand_knockback")
                                        .initialValue(0.01D, 0.025D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.2571D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("distance_bonus_per_block")
                                        .initialValue(0.01D, 0.025D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0857D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("stun_duration")
                                        .initialValue(1D, 2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0429D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("extended_hit").build())
                                        .source(ExperienceSourceTemplate.builder("extended_break").build())
                                        .source(ExperienceSourceTemplate.builder("extended_place").build())
                                        .source(ExperienceSourceTemplate.builder("long_reach_stun_hit")
                                                .rankModifierVisibilityState("long_reach_stun", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("extended_breaks")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("extended_damage_dealt")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("extended_places")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("distance_power_bonus_damage")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("distance_power", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("stunned_targets")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("long_reach_stun", VisibilityState.OBFUSCATED)
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

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("piston");

        if (!ability.canPlayerUse(player)) {
            removeRangeBonuses(player, stack);
            return;
        }

        var rangeBonus = Math.max(0D, ability.getStatData("range_bonus").getValue());

        if (rangeBonus <= 0D)
            removeRangeBonuses(player, stack);
        else
            applyRangeBonuses(player, stack, rangeBonus);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (!(slotContext.entity() instanceof Player player) || stack.getItem() == newStack.getItem())
            return;

        removeRangeBonuses(player, stack);
    }

    private void applyRangeBonuses(Player player, ItemStack stack, double amount) {
        if (player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE) != null)
            EntityUtils.resetAttribute(player, stack, Attributes.ENTITY_INTERACTION_RANGE, (float) amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        if (player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE) != null)
            EntityUtils.resetAttribute(player, stack, Attributes.BLOCK_INTERACTION_RANGE, (float) amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private void removeRangeBonuses(Player player, ItemStack stack) {
        if (player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE) != null)
            EntityUtils.removeAttribute(player, stack, Attributes.ENTITY_INTERACTION_RANGE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        if (player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE) != null)
            EntityUtils.removeAttribute(player, stack, Attributes.BLOCK_INTERACTION_RANGE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static double distanceToBlock(Player player, BlockPos pos) {
        return player.getEyePosition().distanceTo(pos.getCenter());
    }

    private static double getStandardRange(double currentRange, double rangeBonus) {
        var bonus = Math.max(0D, rangeBonus);

        if (bonus <= 0D || currentRange <= 0D)
            return currentRange;

        return currentRange / (1D + bonus);
    }

    private static boolean isBeyondPistonBonusRange(double distance, double currentRange, double rangeBonus) {
        if (distance <= 0D || currentRange <= 0D || rangeBonus <= 0D)
            return false;

        return distance > getStandardRange(currentRange, rangeBonus) + 1.0E-3D;
    }

    private record RangeContext(PocketPistonItem relic, ItemStack stack, double rangeBonus) {
    }

    private static RangeContext findBestRangeContext(Player player) {
        PocketPistonItem bestRelic = null;
        ItemStack bestStack = ItemStack.EMPTY;
        var bestRangeBonus = 0D;

        for (var stack : EntityUtils.findEquippedCurios(player, ModItems.POCKET_PISTON.value())) {
            if (!(stack.getItem() instanceof PocketPistonItem relic))
                continue;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("piston");

            if (!ability.canPlayerUse(player))
                continue;

            var rangeBonus = Math.max(0D, ability.getStatData("range_bonus").getValue());

            if (rangeBonus <= bestRangeBonus)
                continue;

            bestRangeBonus = rangeBonus;
            bestRelic = relic;
            bestStack = stack;
        }

        if (bestRelic == null || bestRangeBonus <= 0D)
            return null;

        return new RangeContext(bestRelic, bestStack, bestRangeBonus);
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            var distance = player.distanceTo(event.getEntity());
            var distanceBonusPerBlock = 0D;
            var emptyHandKnockback = 0D;
            var stunTicks = 0;
            var canApplyLongReachStun = false;
            PocketPistonItem distancePowerRelic = null;
            ItemStack distancePowerStack = ItemStack.EMPTY;
            PocketPistonItem stunRelic = null;
            ItemStack stunStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.POCKET_PISTON.value())) {
                if (!(stack.getItem() instanceof PocketPistonItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("piston");

                if (!ability.canPlayerUse(player))
                    continue;

                if (ability.isRankModifierUnlocked("repulse") && player.getMainHandItem().isEmpty())
                    emptyHandKnockback = Math.max(emptyHandKnockback, Math.max(0D, ability.getStatData("empty_hand_knockback").getValue()));

                if (ability.isRankModifierUnlocked("distance_power")) {
                    var localBonus = Math.max(0D, Math.min(1D, ability.getStatData("distance_bonus_per_block").getValue()));

                    if (localBonus > distanceBonusPerBlock) {
                        distanceBonusPerBlock = localBonus;
                        distancePowerRelic = relic;
                        distancePowerStack = stack;
                    }
                }

                if (ability.isRankModifierUnlocked("long_reach_stun")) {
                    var ticks = Math.max(0, (int) Math.round(Math.max(0D, ability.getStatData("stun_duration").getValue()) * 20D));

                    if (ticks > stunTicks) {
                        stunTicks = ticks;
                        stunRelic = relic;
                        stunStack = stack;
                    }

                    canApplyLongReachStun = true;
                }
            }

            if (distanceBonusPerBlock > 0D) {
                var baseDamage = event.getAmount();
                var boostedDamage = (float) (baseDamage * (1D + distanceBonusPerBlock * distance));
                var extraDamage = Math.max(0F, boostedDamage - baseDamage);

                event.setAmount(boostedDamage);

                if (distancePowerRelic != null && extraDamage > 0F) {
                    var ability = distancePowerRelic.getRelicData(player, distancePowerStack).getAbilitiesData().getAbilityData("piston");
                    ability.getStatisticData().getMetricData("distance_power_bonus_damage").addValue(extraDamage);
                }
            }

            if (emptyHandKnockback > 0D && event.getEntity() instanceof LivingEntity target)
                target.knockback(emptyHandKnockback, player.getX() - target.getX(), player.getZ() - target.getZ());

            if (!canApplyLongReachStun || stunTicks <= 0 || !(event.getEntity() instanceof LivingEntity target))
                return;

            var maxRange = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);

            if (maxRange > 0D && distance >= maxRange * 0.9D && target.addEffect(new MobEffectInstance(RelicsMobEffects.STUN, stunTicks, 0, false, true)) && stunRelic != null) {
                var relicData = stunRelic.getRelicData(player, stunStack);
                var ability = relicData.getAbilitiesData().getAbilityData("piston");

                relicData.getLevelingData().addExperience("piston", "long_reach_stun_hit", 1D);
                ability.getStatisticData().getMetricData("stunned_targets").addValue(1D);
            }
        }

        @SubscribeEvent
        public static void onLivingDamagePost(LivingDamageEvent.Post event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getNewDamage() <= 0F)
                return;

            var context = findBestRangeContext(player);

            if (context == null)
                return;

            var currentRange = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
            var distance = player.distanceTo(event.getEntity());

            if (!isBeyondPistonBonusRange(distance, currentRange, context.rangeBonus()))
                return;

            var relicData = context.relic().getRelicData(player, context.stack());
            var ability = relicData.getAbilitiesData().getAbilityData("piston");

            relicData.getLevelingData().addExperience("piston", "extended_hit", 1D);
            ability.getStatisticData().getMetricData("extended_damage_dealt").addValue(event.getNewDamage());
        }

        @SubscribeEvent
        public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
            var player = event.getEntity();

            if (player.level().isClientSide() || event.getNewSpeed() <= 0F)
                return;

            var pos = event.getPosition().orElse(null);

            if (pos == null)
                return;

            var distance = distanceToBlock(player, pos);
            var bonusPerBlock = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.POCKET_PISTON.value())) {
                if (!(stack.getItem() instanceof PocketPistonItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("piston");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("distance_power"))
                    continue;

                bonusPerBlock = Math.max(bonusPerBlock, Math.max(0D, Math.min(1D, ability.getStatData("distance_bonus_per_block").getValue())));
            }

            if (bonusPerBlock > 0D)
                event.setNewSpeed((float) Math.max(0D, event.getNewSpeed() * (1D + bonusPerBlock * distance)));
        }

        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            var player = event.getPlayer();

            if (player.level().isClientSide() || event.isCanceled())
                return;

            var context = findBestRangeContext(player);

            if (context == null)
                return;

            var currentRange = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
            var distance = distanceToBlock(player, event.getPos());

            if (!isBeyondPistonBonusRange(distance, currentRange, context.rangeBonus()))
                return;

            var relicData = context.relic().getRelicData(player, context.stack());
            var ability = relicData.getAbilitiesData().getAbilityData("piston");

            relicData.getLevelingData().addExperience("piston", "extended_break", 1D);
            ability.getStatisticData().getMetricData("extended_breaks").addValue(1D);
        }

        @SubscribeEvent
        public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
            if (event.isCanceled() || !(event.getEntity() instanceof Player player) || player.level().isClientSide())
                return;

            var context = findBestRangeContext(player);

            if (context == null)
                return;

            var currentRange = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
            var distance = distanceToBlock(player, event.getPos());

            if (!isBeyondPistonBonusRange(distance, currentRange, context.rangeBonus()))
                return;

            var relicData = context.relic().getRelicData(player, context.stack());
            var ability = relicData.getAbilitiesData().getAbilityData("piston");

            relicData.getLevelingData().addExperience("piston", "extended_place", 1D);
            ability.getStatisticData().getMetricData("extended_places").addValue(1D);
        }
    }
}
