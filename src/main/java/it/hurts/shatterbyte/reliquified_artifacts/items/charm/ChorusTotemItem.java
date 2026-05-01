package it.hurts.shatterbyte.reliquified_artifacts.items.charm;

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
import it.hurts.sskirillss.relics.init.RelicsMobEffects;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import top.theillusivec4.curios.api.SlotContext;

public class ChorusTotemItem extends RAWearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("chorus")
                                .rankModifier(1, "recovery")
                                .rankModifier(3, "disorient")
                                .rankModifier(5, "vanishing")
                                .stat(AbilityStatTemplate.builder("cooldown")
                                        .initialValue(75D, 100D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.019D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("regen_boost")
                                        .initialValue(0.15D, 0.4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("regen_boost_duration")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("blind_radius")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("blind_duration")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0667D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("vanishing_duration")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("trigger").build())
                                        .source(ExperienceSourceTemplate.builder("disorient_target")
                                                .rankModifierVisibilityState("disorient", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("triggers")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("recovery_healed")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("recovery", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("disoriented_targets")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("disorient", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.THE_END, LootEntries.END_LIKE)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        if (getCooldownTicks(stack) > 0)
            setCooldownTicks(stack, getCooldownTicks(stack) - 1);

        if (getRegenBoostTicks(stack) > 0)
            setRegenBoostTicks(stack, getRegenBoostTicks(stack) - 1);
    }

    private int getCooldownTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(RADataComponent.CHORUS_TOTEM_COOLDOWN_TICKS.get(), 0));
    }

    private void setCooldownTicks(ItemStack stack, int ticks) {
        stack.set(RADataComponent.CHORUS_TOTEM_COOLDOWN_TICKS.get(), Math.max(0, ticks));
    }

    private int getRegenBoostTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(RADataComponent.CHORUS_TOTEM_REGEN_BOOST_TICKS.get(), 0));
    }

    private void setRegenBoostTicks(ItemStack stack, int ticks) {
        stack.set(RADataComponent.CHORUS_TOTEM_REGEN_BOOST_TICKS.get(), Math.max(0, ticks));
    }

    private static long secondsToTicks(double seconds) {
        return Math.max(0L, Math.round(Math.max(0D, seconds) * 20D));
    }

    private boolean teleportToSafeLocation(Player player, Vec3 origin) {
        var level = player.level();
        var random = player.getRandom();
        var minY = level.getMinBuildHeight() + 1;
        var maxY = level.getMaxBuildHeight() - 2;

        for (var attempt = 0; attempt < 40; attempt++) {
            var angle = random.nextDouble() * Math.PI * 2D;
            var distance = 32D * (0.35D + random.nextDouble() * 0.65D);
            var x = origin.x + Math.cos(angle) * distance;
            var z = origin.z + Math.sin(angle) * distance;
            var surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
            var y = Math.max(minY, Math.min(maxY, surfaceY + 1D));

            if (player.randomTeleport(x + 0.5D, y, z + 0.5D, true)) {
                player.fallDistance = 0F;
                return true;
            }
        }

        var fallbackY = Math.max(minY, Math.min(maxY, origin.y + 1D));

        if (player.randomTeleport(origin.x, fallbackY, origin.z, true)) {
            player.fallDistance = 0F;
            return true;
        }

        return false;
    }

    private static int applyBlindnessAndForget(Player player, Vec3 center, double radius, int durationTicks) {
        if (radius <= 0D || durationTicks <= 0)
            return 0;

        var box = new AABB(center, center).inflate(radius);
        var maxDistanceSq = radius * radius;
        var affectedTargets = 0;

        for (var entity : player.level().getEntitiesOfClass(LivingEntity.class, box, entity -> entity.isAlive() && entity != player)) {
            if (entity.distanceToSqr(center) > maxDistanceSq)
                continue;

            if (entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, durationTicks, 0, false, true)))
                affectedTargets++;

            if (entity instanceof Mob mob && mob.getTarget() == player)
                mob.setTarget(null);
        }

        return affectedTargets;
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getNewDamage() <= 0F)
                return;

            var lethalThreshold = player.getHealth() + player.getAbsorptionAmount();

            if (event.getNewDamage() < lethalThreshold)
                return;

            var triggerPos = player.position();

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.CHORUS_TOTEM.value())) {
                if (!(stack.getItem() instanceof ChorusTotemItem relic))
                    continue;

                var relicData = relic.getRelicData(player, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("chorus");

                if (!ability.canPlayerUse(player) || relic.getCooldownTicks(stack) > 0)
                    continue;

                var disorientedTargets = 0;

                if (ability.getRankModifierData("disorient").isEnabled()) {
                    var radius = Math.max(0D, ability.getStatData("blind_radius").getValue());
                    var durationTicks = Math.max(0, (int) secondsToTicks(ability.getStatData("blind_duration").getValue()));

                    disorientedTargets = applyBlindnessAndForget(player, triggerPos, radius, durationTicks);
                }

                var safeDamage = Math.max(0F, lethalThreshold - 1F);

                event.setNewDamage(Math.min(event.getNewDamage(), safeDamage));
                relic.teleportToSafeLocation(player, triggerPos);

                var cooldownTicks = Math.max(0, (int) secondsToTicks(ability.getStatData("cooldown").getValue()));

                if (cooldownTicks > 0)
                    relic.setCooldownTicks(stack, cooldownTicks);

                if (ability.getRankModifierData("recovery").isEnabled()) {
                    var regenBoostTicks = Math.max(0, (int) secondsToTicks(ability.getStatData("regen_boost_duration").getValue()));

                    relic.setRegenBoostTicks(stack, regenBoostTicks);
                } else {
                    relic.setRegenBoostTicks(stack, 0);
                }

                if (ability.getRankModifierData("vanishing").isEnabled()) {
                    var vanishingTicks = Math.max(0, (int) secondsToTicks(ability.getStatData("vanishing_duration").getValue()));

                    if (vanishingTicks > 0)
                        player.addEffect(new MobEffectInstance(RelicsMobEffects.VANISHING, vanishingTicks, 0, false, false));
                }

                relicData.getLevelingData().addExperience("chorus", "trigger", 1D);
                ability.getStatisticData().getMetricData("triggers").addValue(1D);

                if (disorientedTargets > 0) {
                    relicData.getLevelingData().addExperience("chorus", "disorient_target", disorientedTargets);
                    ability.getStatisticData().getMetricData("disoriented_targets").addValue(disorientedTargets);
                }

                break;
            }
        }

        @SubscribeEvent
        public static void onLivingHeal(LivingHealEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            var bonus = 0D;
            ChorusTotemItem bestRelic = null;
            ItemStack bestStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.CHORUS_TOTEM.value())) {
                if (!(stack.getItem() instanceof ChorusTotemItem relic))
                    continue;

                if (relic.getRegenBoostTicks(stack) <= 0)
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("chorus");

                if (!ability.canPlayerUse(player) || !ability.getRankModifierData("recovery").isEnabled())
                    continue;

                var value = Math.max(0D, ability.getStatData("regen_boost").getValue());

                if (value > bonus) {
                    bonus = value;
                    bestRelic = relic;
                    bestStack = stack;
                }
            }

            if (bonus <= 0D)
                return;

            var baseHeal = event.getAmount();
            var boostedHeal = (float) (baseHeal * (1D + bonus));
            var extraHeal = Math.max(0F, boostedHeal - baseHeal);

            event.setAmount(boostedHeal);

            if (bestRelic != null && extraHeal > 0F) {
                var ability = bestRelic.getRelicData(player, bestStack).getAbilitiesData().getAbilityData("chorus");
                ability.getStatisticData().getMetricData("recovery_healed").addValue(extraHeal);
            }
        }
    }
}
