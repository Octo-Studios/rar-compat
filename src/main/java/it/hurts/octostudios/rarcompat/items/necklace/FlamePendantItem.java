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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class FlamePendantItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("fire")
                                .rankModifier(1, "burning_damage")
                                .rankModifier(3, "resistance")
                                .rankModifier(5, "healing")
                                .stat(AbilityStatTemplate.builder("chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("fire_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("burning_damage_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("healing")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.5D, 2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("ignite").build())
                                        .source(ExperienceSourceTemplate.builder("burning_hit").build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("ignited_targets")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("ignited_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("burning_bonus_damage")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("burning_damage", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("healing_restored")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("healing", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        var entity = slotContext.entity();

        if (!(entity instanceof LivingEntity living) || living.level().isClientSide())
            return;

        var ability = this.getRelicData(living, stack).getAbilitiesData().getAbilityData("fire");

        if (!ability.canPlayerUse(living) || !ability.isRankModifierUnlocked("healing") || !living.isOnFire())
            return;

        if (living.tickCount % 20 != 0)
            return;

        var heal = (float) Math.max(0D, ability.getStatData("healing").getValue());

        if (heal <= 0F)
            return;

        var beforeHealth = living.getHealth();

        living.heal(heal);

        var restored = Math.max(0F, living.getHealth() - beforeHealth);

        if (restored > 0F)
            ability.getStatisticData().getMetricData("healing_restored").addValue(restored);
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            var entity = event.getEntity();

            if (entity.level().isClientSide() || event.getAmount() <= 0F)
                return;

            var attacker = event.getSource().getEntity() instanceof LivingEntity living && living != entity ? living : null;
            var fireImmune = false;

            for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.FLAME_PENDANT.value())) {
                if (!(stack.getItem() instanceof FlamePendantItem relic))
                    continue;

                var relicData = relic.getRelicData(entity, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("fire");

                if (!ability.canPlayerUse(entity))
                    continue;

                if (ability.isRankModifierUnlocked("resistance"))
                    fireImmune = true;

                if (attacker == null)
                    continue;

                var chance = Math.max(0D, Math.min(1D, ability.getStatData("chance").getValue()));

                if (chance <= 0D || entity.getRandom().nextDouble() > chance)
                    continue;

                var duration = (float) Math.max(0D, ability.getStatData("fire_duration").getValue());

                if (duration <= 0F)
                    continue;

                attacker.igniteForSeconds(duration);

                relicData.getLevelingData().addExperience("fire", "ignite", 1D);
                ability.getStatisticData().getMetricData("ignited_targets").addValue(1D);
                ability.getStatisticData().getMetricData("ignited_duration").addValue(duration);
            }

            if (fireImmune && event.getSource().is(DamageTypeTags.IS_FIRE) && !event.getSource().is(DamageTypes.LAVA)) {
                event.setAmount(0F);

                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof LivingEntity source) || source.level().isClientSide() || event.getEntity() == source || event.getAmount() <= 0F)
                return;

            if (!event.getEntity().isOnFire())
                return;

            var bonus = 0D;
            FlamePendantItem bonusRelic = null;
            ItemStack bonusStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(source, ModItems.FLAME_PENDANT.value())) {
                if (!(stack.getItem() instanceof FlamePendantItem relic))
                    continue;

                var ability = relic.getRelicData(source, stack).getAbilitiesData().getAbilityData("fire");

                if (!ability.canPlayerUse(source) || !ability.isRankModifierUnlocked("burning_damage"))
                    continue;

                var value = Math.max(0D, Math.min(1D, ability.getStatData("burning_damage_bonus").getValue()));

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
                var ability = relicData.getAbilitiesData().getAbilityData("fire");

                relicData.getLevelingData().addExperience("fire", "burning_hit", 1D);

                if (additionalDamage > 0F)
                    ability.getStatisticData().getMetricData("burning_bonus_damage").addValue(additionalDamage);
            }
        }
    }
}