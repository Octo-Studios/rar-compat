package it.hurts.octostudios.rarcompat.items.necklace;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.LivingEntity;
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
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 8D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("fire_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("undead_damage")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("survival_chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.08D, 0.2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .build())
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

            for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.CROSS_NECKLACE.value())) {
                if (!(stack.getItem() instanceof CrossNecklaceItem relic))
                    continue;

                var ability = relic.getRelicData(entity, stack).getAbilitiesData().getAbilityData("protection");

                if (!ability.canPlayerUse(entity) || !ability.isRankModifierUnlocked("salvation"))
                    continue;

                var value = Math.max(0D, Math.min(1D, ability.getStatData("survival_chance").getValue()));

                chance = Math.max(chance, value);
            }

            if (chance <= 0D || entity.getRandom().nextDouble() > chance)
                return;

            var safeDamage = Math.max(0F, lethalThreshold - 1F);

            event.setNewDamage(Math.min(event.getNewDamage(), safeDamage));
        }

        @SubscribeEvent
        public static void onLivingDamagePost(LivingDamageEvent.Post event) {
            var entity = event.getEntity();

            if (entity.level().isClientSide() || event.getNewDamage() <= 0F)
                return;

            var attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
            var bonusTicks = 0;
            var fireSeconds = 0F;

            for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.CROSS_NECKLACE.value())) {
                if (!(stack.getItem() instanceof CrossNecklaceItem relic))
                    continue;

                var ability = relic.getRelicData(entity, stack).getAbilitiesData().getAbilityData("protection");

                if (!ability.canPlayerUse(entity))
                    continue;

                var invulnerability = Math.max(0, (int) MathUtils.round(ability.getStatData("invulnerability").getValue(), 0));

                bonusTicks = Math.max(bonusTicks, invulnerability);

                if (attacker != null && attacker.isInvertedHealAndHarm() && ability.isRankModifierUnlocked("holy_fire")) {
                    var value = (float) Math.max(0D, ability.getStatData("fire_duration").getValue());

                    fireSeconds = Math.max(fireSeconds, value);
                }
            }

            if (bonusTicks > 0) {
                var baseTicks = Math.max(entity.invulnerableTime, event.getPostAttackInvulnerabilityTicks());

                entity.invulnerableTime = baseTicks + bonusTicks;
            }

            if (attacker != null && fireSeconds > 0F)
                attacker.igniteForSeconds(fireSeconds);
        }

        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof LivingEntity source) || source.level().isClientSide() || event.getEntity() == source)
                return;

            if (!event.getEntity().isInvertedHealAndHarm())
                return;

            var bonus = 0D;

            for (var stack : EntityUtils.findEquippedCurios(source, ModItems.CROSS_NECKLACE.value())) {
                if (!(stack.getItem() instanceof CrossNecklaceItem relic))
                    continue;

                var ability = relic.getRelicData(source, stack).getAbilitiesData().getAbilityData("protection");

                if (!ability.canPlayerUse(source) || !ability.isRankModifierUnlocked("smite"))
                    continue;

                var value = Math.max(0D, Math.min(1D, ability.getStatData("undead_damage").getValue()));

                bonus = Math.max(bonus, value);
            }

            if (bonus > 0D)
                event.setAmount((float) (event.getAmount() * (1D + bonus)));
        }
    }
}