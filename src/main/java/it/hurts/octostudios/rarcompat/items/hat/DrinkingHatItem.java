package it.hurts.octostudios.rarcompat.items.hat;

import artifacts.registry.ModAttributes;
import artifacts.registry.ModItems;
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
import it.hurts.sskirillss.relics.items.relics.base.data.RelicAttributeModifier;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DrinkingHatItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("drinking")
                                .rankModifier(1, "breathing")
                                .rankModifier(3, "nutrition")
                                .rankModifier(5, "healing")
                                .stat(AbilityStatTemplate.builder("speed")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.2D, 0.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("hunger")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("health")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("air")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(40D, 100D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("consume").build())
                                        .source(ExperienceSourceTemplate.builder("breathing")
                                                .rankModifierVisibilityState("breathing", VisibilityState.OBFUSCATED)
                                                .build())
                                        .source(ExperienceSourceTemplate.builder("nutrition")
                                                .rankModifierVisibilityState("nutrition", VisibilityState.OBFUSCATED)
                                                .build())
                                        .source(ExperienceSourceTemplate.builder("healing")
                                                .rankModifierVisibilityState("healing", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("consumed_items")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("consumed_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("air_restored")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("breathing", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("hunger_restored")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("nutrition", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("health_restored")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 1)))
                                                .rankModifierVisibilityState("healing", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public @Nullable RelicAttributeModifier getRelicAttributeModifiers(LivingEntity entity, ItemStack stack) {
        var ability = this.getRelicData(entity, stack).getAbilitiesData().getAbilityData("drinking");

        if (!ability.canPlayerUse(entity))
            return null;

        var speed = Math.max(0D, ability.getStatData("speed").getValue());

        if (speed <= 0D)
            return null;

        return RelicAttributeModifier.builder()
                .attribute(new RelicAttributeModifier.Modifier(ModAttributes.DRINKING_SPEED, (float) speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || newStack.getItem() == stack.getItem())
            return;

        EntityUtils.removeAttribute(player, stack, ModAttributes.DRINKING_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @EventBusSubscriber
    public static class HatEvents {
        private static final Map<UUID, Long> DRINK_START_TICKS = new HashMap<>();

        private static ItemStack getEquippedDrinkingHat(Player player) {
            var stack = EntityUtils.findEquippedCurio(player, ModItems.PLASTIC_DRINKING_HAT.value());

            if (stack.isEmpty())
                stack = EntityUtils.findEquippedCurio(player, ModItems.NOVELTY_DRINKING_HAT.value());

            return stack;
        }

        @SubscribeEvent
        public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getItem().getUseAnimation() != UseAnim.DRINK)
                return;

            var stack = getEquippedDrinkingHat(player);

            if (!(stack.getItem() instanceof DrinkingHatItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("drinking");

            if (!ability.canPlayerUse(player))
                return;

            DRINK_START_TICKS.put(player.getUUID(), player.level().getGameTime());
        }

        @SubscribeEvent
        public static void onUseItemStop(LivingEntityUseItemEvent.Stop event) {
            if (event.getEntity() instanceof Player player)
                DRINK_START_TICKS.remove(player.getUUID());
        }

        @SubscribeEvent
        public static void onUseItem(LivingEntityUseItemEvent.Finish event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getItem().getUseAnimation() != UseAnim.DRINK)
                return;

            var stack = getEquippedDrinkingHat(player);

            if (!(stack.getItem() instanceof DrinkingHatItem relic))
                return;

            var relicData = relic.getRelicData(player, stack);
            var ability = relicData.getAbilitiesData().getAbilityData("drinking");

            if (!ability.canPlayerUse(player))
                return;

            ability.getStatisticData().getMetricData("consumed_items").addValue(1D);
            relicData.getLevelingData().addExperience("drinking", "consume", 1D);

            var startTick = DRINK_START_TICKS.remove(player.getUUID());

            if (startTick != null) {
                var durationSeconds = Math.max(0D, (player.level().getGameTime() - startTick) / 20D);

                if (durationSeconds > 0D)
                    ability.getStatisticData().getMetricData("consumed_duration").addValue(durationSeconds);
            }

            if (ability.isRankModifierUnlocked("breathing")) {
                var air = Math.max(0, (int) MathUtils.round(ability.getStatData("air").getValue(), 0));

                if (air > 0) {
                    var beforeAir = player.getAirSupply();

                    player.setAirSupply(Math.min(player.getMaxAirSupply(), beforeAir + air));

                    var restoredAir = Math.max(0, player.getAirSupply() - beforeAir);

                    if (restoredAir > 0) {
                        ability.getStatisticData().getMetricData("air_restored").addValue(restoredAir);
                        relicData.getLevelingData().addExperience("drinking", "breathing", restoredAir);
                    }
                }
            }

            if (ability.isRankModifierUnlocked("nutrition")) {
                var hunger = Math.max(0, (int) MathUtils.round(ability.getStatData("hunger").getValue(), 0));

                if (hunger > 0) {
                    var beforeFood = player.getFoodData().getFoodLevel();

                    player.getFoodData().eat(hunger, 0F);

                    var restoredHunger = Math.max(0, player.getFoodData().getFoodLevel() - beforeFood);

                    if (restoredHunger > 0) {
                        ability.getStatisticData().getMetricData("hunger_restored").addValue(restoredHunger);
                        relicData.getLevelingData().addExperience("drinking", "nutrition", restoredHunger);
                    }
                }
            }

            if (ability.isRankModifierUnlocked("healing")) {
                var heal = (float) Math.max(0D, ability.getStatData("health").getValue());

                if (heal > 0F) {
                    var beforeHealth = player.getHealth();

                    player.heal(heal);

                    var restoredHealth = Math.max(0F, player.getHealth() - beforeHealth);

                    if (restoredHealth > 0F) {
                        ability.getStatisticData().getMetricData("health_restored").addValue(restoredHealth);
                        relicData.getLevelingData().addExperience("drinking", "healing", restoredHealth);
                    }
                }
            }
        }
    }
}