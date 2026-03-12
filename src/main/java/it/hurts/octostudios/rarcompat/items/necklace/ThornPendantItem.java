package it.hurts.octostudios.rarcompat.items.necklace;

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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class ThornPendantItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("poison")
                                .rankModifier(1, "poison")
                                .rankModifier(3, "damage")
                                .rankModifier(5, "resistance")
                                .stat(AbilityStatTemplate.builder("reflect_damage")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("poison_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("poison_level")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("poisoned_damage_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("reflect_proc").build())
                                        .source(ExperienceSourceTemplate.builder("poisoned_hit").build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("procs")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("poison_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("poison", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("poisoned_bonus_damage")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("damage", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private static boolean isPoisonDamage(LivingIncomingDamageEvent event, LivingEntity entity) {
        if (event.getSource().is(NeoForgeMod.POISON_DAMAGE))
            return true;

        return event.getSource().is(DamageTypes.MAGIC)
                && entity.hasEffect(MobEffects.POISON)
                && event.getSource().getEntity() == null
                && event.getSource().getDirectEntity() == null;
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            var entity = event.getEntity();

            if (entity.level().isClientSide() || event.getAmount() <= 0F || event.getSource().is(DamageTypes.THORNS))
                return;

            var attacker = event.getSource().getEntity() instanceof LivingEntity living && living != entity ? living : null;
            var poisonImmune = false;

            for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.THORN_PENDANT.value())) {
                if (!(stack.getItem() instanceof ThornPendantItem relic))
                    continue;

                var relicData = relic.getRelicData(entity, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("poison");

                if (!ability.canPlayerUse(entity))
                    continue;

                if (ability.isRankModifierUnlocked("resistance") && isPoisonDamage(event, entity))
                    poisonImmune = true;

                if (attacker == null)
                    continue;

                var reflect = Math.max(0D, Math.min(1D, ability.getStatData("reflect_damage").getValue()));

                if (reflect <= 0D)
                    continue;

                var reflectedDamage = (float) Math.max(0D, event.getAmount() * reflect);

                if (reflectedDamage <= 0F)
                    continue;

                var reflected = attacker.hurt(entity.damageSources().thorns(entity), reflectedDamage);

                if (!reflected)
                    continue;

                relicData.getLevelingData().addExperience("poison", "reflect_proc", 1D);
                ability.getStatisticData().getMetricData("procs").addValue(1D);

                if (!ability.isRankModifierUnlocked("poison"))
                    continue;

                var seconds = Math.max(0D, ability.getStatData("poison_duration").getValue());
                var ticks = Math.max(1, (int) Math.round(seconds * 20D));
                var level = Math.max(1, (int) MathUtils.round(ability.getStatData("poison_level").getValue(), 0));

                attacker.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, level - 1, false, true));
                ability.getStatisticData().getMetricData("poison_duration").addValue(ticks / 20D);
            }

            if (poisonImmune) {
                event.setAmount(0F);

                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof LivingEntity source) || source.level().isClientSide() || event.getEntity() == source || event.getAmount() <= 0F)
                return;

            if (!event.getEntity().hasEffect(MobEffects.POISON))
                return;

            var bonus = 0D;
            ThornPendantItem bonusRelic = null;
            ItemStack bonusStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(source, ModItems.THORN_PENDANT.value())) {
                if (!(stack.getItem() instanceof ThornPendantItem relic))
                    continue;

                var ability = relic.getRelicData(source, stack).getAbilitiesData().getAbilityData("poison");

                if (!ability.canPlayerUse(source) || !ability.isRankModifierUnlocked("damage"))
                    continue;

                var value = Math.max(0D, Math.min(1D, ability.getStatData("poisoned_damage_bonus").getValue()));

                if (value > bonus) {
                    bonus = value;
                    bonusRelic = relic;
                    bonusStack = stack;
                }
            }

            if (bonus <= 0D)
                return;

            var baseDamage = event.getAmount();
            var boostedDamage = (float) (baseDamage * (1D + bonus));
            var additionalDamage = Math.max(0F, boostedDamage - baseDamage);

            event.setAmount(boostedDamage);

            if (bonusRelic != null) {
                var relicData = bonusRelic.getRelicData(source, bonusStack);
                var ability = relicData.getAbilitiesData().getAbilityData("poison");

                relicData.getLevelingData().addExperience("poison", "poisoned_hit", 1D);

                if (additionalDamage > 0F)
                    ability.getStatisticData().getMetricData("poisoned_bonus_damage").addValue(additionalDamage);
            }
        }
    }
}