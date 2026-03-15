package it.hurts.shatterbyte.reliquified_artifacts.items.hands;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.items.WearableRelicItem;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public class GoldenHookItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("hook")
                                .rankModifier(1, "boat_guard")
                                .rankModifier(3, "crowd_pull")
                                .rankModifier(5, "disarm")
                                .stat(AbilityStatTemplate.builder("experience_bonus")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1755D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("experience_pull_radius")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("entities_pull_radius")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("steal_chance")
                                        .initialValue(0.01D, 0.025D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("xp_drop").build())
                                        .source(ExperienceSourceTemplate.builder("crowd_pull_target")
                                                .rankModifierVisibilityState("crowd_pull", VisibilityState.OBFUSCATED)
                                                .build())
                                        .source(ExperienceSourceTemplate.builder("disarm_item")
                                                .rankModifierVisibilityState("disarm", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("bonus_experience_gained")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("xp_orbs_picked")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("boat_guard", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("disarmed_items")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("disarm", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.AQUATIC)
                        .build())
                .build();
    }

    private static boolean tryStealItem(Player player, LivingEntity target) {
        if (target instanceof Player targetPlayer && targetPlayer.getAbilities().instabuild)
            return false;

        var main = target.getMainHandItem();
        var off = target.getOffhandItem();
        InteractionHand hand = null;

        if (!main.isEmpty() && !off.isEmpty())
            hand = player.getRandom().nextBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        else if (!main.isEmpty())
            hand = InteractionHand.MAIN_HAND;
        else if (!off.isEmpty())
            hand = InteractionHand.OFF_HAND;

        if (hand == null)
            return false;

        var stolen = target.getItemInHand(hand).copy();

        if (stolen.isEmpty())
            return false;

        target.setItemInHand(hand, ItemStack.EMPTY);

        if (!player.addItem(stolen))
            player.drop(stolen, false);

        return true;
    }

    private static void tryAutoPickupExperience(Player player) {
        var radius = 0D;
        GoldenHookItem bestRelic = null;
        ItemStack bestStack = ItemStack.EMPTY;

        for (var stack : EntityUtils.findEquippedCurios(player, ModItems.GOLDEN_HOOK.value())) {
            if (!(stack.getItem() instanceof GoldenHookItem relic))
                continue;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("hook");

            if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("boat_guard"))
                continue;

            var value = Math.max(0D, ability.getStatData("experience_pull_radius").getValue());

            if (bestRelic == null || value > radius) {
                radius = value;
                bestRelic = relic;
                bestStack = stack;
            }
        }

        if (bestRelic == null || radius <= 0D)
            return;

        var radiusSq = radius * radius;
        var pickedOrbs = 0;

        for (var orb : player.level().getEntitiesOfClass(ExperienceOrb.class, player.getBoundingBox().inflate(radius), entity -> entity.isAlive())) {
            if (orb.distanceToSqr(player) > radiusSq)
                continue;

            var aliveBefore = orb.isAlive();
            orb.playerTouch(player);

            if (aliveBefore && !orb.isAlive())
                pickedOrbs++;
        }

        if (pickedOrbs <= 0)
            return;

        bestRelic.getRelicData(player, bestStack).getAbilitiesData().getAbilityData("hook")
                .getStatisticData().getMetricData("xp_orbs_picked").addValue(pickedOrbs);
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLevelTickPost(LevelTickEvent.Post event) {
            var level = event.getLevel();

            if (level.isClientSide())
                return;

            for (var player : level.players()) {
                if (!player.isAlive() || player.isSpectator())
                    continue;

                tryAutoPickupExperience(player);
            }
        }

        @SubscribeEvent
        public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
            var player = event.getAttackingPlayer();

            if (player == null)
                return;

            var bonus = 0D;
            GoldenHookItem bestRelic = null;
            ItemStack bestStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.GOLDEN_HOOK.value())) {
                if (!(stack.getItem() instanceof GoldenHookItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("hook");

                if (!ability.canPlayerUse(player))
                    continue;

                var value = Math.max(0D, ability.getStatData("experience_bonus").getValue());

                if (bestRelic == null || value > bonus) {
                    bonus = value;
                    bestRelic = relic;
                    bestStack = stack;
                }
            }

            if (bestRelic == null || bonus <= 0D)
                return;

            var droppedExperience = Math.max(0, event.getDroppedExperience());
            var boostedExperience = Math.max(0, (int) Math.round(droppedExperience * (1D + bonus)));

            event.setDroppedExperience(boostedExperience);

            var relicData = bestRelic.getRelicData(player, bestStack);
            var ability = relicData.getAbilitiesData().getAbilityData("hook");

            relicData.getLevelingData().addExperience("hook", "xp_drop", 1D);

            var gainedExperience = Math.max(0, boostedExperience - droppedExperience);

            if (gainedExperience > 0)
                ability.getStatisticData().getMetricData("bonus_experience_gained").addValue(gainedExperience);
        }

        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            var pullRadius = 0D;
            var stealChance = 0D;
            GoldenHookItem pullRelic = null;
            GoldenHookItem stealRelic = null;
            ItemStack pullStack = ItemStack.EMPTY;
            ItemStack stealStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.GOLDEN_HOOK.value())) {
                if (!(stack.getItem() instanceof GoldenHookItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("hook");

                if (!ability.canPlayerUse(player))
                    continue;

                if (ability.isRankModifierUnlocked("crowd_pull")) {
                    var value = Math.max(0D, ability.getStatData("entities_pull_radius").getValue());

                    if (pullRelic == null || value > pullRadius) {
                        pullRadius = value;
                        pullRelic = relic;
                        pullStack = stack;
                    }
                }

                if (ability.isRankModifierUnlocked("disarm")) {
                    var value = Math.max(0D, Math.min(1D, ability.getStatData("steal_chance").getValue()));

                    if (stealRelic == null || value > stealChance) {
                        stealChance = value;
                        stealRelic = relic;
                        stealStack = stack;
                    }
                }
            }

            var fullyChargedAttack = player.getAttackStrengthScale(0.5F) > 0.9F;

            if (pullRadius > 0D && fullyChargedAttack) {
                var center = event.getEntity();
                var centerPos = center.position().add(0D, center.getBbHeight() * 0.5D, 0D);
                var maxDistanceSq = pullRadius * pullRadius;
                var pulledTargets = 0;

                for (var nearby : center.level().getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(pullRadius), entity -> entity.isAlive() && entity != center && entity != player)) {
                    var nearbyPos = nearby.position().add(0D, nearby.getBbHeight() * 0.5D, 0D);

                    if (nearbyPos.distanceToSqr(centerPos) > maxDistanceSq)
                        continue;

                    var direction = centerPos.subtract(nearbyPos);

                    if (direction.lengthSqr() <= 1.0E-6D)
                        continue;

                    var velocity = nearby.getDeltaMovement().add(direction.normalize().scale(0.28D));

                    nearby.setDeltaMovement(velocity);
                    nearby.hasImpulse = true;
                    pulledTargets++;

                    if (nearby instanceof Mob mob) {
                        mob.setTarget(null);
                        mob.getNavigation().stop();
                    }
                }

                if (pullRelic != null && pulledTargets > 0)
                    pullRelic.getRelicData(player, pullStack).getLevelingData().addExperience("hook", "crowd_pull_target", pulledTargets);
            }

            if (stealChance > 0D && stealRelic != null && event.getEntity() instanceof LivingEntity target && player.getRandom().nextDouble() <= stealChance
                    && tryStealItem(player, target)) {
                var relicData = stealRelic.getRelicData(player, stealStack);

                relicData.getLevelingData().addExperience("hook", "disarm_item", 1D);
                relicData.getAbilitiesData().getAbilityData("hook").getStatisticData().getMetricData("disarmed_items").addValue(1D);
            }
        }
    }
}
