package it.hurts.octostudios.rarcompat.items.hands;

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
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.12D, 0.4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("pull_radius")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(3D, 8D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("steal_chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.05D, 0.2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private static void tryStealItem(Player player, LivingEntity target) {
        if (target instanceof Player targetPlayer && targetPlayer.getAbilities().instabuild)
            return;

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
            return;

        var stolen = target.getItemInHand(hand).copy();

        if (stolen.isEmpty())
            return;

        target.setItemInHand(hand, ItemStack.EMPTY);

        if (!player.addItem(stolen))
            player.drop(stolen, false);
    }

    private static double getAutoPickupRadius(Player player) {
        var radius = 0D;

        for (var stack : EntityUtils.findEquippedCurios(player, ModItems.GOLDEN_HOOK.value())) {
            if (!(stack.getItem() instanceof GoldenHookItem relic))
                continue;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("hook");

            if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("boat_guard"))
                continue;

            radius = Math.max(radius, Math.max(0D, ability.getStatData("pull_radius").getValue()));
        }

        return radius;
    }

    private static void tryAutoPickupExperience(Player player) {
        var radius = getAutoPickupRadius(player);

        if (radius <= 0D)
            return;

        var radiusSq = radius * radius;

        for (var orb : player.level().getEntitiesOfClass(ExperienceOrb.class, player.getBoundingBox().inflate(radius), entity -> entity.isAlive())) {
            if (orb.distanceToSqr(player) > radiusSq)
                continue;

            orb.playerTouch(player);
        }
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
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

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.GOLDEN_HOOK.value())) {
                if (!(stack.getItem() instanceof GoldenHookItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("hook");

                if (!ability.canPlayerUse(player))
                    continue;

                var value = Math.max(0D, ability.getStatData("experience_bonus").getValue());

                bonus = Math.max(bonus, value);
            }

            if (bonus <= 0D)
                return;

            event.setDroppedExperience(Math.max(0, (int) Math.round(event.getDroppedExperience() * (1D + bonus))));
        }

        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            var pullRadius = 0D;
            var stealChance = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.GOLDEN_HOOK.value())) {
                if (!(stack.getItem() instanceof GoldenHookItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("hook");

                if (!ability.canPlayerUse(player))
                    continue;

                if (ability.isRankModifierUnlocked("crowd_pull"))
                    pullRadius = Math.max(pullRadius, Math.max(0D, ability.getStatData("pull_radius").getValue()));

                if (ability.isRankModifierUnlocked("disarm")) {
                    var value = Math.max(0D, Math.min(1D, ability.getStatData("steal_chance").getValue()));

                    stealChance = Math.max(stealChance, value);
                }
            }

            var fullyChargedAttack = player.getAttackStrengthScale(0.5F) > 0.9F;

            if (pullRadius > 0D && fullyChargedAttack) {
                var center = event.getEntity();
                var centerPos = center.position().add(0D, center.getBbHeight() * 0.5D, 0D);
                var maxDistanceSq = pullRadius * pullRadius;

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

                    if (nearby instanceof Mob mob) {
                        mob.setTarget(null);
                        mob.getNavigation().stop();
                    }
                }
            }

            if (stealChance > 0D && event.getEntity() instanceof LivingEntity target && player.getRandom().nextDouble() <= stealChance)
                tryStealItem(player, target);
        }
    }
}
