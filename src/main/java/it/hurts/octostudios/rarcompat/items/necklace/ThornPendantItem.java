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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
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
                                .stat(AbilityStatTemplate.builder("poisoned_damage_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
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

                var ability = relic.getRelicData(entity, stack).getAbilitiesData().getAbilityData("poison");

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

                if (!reflected || !ability.isRankModifierUnlocked("poison"))
                    continue;

                var seconds = Math.max(0D, ability.getStatData("poison_duration").getValue());
                var ticks = Math.max(1, (int) Math.round(seconds * 20D));

                attacker.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, 0, false, true));
            }

            if (poisonImmune) {
                event.setAmount(0F);

                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof LivingEntity source) || source.level().isClientSide() || event.getEntity() == source)
                return;

            if (!event.getEntity().hasEffect(MobEffects.POISON))
                return;

            var bonus = 0D;

            for (var stack : EntityUtils.findEquippedCurios(source, ModItems.THORN_PENDANT.value())) {
                if (!(stack.getItem() instanceof ThornPendantItem relic))
                    continue;

                var ability = relic.getRelicData(source, stack).getAbilitiesData().getAbilityData("poison");

                if (!ability.canPlayerUse(source) || !ability.isRankModifierUnlocked("damage"))
                    continue;

                var value = Math.max(0D, Math.min(1D, ability.getStatData("poisoned_damage_bonus").getValue()));
                bonus = Math.max(bonus, value);
            }

            if (bonus > 0D)
                event.setAmount((float) (event.getAmount() * (1D + bonus)));
        }
    }
}
