package it.hurts.octostudios.rarcompat.items.necklace;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class ShockPendantItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("shock")
                                .rankModifier(1, "resistance")
                                .rankModifier(3, "conductor")
                                .rankModifier(5, "tremor")
                                .stat(AbilityStatTemplate.builder("chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("distance")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(4D, 10D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("bounces")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("damage")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("damage_modifier")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("tremor_chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.12D, 0.3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("tremor_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            var entity = event.getEntity();

            if (entity.level().isClientSide() || event.getAmount() <= 0F)
                return;

            var attacker = event.getSource().getEntity() instanceof LivingEntity living && living != entity ? living : null;
            var lightningDamage = event.getSource().is(DamageTypes.LIGHTNING_BOLT);
            var lightningImmune = false;
            var tremorChance = 0D;
            var tremorDuration = 0D;

            for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.SHOCK_PENDANT.value())) {
                if (!(stack.getItem() instanceof ShockPendantItem relic))
                    continue;

                var relicData = relic.getRelicData(entity, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("shock");

                if (!ability.canPlayerUse(entity))
                    continue;

                if (lightningDamage && ability.isRankModifierUnlocked("resistance"))
                    lightningImmune = true;

                if (attacker == null)
                    continue;

                var chance = Math.max(0D, Math.min(1D, ability.getStatData("chance").getValue()));

                if (chance > 0D && entity.getRandom().nextDouble() <= chance) {
                    var distance = (float) Math.max(0D, ability.getStatData("distance").getValue());
                    var bounces = Math.max(0, (int) MathUtils.round(ability.getStatData("bounces").getValue(), 0));
                    var damage = (float) Math.max(0D, ability.getStatData("damage").getValue());

                    if (distance > 0F && bounces > 0 && damage > 0F) {
                        var spark = new ElectricSparkEntity(RelicsEntities.ELECTRIC_SPARK.get(), entity.level());

                        if (ability.isRankModifierUnlocked("conductor"))
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
                    }
                }

                if (ability.isRankModifierUnlocked("tremor")) {
                    tremorChance = Math.max(tremorChance, Math.max(0D, Math.min(1D, ability.getStatData("tremor_chance").getValue())));
                    tremorDuration = Math.max(tremorDuration, Math.max(0D, ability.getStatData("tremor_duration").getValue()));
                }
            }

            if (lightningDamage && lightningImmune)
                event.setAmount(0F);

            if (attacker != null && tremorChance > 0D && tremorDuration > 0D && entity.getRandom().nextDouble() <= tremorChance) {
                var durationTicks = Math.max(1, (int) Math.round(tremorDuration * 20D));
                attacker.addEffect(new MobEffectInstance(RelicsMobEffects.TREMOR, durationTicks, 0, false, true));
            }
        }
    }
}