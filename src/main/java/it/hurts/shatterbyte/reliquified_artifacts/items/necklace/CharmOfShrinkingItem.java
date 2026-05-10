package it.hurts.shatterbyte.reliquified_artifacts.items.necklace;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class CharmOfShrinkingItem extends RAWearableRelicItem {
    private static final ResourceLocation SCALE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(ReliquifiedArtifacts.MODID, "charm_of_shrinking_scale");

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("size")
                                .modes("stabilize", "shrink", "grow")
                                .rankModifier(1, "fall_resistance")
                                .rankModifier(3, "target_escape")
                                .rankModifier(5, "evasion")
                                .stat(AbilityStatTemplate.builder("min_scale")
                                        .initialValue(1D, 0.75D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.25125D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat(AbilityStatTemplate.builder("change_per_second")
                                        .initialValue(0.025D, 0.05D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.25003D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat(AbilityStatTemplate.builder("fall_reduction_per_size")
                                        .initialValue(0.15D, 0.35D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 1.00048D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("target_loss_chance_per_size")
                                        .initialValue(0.015D, 0.25D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.99988D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("target_loss_radius")
                                        .initialValue(3D, 5D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 14.9925D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("evasion_per_size")
                                        .initialValue(0.1D, 0.25D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.50025D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("moving_shrink").build())
                                        .source(ExperienceSourceTemplate.builder("fall_reduced")
                                                .rankModifierVisibilityState("fall_resistance", VisibilityState.OBFUSCATED)
                                                .build())
                                        .source(ExperienceSourceTemplate.builder("evasion_parry")
                                                .rankModifierVisibilityState("evasion", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("shrink_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("fall_damage_reduced")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("fall_resistance", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("evasion_misses")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("evasion", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("evasion_damage_avoided")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("evasion", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.THE_END, LootEntries.END_LIKE)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("size");
        var currentScale = getCurrentScale(stack);

        if (!ability.canPlayerUse(player)) {
            setMovingTicks(stack, 0);
            applyScaleModifier(player, currentScale);
            return;
        }

        var changePerSecond = Math.max(0D, ability.getStatData("change_per_second").getValue());
        var stepPerTick = changePerSecond / 20D;
        var targetScale = currentScale;

        switch (ability.getMode()) {
            case "shrink" -> targetScale = clampScale(Math.min(1D, ability.getStatData("min_scale").getValue()));
            case "grow" -> targetScale = 1D;
            default -> {
            }
        }

        var newScale = stepPerTick > 0D ? moveTowards(currentScale, targetScale, stepPerTick) : currentScale;

        newScale = clampScale(newScale);

        setCurrentScale(stack, newScale);
        applyScaleModifier(player, newScale);

        if (newScale < 1D && ability.getMode().equals("shrink") && player.tickCount % 20 == 0)
            ability.getStatisticData().getMetricData("shrink_duration").addValue(1D);

        var horizontalSpeedSqr = Math.max(
                player.getKnownMovement().horizontalDistanceSqr(),
                player.getDeltaMovement().horizontalDistanceSqr()
        );

        if (newScale < 1D && horizontalSpeedSqr > 1.0E-6D) {
            var movingTicks = getMovingTicks(stack) + 1;

            if (movingTicks >= 100) {
                var cycles = movingTicks / 100;
                var shrinkDelta = Math.max(0D, 1D - newScale);

                if (shrinkDelta > 0D)
                    relicData.getLevelingData().addExperience("size", "moving_shrink", cycles * shrinkDelta);

                movingTicks %= 100;
            }

            setMovingTicks(stack, movingTicks);
        }

        if (!ability.getRankModifierData("target_escape").isEnabled() || newScale >= 1D || player.tickCount % 20 != 0)
            return;

        var chancePerSize = Math.max(0D, ability.getStatData("target_loss_chance_per_size").getValue());
        var chance = Math.max(0D, Math.min(1D, (1D - newScale) * chancePerSize));
        var radius = Math.max(0D, ability.getStatData("target_loss_radius").getValue());

        if (chance <= 0D || radius <= 0D)
            return;

        var maxDistanceSq = radius * radius;

        for (var mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius), entity -> entity.isAlive() && entity.getTarget() == player)) {
            if (mob.distanceToSqr(player) > maxDistanceSq || player.getRandom().nextDouble() > chance)
                continue;

            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        if (slotContext.entity() instanceof Player player && !player.level().isClientSide())
            removeScaleModifier(player);

        setCurrentScale(stack, 1D);
        setMovingTicks(stack, 0);
    }

    private void applyScaleModifier(Player player, double scale) {
        var attribute = player.getAttribute(Attributes.SCALE);

        if (attribute == null)
            return;

        var amount = clampScale(scale) - 1D;

        attribute.addOrUpdateTransientModifier(new AttributeModifier(SCALE_MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private void removeScaleModifier(Player player) {
        var attribute = player.getAttribute(Attributes.SCALE);

        if (attribute == null)
            return;

        attribute.removeModifier(new AttributeModifier(SCALE_MODIFIER_ID, 0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private double getCurrentScale(ItemStack stack) {
        return clampScale(stack.getOrDefault(RADataComponent.CHARM_OF_SHRINKING_CURRENT_SCALE.get(), 1D));
    }

    private void setCurrentScale(ItemStack stack, double scale) {
        stack.set(RADataComponent.CHARM_OF_SHRINKING_CURRENT_SCALE.get(), clampScale(scale));
    }

    private int getMovingTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(RADataComponent.CHARM_OF_SHRINKING_MOVING_TICKS.get(), 0));
    }

    private void setMovingTicks(ItemStack stack, int ticks) {
        stack.set(RADataComponent.CHARM_OF_SHRINKING_MOVING_TICKS.get(), Math.max(0, ticks));
    }

    private static double moveTowards(double current, double target, double maxDelta) {
        if (maxDelta <= 0D)
            return current;

        if (current < target)
            return Math.min(target, current + maxDelta);

        if (current > target)
            return Math.max(target, current - maxDelta);

        return current;
    }

    private static double clampScale(double scale) {
        return Math.max(0.125D, Math.min(10D, scale));
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            var isFallDamage = event.getSource().is(DamageTypes.FALL);
            var fallReduction = 0D;
            CharmOfShrinkingItem fallRelic = null;
            ItemStack fallStack = ItemStack.EMPTY;
            var evasionChance = 0D;
            CharmOfShrinkingItem evasionRelic = null;
            ItemStack evasionStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.CHARM_OF_SHRINKING.value())) {
                if (!(stack.getItem() instanceof CharmOfShrinkingItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("size");

                if (!ability.canPlayerUse(player))
                    continue;

                var scale = relic.getCurrentScale(stack);

                if (scale >= 1D)
                    continue;

                var shrinkDelta = 1D - scale;

                if (isFallDamage && ability.getRankModifierData("fall_resistance").isEnabled()) {
                    var perSize = Math.max(0D, ability.getStatData("fall_reduction_per_size").getValue());
                    var reduction = Math.max(0D, Math.min(1D, shrinkDelta * perSize));

                    if (reduction > fallReduction) {
                        fallReduction = reduction;
                        fallRelic = relic;
                        fallStack = stack;
                    }
                }

                if (ability.getRankModifierData("evasion").isEnabled()) {
                    var perSize = Math.max(0D, ability.getStatData("evasion_per_size").getValue());
                    var chance = Math.max(0D, Math.min(1D, shrinkDelta * perSize));

                    if (chance > evasionChance) {
                        evasionChance = chance;
                        evasionRelic = relic;
                        evasionStack = stack;
                    }
                }
            }

            if (isFallDamage && fallReduction > 0D) {
                var baseDamage = event.getAmount();
                var reducedDamage = (float) Math.max(0D, baseDamage * (1D - fallReduction));
                var reduced = Math.max(0F, baseDamage - reducedDamage);

                event.setAmount(reducedDamage);

                if (fallRelic != null && reduced > 0F) {
                    var relicData = fallRelic.getRelicData(player, fallStack);
                    var ability = relicData.getAbilitiesData().getAbilityData("size");

                    relicData.getLevelingData().addExperience("size", "fall_reduced", reduced);
                    ability.getStatisticData().getMetricData("fall_damage_reduced").addValue(reduced);
                }
            }

            if (evasionChance <= 0D || player.getRandom().nextDouble() > evasionChance)
                return;

            var avoidedDamage = Math.max(0F, event.getAmount());

            event.setAmount(0F);
            event.setCanceled(true);

            if (evasionRelic != null) {
                var relicData = evasionRelic.getRelicData(player, evasionStack);
                var ability = relicData.getAbilitiesData().getAbilityData("size");

                relicData.getLevelingData().addExperience("size", "evasion_parry", 1D);
                ability.getStatisticData().getMetricData("evasion_misses").addValue(1D);
                ability.getStatisticData().getMetricData("evasion_damage_avoided").addValue(avoidedDamage);
            }
        }
    }
}
