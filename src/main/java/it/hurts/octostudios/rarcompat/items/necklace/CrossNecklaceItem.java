package it.hurts.octostudios.rarcompat.items.necklace;

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
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class CrossNecklaceItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("protection")
                                .rankModifier(1, "holy_fire")
                                .rankModifier(3, "smite")
                                .rankModifier(5, "salvation")
                                .stat(AbilityStatTemplate.builder("invulnerability")
                                        .initialValue(0.05D, 0.1D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.2571D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("fire_duration")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1429D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("undead_damage")
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.2571D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("survival_chance")
                                        .initialValue(0.05D, 0.1D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0429D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("damage_taken").build())
                                        .source(ExperienceSourceTemplate.builder("holy_fire_ignite")
                                                .rankModifierVisibilityState("holy_fire", VisibilityState.OBFUSCATED)
                                                .build())
                                        .source(ExperienceSourceTemplate.builder("smite_hit")
                                                .rankModifierVisibilityState("smite", VisibilityState.OBFUSCATED)
                                                .build())
                                        .source(ExperienceSourceTemplate.builder("salvation_save")
                                                .rankModifierVisibilityState("salvation", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("invulnerability_time")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("holy_fire_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("holy_fire", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("smite_bonus_damage")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("smite", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("fatal_cancels")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("salvation", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.DESERT, LootEntries.VILLAGE)
                        .build())
                .build();
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
            var entity = event.getEntity();

            if (entity.level().isClientSide() || event.getNewDamage() <= 0F)
                return;

            var lethalThreshold = entity.getHealth() + entity.getAbsorptionAmount();

            if (event.getNewDamage() < lethalThreshold)
                return;

            var chance = 0D;
            CrossNecklaceItem salvationRelic = null;
            ItemStack salvationStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.CROSS_NECKLACE.value())) {
                if (!(stack.getItem() instanceof CrossNecklaceItem relic))
                    continue;

                var relicData = relic.getRelicData(entity, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("protection");

                if (!ability.canPlayerUse(entity) || !ability.isRankModifierUnlocked("salvation"))
                    continue;

                var value = Math.max(0D, Math.min(1D, ability.getStatData("survival_chance").getValue()));

                if (value > chance) {
                    chance = value;
                    salvationRelic = relic;
                    salvationStack = stack;
                }
            }

            if (chance <= 0D || entity.getRandom().nextDouble() > chance)
                return;

            var originalDamage = event.getNewDamage();
            var safeDamage = Math.max(0F, lethalThreshold - 1F);
            var newDamage = Math.min(originalDamage, safeDamage);
            var canceledDamage = Math.max(0F, originalDamage - newDamage);

            event.setNewDamage(newDamage);

            if (salvationRelic != null) {
                var relicData = salvationRelic.getRelicData(entity, salvationStack);
                var ability = relicData.getAbilitiesData().getAbilityData("protection");

                if (canceledDamage > 0F)
                    relicData.getLevelingData().addExperience("protection", "salvation_save", canceledDamage);
                ability.getStatisticData().getMetricData("fatal_cancels").addValue(1D);
            }
        }

        @SubscribeEvent
        public static void onLivingDamagePost(LivingDamageEvent.Post event) {
            var entity = event.getEntity();

            if (entity.level().isClientSide() || event.getNewDamage() <= 0F)
                return;

            var attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
            var bonusTicks = 0;
            var fireSeconds = 0F;
            CrossNecklaceItem holyFireRelic = null;
            ItemStack holyFireStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.CROSS_NECKLACE.value())) {
                if (!(stack.getItem() instanceof CrossNecklaceItem relic))
                    continue;

                var relicData = relic.getRelicData(entity, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("protection");

                if (!ability.canPlayerUse(entity))
                    continue;

                relicData.getLevelingData().addExperience("protection", "damage_taken", 1D);

                var invulnerability = Math.max(0, (int) MathUtils.round(ability.getStatData("invulnerability").getValue() / 20, 0));

                bonusTicks = Math.max(bonusTicks, invulnerability);

                if (invulnerability > 0)
                    ability.getStatisticData().getMetricData("invulnerability_time").addValue(invulnerability / 20D);

                if (attacker != null && attacker.isInvertedHealAndHarm() && ability.isRankModifierUnlocked("holy_fire")) {
                    var value = (float) Math.max(0D, ability.getStatData("fire_duration").getValue());

                    if (value > fireSeconds) {
                        fireSeconds = value;
                        holyFireRelic = relic;
                        holyFireStack = stack;
                    }
                }
            }

            if (bonusTicks > 0) {
                var baseTicks = Math.max(entity.invulnerableTime, event.getPostAttackInvulnerabilityTicks());

                entity.invulnerableTime = baseTicks + bonusTicks;
            }

            if (attacker != null && fireSeconds > 0F) {
                attacker.igniteForSeconds(fireSeconds);

                if (holyFireRelic != null) {
                    var relicData = holyFireRelic.getRelicData(entity, holyFireStack);
                    var ability = relicData.getAbilitiesData().getAbilityData("protection");

                    relicData.getLevelingData().addExperience("protection", "holy_fire_ignite", 1D);
                    ability.getStatisticData().getMetricData("holy_fire_duration").addValue(fireSeconds);
                }
            }
        }

        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof LivingEntity source) || source.level().isClientSide() || event.getEntity() == source)
                return;

            if (!event.getEntity().isInvertedHealAndHarm())
                return;

            var bonus = 0D;
            CrossNecklaceItem smiteRelic = null;
            ItemStack smiteStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(source, ModItems.CROSS_NECKLACE.value())) {
                if (!(stack.getItem() instanceof CrossNecklaceItem relic))
                    continue;

                var relicData = relic.getRelicData(source, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("protection");

                if (!ability.canPlayerUse(source) || !ability.isRankModifierUnlocked("smite"))
                    continue;

                relicData.getLevelingData().addExperience("protection", "smite_hit", 1D);

                var value = Math.max(0D, Math.min(1D, ability.getStatData("undead_damage").getValue()));

                if (value > bonus) {
                    bonus = value;
                    smiteRelic = relic;
                    smiteStack = stack;
                }
            }

            if (bonus <= 0D)
                return;

            var baseDamage = event.getAmount();
            var boostedDamage = (float) (baseDamage * (1D + bonus));
            var additionalDamage = Math.max(0F, boostedDamage - baseDamage);

            event.setAmount(boostedDamage);

            if (smiteRelic != null && additionalDamage > 0F) {
                var ability = smiteRelic.getRelicData(source, smiteStack).getAbilitiesData().getAbilityData("protection");
                ability.getStatisticData().getMetricData("smite_bonus_damage").addValue(additionalDamage);
            }
        }
    }
}