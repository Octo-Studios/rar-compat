package it.hurts.octostudios.rarcompat.items.hands;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.entities.SparkEntity;
import it.hurts.octostudios.rarcompat.init.EntityRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Comparator;

public class FireGauntletItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("flame")
                                .rankModifier(1, "burning_damage")
                                .rankModifier(3, "spread")
                                .rankModifier(5, "spark")
                                .stat(AbilityStatTemplate.builder("fire_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("burning_damage_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.12D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("spread_radius")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(3D, 8D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("spread_fire_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("spark_count")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("spark_damage")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 7D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("spark_fire_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private static long secondsToTicks(double seconds) {
        return Math.max(0L, Math.round(Math.max(0D, seconds) * 20D));
    }

    public static void igniteFromGauntlet(LivingEntity target, Player owner, double seconds) {
        if (seconds <= 0D)
            return;

        target.igniteForSeconds((float) seconds);

        var data = target.getPersistentData();
        var expireTick = owner.level().getGameTime() + Math.max(1L, secondsToTicks(seconds));

        if (data.hasUUID("rarcompat_fire_gauntlet_owner") && owner.getUUID().equals(data.getUUID("rarcompat_fire_gauntlet_owner")))
            expireTick = Math.max(expireTick, data.getLong("rarcompat_fire_gauntlet_expire"));

        data.putUUID("rarcompat_fire_gauntlet_owner", owner.getUUID());
        data.putLong("rarcompat_fire_gauntlet_expire", expireTick);
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            var target = event.getEntity();
            var wasOnFire = target.isOnFire();
            var burningDamageBonus = 0D;
            var spreadRadius = 0D;
            var spreadFireDuration = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.FIRE_GAUNTLET.value())) {
                if (!(stack.getItem() instanceof FireGauntletItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("flame");

                if (!ability.canPlayerUse(player))
                    continue;

                var fireDuration = Math.max(0D, ability.getStatData("fire_duration").getValue());

                if (fireDuration > 0D)
                    igniteFromGauntlet(target, player, fireDuration);

                if (!wasOnFire)
                    continue;

                if (ability.isRankModifierUnlocked("burning_damage")) {
                    var value = Math.max(0D, Math.min(1D, ability.getStatData("burning_damage_bonus").getValue()));

                    burningDamageBonus = Math.max(burningDamageBonus, value);
                }

                if (ability.isRankModifierUnlocked("spread")) {
                    spreadRadius = Math.max(spreadRadius, Math.max(0D, ability.getStatData("spread_radius").getValue()));
                    spreadFireDuration = Math.max(spreadFireDuration, Math.max(0D, ability.getStatData("spread_fire_duration").getValue()));
                }
            }

            if (burningDamageBonus > 0D)
                event.setAmount((float) (event.getAmount() * (1D + burningDamageBonus)));

            if (spreadRadius <= 0D || spreadFireDuration <= 0D || !wasOnFire)
                return;

            var maxDistanceSq = spreadRadius * spreadRadius;

            for (var nearby : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(spreadRadius), entity -> entity.isAlive() && entity != target && entity != player)) {
                if (nearby.distanceToSqr(target) > maxDistanceSq)
                    continue;

                igniteFromGauntlet(nearby, player, spreadFireDuration);
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            var entity = event.getEntity();
            var level = entity.level();

            if (level.isClientSide() || !entity.isOnFire())
                return;

            var data = entity.getPersistentData();

            if (!data.hasUUID("rarcompat_fire_gauntlet_owner"))
                return;

            var expireTick = data.getLong("rarcompat_fire_gauntlet_expire");

            if (level.getGameTime() > expireTick)
                return;

            var owner = level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(data.getUUID("rarcompat_fire_gauntlet_owner"));

            if (owner == null || !owner.isAlive())
                return;

            var sparkCount = 0;
            var sparkDamage = 0D;
            var sparkFireDuration = 0D;
            ItemStack sparkStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(owner, ModItems.FIRE_GAUNTLET.value())) {
                if (!(stack.getItem() instanceof FireGauntletItem relic))
                    continue;

                var ability = relic.getRelicData(owner, stack).getAbilitiesData().getAbilityData("flame");

                if (!ability.canPlayerUse(owner) || !ability.isRankModifierUnlocked("spark"))
                    continue;

                var count = Math.max(0, (int) MathUtils.round(ability.getStatData("spark_count").getValue(), 0));
                var damage = Math.max(0D, ability.getStatData("spark_damage").getValue());
                var fireDuration = Math.max(0D, ability.getStatData("spark_fire_duration").getValue());

                if (count > sparkCount) {
                    sparkCount = count;
                    sparkStack = stack.copy();
                }

                sparkDamage = Math.max(sparkDamage, damage);
                sparkFireDuration = Math.max(sparkFireDuration, fireDuration);
            }

            if (sparkCount <= 0 || sparkDamage <= 0D || sparkStack.isEmpty())
                return;

            var targets = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(10D), target -> target.isAlive() && target != owner && target != entity)
                    .stream()
                    .sorted(Comparator.comparingDouble(target -> target.distanceToSqr(entity)))
                    .limit(sparkCount)
                    .toList();

            if (targets.isEmpty())
                return;

            var startPos = entity.position().add(0D, entity.getBbHeight() / 2F, 0D);

            for (var target : targets) {
                var spark = new SparkEntity(EntityRegistry.SPARK.get(), level);

                spark.setOwner(owner);
                spark.setTarget(target);
                spark.setDamage((float) sparkDamage);
                spark.setFireDuration((float) sparkFireDuration);
                spark.setRelicStack(sparkStack.copy());
                spark.setPos(startPos.x(), startPos.y(), startPos.z());

                var direction = target.position().add(0D, target.getBbHeight() / 2F, 0D).subtract(startPos);

                if (direction.lengthSqr() > 1.0E-6D)
                    spark.setDeltaMovement(direction.normalize().scale(0.2D));

                level.addFreshEntity(spark);
            }
        }
    }
}