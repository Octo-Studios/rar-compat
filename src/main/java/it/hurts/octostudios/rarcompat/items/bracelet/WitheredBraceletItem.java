package it.hurts.octostudios.rarcompat.items.bracelet;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class WitheredBraceletItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("withering")
                                .rankModifier(1, "resistance")
                                .rankModifier(3, "spread")
                                .rankModifier(5, "leech")
                                .stat(AbilityStatTemplate.builder("chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("wither_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("wither_level")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("spread_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("spread_radius")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(3D, 8D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("new_wither_target").build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("withered_targets")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("leech_healing_done")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("leech", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(0, (int) Math.round(Math.max(0D, seconds) * 20D));
    }

    private static boolean isWitherDamage(LivingIncomingDamageEvent event) {
        return event.getSource().is(DamageTypes.WITHER) || event.getSource().is(DamageTypes.WITHER_SKULL);
    }

    private static boolean isWitherDamage(LivingDamageEvent.Post event) {
        return event.getSource().is(DamageTypes.WITHER) || event.getSource().is(DamageTypes.WITHER_SKULL);
    }

    private static boolean applyWitherFromBracelet(LivingEntity target, Player owner, int ticks, int amplifier) {
        if (ticks <= 0)
            return false;

        var newlyWithered = !target.hasEffect(MobEffects.WITHER);

        target.addEffect(new MobEffectInstance(MobEffects.WITHER, ticks, Math.max(0, amplifier), false, true));

        var data = target.getPersistentData();
        var expireTick = owner.level().getGameTime() + Math.max(1L, ticks);

        if (data.hasUUID("rarcompat_withered_bracelet_owner") && owner.getUUID().equals(data.getUUID("rarcompat_withered_bracelet_owner")))
            expireTick = Math.max(expireTick, data.getLong("rarcompat_withered_bracelet_expire"));

        data.putUUID("rarcompat_withered_bracelet_owner", owner.getUUID());
        data.putLong("rarcompat_withered_bracelet_expire", expireTick);

        return newlyWithered;
    }

    private static Player getActiveOwner(LivingEntity target) {
        var level = target.level();

        if (level.isClientSide())
            return null;

        var data = target.getPersistentData();

        if (!data.hasUUID("rarcompat_withered_bracelet_owner"))
            return null;

        var expireTick = data.getLong("rarcompat_withered_bracelet_expire");

        if (level.getGameTime() > expireTick)
            return null;

        var server = level.getServer();

        if (server == null)
            return null;

        return server.getPlayerList().getPlayer(data.getUUID("rarcompat_withered_bracelet_owner"));
    }

    private static void awardNewWitherTarget(WitheredBraceletItem relic, Player owner, ItemStack stack, int targets) {
        if (targets <= 0)
            return;

        var relicData = relic.getRelicData(owner, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("withering");

        if (!ability.canPlayerUse(owner))
            return;

        relicData.getLevelingData().addExperience("withering", "new_wither_target", targets);
        ability.getStatisticData().getMetricData("withered_targets").addValue(targets);
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            var entity = event.getEntity();

            if (entity.level().isClientSide() || event.getAmount() <= 0F)
                return;

            if (isWitherDamage(event)) {
                var witherImmune = false;

                for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.WITHERED_BRACELET.value())) {
                    if (!(stack.getItem() instanceof WitheredBraceletItem relic))
                        continue;

                    var ability = relic.getRelicData(entity, stack).getAbilitiesData().getAbilityData("withering");

                    if (ability.canPlayerUse(entity) && ability.isRankModifierUnlocked("resistance")) {
                        witherImmune = true;
                        break;
                    }
                }

                if (witherImmune) {
                    event.setAmount(0F);
                    event.setCanceled(true);
                }

                return;
            }

            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || event.getEntity() == player)
                return;

            var target = event.getEntity();

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.WITHERED_BRACELET.value())) {
                if (!(stack.getItem() instanceof WitheredBraceletItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("withering");

                if (!ability.canPlayerUse(player))
                    continue;

                var chance = Math.max(0D, Math.min(1D, ability.getStatData("chance").getValue()));

                if (chance <= 0D || player.getRandom().nextDouble() > chance)
                    continue;

                var durationTicks = secondsToTicks(ability.getStatData("wither_duration").getValue());

                if (durationTicks <= 0)
                    continue;

                var witherLevel = Math.max(1, (int) MathUtils.round(ability.getStatData("wither_level").getValue(), 0));

                if (applyWitherFromBracelet(target, player, durationTicks, witherLevel - 1))
                    awardNewWitherTarget(relic, player, stack, 1);
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            var entity = event.getEntity();
            var level = entity.level();

            if (level.isClientSide() || !entity.hasEffect(MobEffects.WITHER))
                return;

            var owner = getActiveOwner(entity);

            if (owner == null || !owner.isAlive())
                return;

            var spreadDuration = 0;
            var spreadRadius = 0D;
            var spreadAmplifier = 0;
            WitheredBraceletItem spreadRelic = null;
            ItemStack spreadStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(owner, ModItems.WITHERED_BRACELET.value())) {
                if (!(stack.getItem() instanceof WitheredBraceletItem relic))
                    continue;

                var ability = relic.getRelicData(owner, stack).getAbilitiesData().getAbilityData("withering");

                if (!ability.canPlayerUse(owner) || !ability.isRankModifierUnlocked("spread"))
                    continue;

                var localDuration = secondsToTicks(ability.getStatData("spread_duration").getValue());
                var localRadius = Math.max(0D, ability.getStatData("spread_radius").getValue());
                var localAmplifier = Math.max(0, (int) MathUtils.round(ability.getStatData("wither_level").getValue(), 0) - 1);

                if (localRadius > spreadRadius || localRadius == spreadRadius && localDuration > spreadDuration || localRadius == spreadRadius && localDuration == spreadDuration && localAmplifier > spreadAmplifier) {
                    spreadRelic = relic;
                    spreadStack = stack;
                }

                spreadDuration = Math.max(spreadDuration, localDuration);
                spreadRadius = Math.max(spreadRadius, localRadius);
                spreadAmplifier = Math.max(spreadAmplifier, localAmplifier);
            }

            if (spreadDuration <= 0 || spreadRadius <= 0D || spreadRelic == null || spreadStack.isEmpty())
                return;

            var radiusSq = spreadRadius * spreadRadius;
            var newWitheredTargets = 0;

            for (var nearby : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(spreadRadius), target -> target.isAlive() && target != entity && target != owner)) {
                if (nearby.distanceToSqr(entity) > radiusSq)
                    continue;

                if (applyWitherFromBracelet(nearby, owner, spreadDuration, spreadAmplifier))
                    newWitheredTargets++;
            }

            if (newWitheredTargets > 0)
                awardNewWitherTarget(spreadRelic, owner, spreadStack, newWitheredTargets);
        }

        @SubscribeEvent
        public static void onLivingDamagePost(LivingDamageEvent.Post event) {
            var target = event.getEntity();

            if (target.level().isClientSide() || event.getNewDamage() <= 0F || !isWitherDamage(event))
                return;

            var owner = getActiveOwner(target);

            if (owner == null || !owner.isAlive())
                return;

            ItemStack leechStack = ItemStack.EMPTY;
            WitheredBraceletItem leechRelic = null;

            for (var stack : EntityUtils.findEquippedCurios(owner, ModItems.WITHERED_BRACELET.value())) {
                if (!(stack.getItem() instanceof WitheredBraceletItem relic))
                    continue;

                var ability = relic.getRelicData(owner, stack).getAbilitiesData().getAbilityData("withering");

                if (!ability.canPlayerUse(owner) || !ability.isRankModifierUnlocked("leech"))
                    continue;

                leechRelic = relic;
                leechStack = stack;
                break;
            }

            if (leechRelic == null || leechStack.isEmpty())
                return;

            var healthBefore = owner.getHealth();

            owner.heal(event.getNewDamage());

            var healed = Math.max(0F, owner.getHealth() - healthBefore);

            if (healed <= 0F)
                return;

            leechRelic.getRelicData(owner, leechStack).getAbilitiesData().getAbilityData("withering")
                    .getStatisticData().getMetricData("leech_healing_done").addValue(healed);
        }
    }
}
