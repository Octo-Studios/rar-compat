package it.hurts.shatterbyte.reliquified_artifacts.items.hands;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.entities.SparkEntity;
import it.hurts.shatterbyte.reliquified_artifacts.init.RAEntities;
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
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1429D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("burning_damage_bonus")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0531D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("spread_radius")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1429D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("spread_fire_duration")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("spark_count")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0667D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("spark_damage")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0667D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("spark_fire_duration")
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("ignite_new_target").build())
                                        .source(ExperienceSourceTemplate.builder("spark_damage_hit")
                                                .rankModifierVisibilityState("spark", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("ignited_targets")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("spark_damage_dealt")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("spark", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("sparks_created")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("spark", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.THE_NETHER, LootEntries.NETHER_LIKE)
                        .build())
                .build();
    }

    private static long secondsToTicks(double seconds) {
        return Math.max(0L, Math.round(Math.max(0D, seconds) * 20D));
    }

    private static ItemStack findPreferredGauntletStack(Player owner, boolean requireSparkRank) {
        ItemStack selected = ItemStack.EMPTY;
        var selectedScore = -1D;

        for (var equipped : EntityUtils.findEquippedCurios(owner, ModItems.FIRE_GAUNTLET.value())) {
            if (!(equipped.getItem() instanceof FireGauntletItem relic))
                continue;

            var ability = relic.getRelicData(owner, equipped).getAbilitiesData().getAbilityData("flame");

            if (!ability.canPlayerUse(owner))
                continue;

            if (requireSparkRank && !ability.isRankModifierUnlocked("spark"))
                continue;

            var score = requireSparkRank
                    ? ability.getStatData("spark_count").getValue() * 100D + ability.getStatData("spark_damage").getValue()
                    : ability.getStatData("fire_duration").getValue();

            if (score > selectedScore) {
                selectedScore = score;
                selected = equipped;
            }
        }

        return selected;
    }

    private static ItemStack resolveAwardStack(Player owner, ItemStack stack, boolean requireSparkRank) {
        for (var equipped : EntityUtils.findEquippedCurios(owner, ModItems.FIRE_GAUNTLET.value())) {
            if (equipped != stack || !(equipped.getItem() instanceof FireGauntletItem relic))
                continue;

            var ability = relic.getRelicData(owner, equipped).getAbilitiesData().getAbilityData("flame");

            if (!ability.canPlayerUse(owner))
                continue;

            if (requireSparkRank && !ability.isRankModifierUnlocked("spark"))
                continue;

            return equipped;
        }

        return findPreferredGauntletStack(owner, requireSparkRank);
    }

    public static boolean igniteFromGauntlet(LivingEntity target, Player owner, double seconds) {
        if (seconds <= 0D)
            return false;

        var newlyIgnited = !target.isOnFire();

        target.igniteForSeconds((float) seconds);

        var data = target.getPersistentData();
        var expireTick = owner.level().getGameTime() + Math.max(1L, secondsToTicks(seconds));

        if (data.hasUUID("reliquified_artifacts_fire_gauntlet_owner") && owner.getUUID().equals(data.getUUID("reliquified_artifacts_fire_gauntlet_owner")))
            expireTick = Math.max(expireTick, data.getLong("reliquified_artifacts_fire_gauntlet_expire"));

        data.putUUID("reliquified_artifacts_fire_gauntlet_owner", owner.getUUID());
        data.putLong("reliquified_artifacts_fire_gauntlet_expire", expireTick);

        return newlyIgnited;
    }

    public static void awardIgniteProgress(Player owner, ItemStack stack) {
        var trackedStack = resolveAwardStack(owner, stack, false);

        if (!(trackedStack.getItem() instanceof FireGauntletItem relic))
            return;

        var relicData = relic.getRelicData(owner, trackedStack);
        var ability = relicData.getAbilitiesData().getAbilityData("flame");

        if (!ability.canPlayerUse(owner))
            return;

        relicData.getLevelingData().addExperience("flame", "ignite_new_target", 1D);
        ability.getStatisticData().getMetricData("ignited_targets").addValue(1D);
    }

    public static void awardSparkIgniteProgress(Player owner) {
        var trackedStack = findPreferredGauntletStack(owner, true);

        if (trackedStack.isEmpty())
            return;

        awardIgniteProgress(owner, trackedStack);
    }

    public static void awardSparkHitProgress(Player owner, ItemStack stack, float damageDealt) {
        var trackedStack = resolveAwardStack(owner, stack, true);

        if (!(trackedStack.getItem() instanceof FireGauntletItem relic))
            return;

        var relicData = relic.getRelicData(owner, trackedStack);
        var ability = relicData.getAbilitiesData().getAbilityData("flame");

        if (!ability.canPlayerUse(owner) || !ability.isRankModifierUnlocked("spark"))
            return;

        relicData.getLevelingData().addExperience("flame", "spark_damage_hit", 1D);

        if (damageDealt > 0F)
            ability.getStatisticData().getMetricData("spark_damage_dealt").addValue(damageDealt);
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
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
            FireGauntletItem spreadRelic = null;
            ItemStack spreadStack = ItemStack.EMPTY;
            var spreadSourceRadius = 0D;
            var spreadSourceDuration = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.FIRE_GAUNTLET.value())) {
                if (!(stack.getItem() instanceof FireGauntletItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("flame");

                if (!ability.canPlayerUse(player))
                    continue;

                var fireDuration = Math.max(0D, ability.getStatData("fire_duration").getValue());

                if (fireDuration > 0D && igniteFromGauntlet(target, player, fireDuration))
                    awardIgniteProgress(player, stack);

                if (!wasOnFire)
                    continue;

                if (ability.isRankModifierUnlocked("burning_damage")) {
                    var value = Math.max(0D, Math.min(1D, ability.getStatData("burning_damage_bonus").getValue()));

                    burningDamageBonus = Math.max(burningDamageBonus, value);
                }

                if (ability.isRankModifierUnlocked("spread")) {
                    var localSpreadRadius = Math.max(0D, ability.getStatData("spread_radius").getValue());
                    var localSpreadFireDuration = Math.max(0D, ability.getStatData("spread_fire_duration").getValue());

                    spreadRadius = Math.max(spreadRadius, localSpreadRadius);
                    spreadFireDuration = Math.max(spreadFireDuration, localSpreadFireDuration);

                    if (localSpreadRadius > spreadSourceRadius || localSpreadRadius == spreadSourceRadius && localSpreadFireDuration > spreadSourceDuration) {
                        spreadSourceRadius = localSpreadRadius;
                        spreadSourceDuration = localSpreadFireDuration;
                        spreadRelic = relic;
                        spreadStack = stack;
                    }
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

                if (igniteFromGauntlet(nearby, player, spreadFireDuration) && spreadRelic != null && !spreadStack.isEmpty())
                    awardIgniteProgress(player, spreadStack);
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            var entity = event.getEntity();
            var level = entity.level();

            if (level.isClientSide() || !entity.isOnFire())
                return;

            var data = entity.getPersistentData();

            if (!data.hasUUID("reliquified_artifacts_fire_gauntlet_owner"))
                return;

            var expireTick = data.getLong("reliquified_artifacts_fire_gauntlet_expire");

            if (level.getGameTime() > expireTick)
                return;

            var owner = level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(data.getUUID("reliquified_artifacts_fire_gauntlet_owner"));

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
                    sparkStack = stack;
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
            var spawnedSparks = 0;

            for (var target : targets) {
                var spark = new SparkEntity(RAEntities.SPARK.get(), level);

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
                spawnedSparks++;
            }

            if (spawnedSparks > 0 && sparkStack.getItem() instanceof FireGauntletItem relic) {
                var ability = relic.getRelicData(owner, sparkStack).getAbilitiesData().getAbilityData("flame");

                if (ability.canPlayerUse(owner) && ability.isRankModifierUnlocked("spark"))
                    ability.getStatisticData().getMetricData("sparks_created").addValue(spawnedSparks);
            }
        }
    }
}
