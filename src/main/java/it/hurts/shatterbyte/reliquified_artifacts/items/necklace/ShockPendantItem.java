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
import it.hurts.sskirillss.relics.entities.ElectricSparkEntity;
import it.hurts.sskirillss.relics.init.RelicsEntities;
import it.hurts.sskirillss.relics.init.RelicsMobEffects;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class ShockPendantItem extends RAWearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("shock")
                                .rankModifier(1, "resistance")
                                .rankModifier(3, "conductor")
                                .rankModifier(5, "tremor")
                                .stat(AbilityStatTemplate.builder("chance")
                                        .initialValue(0.1D, 0.2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0429D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("distance")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("bounces")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0667D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("damage")
                                        .initialValue(1D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0857D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("damage_modifier")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0531D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("tremor_chance")
                                        .initialValue(0.05D, 0.1D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0429D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("tremor_duration")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.019D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("spark_created").build())
                                        .source(ExperienceSourceTemplate.builder("lightning_resist")
                                                .rankModifierVisibilityState("resistance", VisibilityState.OBFUSCATED)
                                                .build())
                                        .source(ExperienceSourceTemplate.builder("tremor_applied")
                                                .rankModifierVisibilityState("tremor", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("sparks_created")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("spark_targets_hit")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("spark_damage_dealt")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("lightning_resists")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("resistance", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("tremor_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("tremor", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.MOUNTAIN)
                        .build())
                .build();
    }

    private long getLastLightningExperienceTick(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.SHOCK_PENDANT_LAST_LIGHTNING_XP_TICK.get(), -20L);
    }

    private void setLastLightningExperienceTick(ItemStack stack, long gameTime) {
        stack.set(RADataComponent.SHOCK_PENDANT_LAST_LIGHTNING_XP_TICK.get(), gameTime);
    }

    private boolean canAwardLightningExperience(ItemStack stack, long gameTime) {
        return gameTime - getLastLightningExperienceTick(stack) >= 20L;
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            var entity = event.getEntity();

            if (entity.level().isClientSide() || event.getAmount() <= 0F)
                return;

            if (event.getSource().getDirectEntity() instanceof ElectricSparkEntity spark && spark.getOwner() instanceof LivingEntity owner) {
                var sparkStack = spark.getStack();

                if (!sparkStack.isEmpty() && sparkStack.getItem() instanceof ShockPendantItem relic) {
                    var ability = relic.getRelicData(owner, sparkStack).getAbilitiesData().getAbilityData("shock");

                    ability.getStatisticData().getMetricData("spark_targets_hit").addValue(1D);
                    ability.getStatisticData().getMetricData("spark_damage_dealt").addValue(event.getAmount());
                }
            }

            var attacker = event.getSource().getEntity() instanceof LivingEntity living && living != entity ? living : null;
            var lightningDamage = event.getSource().is(DamageTypes.LIGHTNING_BOLT);
            var gameTime = entity.level().getGameTime();
            var lightningImmune = false;
            ShockPendantItem lightningRelic = null;
            ItemStack lightningStack = ItemStack.EMPTY;
            var tremorChance = 0D;
            var tremorDuration = 0D;
            ShockPendantItem tremorRelic = null;
            ItemStack tremorStack = ItemStack.EMPTY;
            var tremorSourceChance = 0D;
            var tremorSourceDuration = 0D;

            for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.SHOCK_PENDANT.value())) {
                if (!(stack.getItem() instanceof ShockPendantItem relic))
                    continue;

                var relicData = relic.getRelicData(entity, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("shock");

                if (!ability.canPlayerUse(entity))
                    continue;

                if (lightningDamage && ability.getRankModifierData("resistance").isEnabled()) {
                    lightningImmune = true;

                    if (lightningRelic == null) {
                        lightningRelic = relic;
                        lightningStack = stack;
                    }
                }

                if (attacker != null) {
                    var chance = Math.max(0D, Math.min(1D, ability.getStatData("chance").getValue()));

                    if (chance > 0D && entity.getRandom().nextDouble() <= chance) {
                        var distance = (float) Math.max(0D, ability.getStatData("distance").getValue());
                        var bounces = Math.max(0, (int) MathUtils.round(ability.getStatData("bounces").getValue(), 0));
                        var damage = (float) Math.max(0D, ability.getStatData("damage").getValue());

                        if (distance > 0F && bounces > 0 && damage > 0F) {
                            var spark = new ElectricSparkEntity(RelicsEntities.ELECTRIC_SPARK.get(), entity.level());

                            if (ability.getRankModifierData("conductor").isEnabled())
                                spark.setDamageModifier((float) Math.max(0D, ability.getStatData("damage_modifier").getValue()));

                            spark.setDistance(distance);
                            spark.setBounces(bounces);
                            spark.setDamage(damage);
                            spark.setPos(entity.position().add(0D, entity.getBbHeight() / 2F, 0D));
                            spark.setFlawless(relicData.isFlawless());
                            spark.setTarget(attacker);
                            spark.setOwner(entity);
                            spark.setStack(stack);

                            entity.level().addFreshEntity(spark);

                            relicData.getLevelingData().addExperience("shock", "spark_created", 1D);
                            ability.getStatisticData().getMetricData("sparks_created").addValue(1D);
                        }
                    }
                }

                if (ability.getRankModifierData("tremor").isEnabled()) {
                    var currentChance = Math.max(0D, Math.min(1D, ability.getStatData("tremor_chance").getValue()));
                    var currentDuration = Math.max(0D, ability.getStatData("tremor_duration").getValue());

                    tremorChance = Math.max(tremorChance, currentChance);
                    tremorDuration = Math.max(tremorDuration, currentDuration);

                    if (currentChance > tremorSourceChance || currentChance == tremorSourceChance && currentDuration > tremorSourceDuration) {
                        tremorSourceChance = currentChance;
                        tremorSourceDuration = currentDuration;
                        tremorRelic = relic;
                        tremorStack = stack;
                    }
                }
            }

            if (lightningDamage && lightningImmune) {
                event.setAmount(0F);
                event.setCanceled(true);

                if (lightningRelic != null && lightningRelic.canAwardLightningExperience(lightningStack, gameTime)) {
                    var relicData = lightningRelic.getRelicData(entity, lightningStack);
                    var ability = relicData.getAbilitiesData().getAbilityData("shock");

                    relicData.getLevelingData().addExperience("shock", "lightning_resist", 1D);
                    ability.getStatisticData().getMetricData("lightning_resists").addValue(1D);
                    lightningRelic.setLastLightningExperienceTick(lightningStack, gameTime);
                }
            }

            if (attacker != null && tremorChance > 0D && tremorDuration > 0D && entity.getRandom().nextDouble() <= tremorChance) {
                var durationTicks = Math.max(1, (int) Math.round(tremorDuration * 20D));
                attacker.addEffect(new MobEffectInstance(RelicsMobEffects.TREMOR, durationTicks, 0, false, true));

                if (tremorRelic != null) {
                    var relicData = tremorRelic.getRelicData(entity, tremorStack);
                    var ability = relicData.getAbilitiesData().getAbilityData("shock");

                    relicData.getLevelingData().addExperience("shock", "tremor_applied", 1D);
                    ability.getStatisticData().getMetricData("tremor_duration").addValue(durationTicks / 20D);
                }
            }
        }
    }
}
