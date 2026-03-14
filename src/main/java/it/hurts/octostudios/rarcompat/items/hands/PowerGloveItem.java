package it.hurts.octostudios.rarcompat.items.hands;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import top.theillusivec4.curios.api.SlotContext;

public class PowerGloveItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("power")
                                .rankModifier(1, "shield_break")
                                .rankModifier(3, "armor_pierce")
                                .rankModifier(5, "echo_strike")
                                .stat(AbilityStatTemplate.builder("hits_required")
                                        .initialValue(7D, 10D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.0229D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("power_damage_bonus")
                                        .initialValue(1D, 1.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0667D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("armor_ignore")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0531D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("power_strike").build())
                                        .source(ExperienceSourceTemplate.builder("echo_kill")
                                                .rankModifierVisibilityState("echo_strike", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("power_strikes")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("bonus_damage")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("shields_broken")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("shield_break", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("power_strike_kills")
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

        clearState(stack);
    }

    private int getHitCounter(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.POWER_GLOVE_HIT_COUNTER.get(), 0));
    }

    private void setHitCounter(ItemStack stack, int value) {
        stack.set(DataComponentRegistry.POWER_GLOVE_HIT_COUNTER.get(), Math.max(0, value));
    }

    private boolean getForceNext(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.POWER_GLOVE_FORCE_NEXT.get(), false);
    }

    private void setForceNext(ItemStack stack, boolean value) {
        stack.set(DataComponentRegistry.POWER_GLOVE_FORCE_NEXT.get(), value);
    }

    private boolean getPowerStrikeActive(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.POWER_GLOVE_POWER_STRIKE_ACTIVE.get(), false);
    }

    private void setPowerStrikeActive(ItemStack stack, boolean value) {
        stack.set(DataComponentRegistry.POWER_GLOVE_POWER_STRIKE_ACTIVE.get(), value);
    }

    private void clearState(ItemStack stack) {
        setHitCounter(stack, 0);
        setForceNext(stack, false);
        setPowerStrikeActive(stack, false);
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            var fullyCharged = player.getAttackStrengthScale(0.5F) >= 1F;
            var damageBonus = 0D;
            var armorIgnore = 0D;
            var hasPowerStrike = false;
            PowerGloveItem bestRelic = null;
            ItemStack bestStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.POWER_GLOVE.value())) {
                if (!(stack.getItem() instanceof PowerGloveItem relic))
                    continue;

                relic.setPowerStrikeActive(stack, false);

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("power");

                if (!ability.canPlayerUse(player)) {
                    relic.clearState(stack);
                    continue;
                }

                if (!fullyCharged)
                    continue;

                var hitsRequired = Math.max(1, (int) MathUtils.round(ability.getStatData("hits_required").getValue(), 0));
                var powerStrike = relic.getForceNext(stack);

                if (powerStrike) {
                    relic.setForceNext(stack, false);
                    relic.setHitCounter(stack, 0);
                } else {
                    var counter = relic.getHitCounter(stack) + 1;

                    if (counter >= hitsRequired) {
                        powerStrike = true;
                        counter = 0;
                    }

                    relic.setHitCounter(stack, counter);
                }

                if (!powerStrike)
                    continue;

                hasPowerStrike = true;
                relic.setPowerStrikeActive(stack, true);

                var localDamageBonus = Math.max(0D, ability.getStatData("power_damage_bonus").getValue());

                if (bestRelic == null || localDamageBonus > damageBonus) {
                    bestRelic = relic;
                    bestStack = stack;
                    damageBonus = localDamageBonus;
                }

                if (ability.isRankModifierUnlocked("armor_pierce")) {
                    var value = Math.max(0D, Math.min(1D, ability.getStatData("armor_ignore").getValue()));

                    armorIgnore = Math.max(armorIgnore, value);
                }
            }

            if (!hasPowerStrike)
                return;

            var baseDamage = event.getAmount();
            var boostedDamage = baseDamage;

            if (damageBonus > 0D) {
                boostedDamage = (float) (baseDamage * (1D + damageBonus));
                event.setAmount((float) boostedDamage);
            }

            if (bestRelic != null) {
                var relicData = bestRelic.getRelicData(player, bestStack);
                var ability = relicData.getAbilitiesData().getAbilityData("power");

                relicData.getLevelingData().addExperience("power", "power_strike", 1D);
                ability.getStatisticData().getMetricData("power_strikes").addValue(1D);

                var additionalDamage = Math.max(0F, (float) boostedDamage - baseDamage);

                if (additionalDamage > 0F)
                    ability.getStatisticData().getMetricData("bonus_damage").addValue(additionalDamage);
            }

            if (armorIgnore > 0D) {
                var armorIgnoreFinal = armorIgnore;

                event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) ->
                        Math.max(0F, (float) (reduction * (1D - armorIgnoreFinal))));
            }
        }

        @SubscribeEvent
        public static void onLivingShieldBlock(LivingShieldBlockEvent event) {
            if (!event.getBlocked())
                return;

            if (!(event.getDamageSource().getEntity() instanceof Player player) || event.getDamageSource().getDirectEntity() != player || player.level().isClientSide())
                return;

            if (!(event.getEntity() instanceof Player target))
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.POWER_GLOVE.value())) {
                if (!(stack.getItem() instanceof PowerGloveItem relic) || !relic.getPowerStrikeActive(stack))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("power");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("shield_break"))
                    continue;

                target.disableShield();
                ability.getStatisticData().getMetricData("shields_broken").addValue(1D);
                break;
            }
        }

        @SubscribeEvent
        public static void onLivingDamagePost(LivingDamageEvent.Post event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getNewDamage() <= 0F)
                return;

            var killed = !event.getEntity().isAlive() || event.getEntity().getHealth() <= 0F;
            var killStatRecorded = false;
            var killExpRecorded = false;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.POWER_GLOVE.value())) {
                if (!(stack.getItem() instanceof PowerGloveItem relic) || !relic.getPowerStrikeActive(stack))
                    continue;

                var relicData = relic.getRelicData(player, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("power");

                if (killed && ability.canPlayerUse(player)) {
                    if (!killStatRecorded) {
                        ability.getStatisticData().getMetricData("power_strike_kills").addValue(1D);
                        killStatRecorded = true;
                    }

                    if (ability.isRankModifierUnlocked("echo_strike")) {
                        relic.setForceNext(stack, true);

                        if (!killExpRecorded) {
                            relicData.getLevelingData().addExperience("power", "echo_kill", 1D);
                            killExpRecorded = true;
                        }
                    }
                }

                relic.setPowerStrikeActive(stack, false);
            }
        }
    }
}