package it.hurts.shatterbyte.reliquified_artifacts.items;

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
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.food.FoodProperties;

public class EternalSteakItem extends EverlastingFoodRelicItem {
    public EternalSteakItem() {
        super(new FoodProperties.Builder()
                .nutrition(8)
                .saturationModifier(0.8F)
                .build());
    }

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("meal")
                                .rankModifier(1, "restoration")
                                .rankModifier(3, "preservation")
                                .rankModifier(5, "quick_meal")
                                .stat(AbilityStatTemplate.builder("regeneration")
                                        .initialValue(30D, 25D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 4.9975D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("healing")
                                        .initialValue(0.5D, 2.5D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 9.99875D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("preservation_chance")
                                        .initialValue(0.01D, 0.05D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.24985D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("consume_speed")
                                        .thresholdValue(0D, 0.95D)
                                        .initialValue(0.1D, 0.25D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.7505D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("durability")
                                        .initialValue(1D, 3D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 10.00035D)
                                        .formatValue(value -> Math.max(1, (int) Math.round(value)))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("consume").build())
                                        .source(ExperienceSourceTemplate.builder("healing")
                                                .rankModifierVisibilityState("restoration", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("consumes")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("consume_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("healed")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 1)))
                                                .rankModifierVisibilityState("restoration", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
