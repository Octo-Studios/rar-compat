package it.hurts.shatterbyte.reliquified_artifacts.items.hat;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import artifacts.registry.ModSoundEvents;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.handlers.KnockbackHelper;
import it.hurts.shatterbyte.reliquified_artifacts.entities.WhoopeeCloudEntity;
import it.hurts.shatterbyte.reliquified_artifacts.init.RADataComponent;
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
import it.hurts.sskirillss.relics.init.RelicsMobEffects;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.HashSet;

public class WhoopeeCushionItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("push")
                                .rankModifier(1, "retaliation")
                                .rankModifier(3, "toxic_cloud")
                                .rankModifier(5, "paralysis")
                                .stat(AbilityStatTemplate.builder("chance")
                                        .initialValue(0.1D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0531D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("distance")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.019D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("retaliation_chance")
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("paralysis_radius")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.019D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("paralysis_duration")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.019D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("cloud_spawn_chance")
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("activation").build())
                                        .source(ExperienceSourceTemplate.builder("cloud_created")
                                                .rankModifierVisibilityState("toxic_cloud", VisibilityState.OBFUSCATED)
                                                .build())
                                        .source(ExperienceSourceTemplate.builder("paralyzed_target")
                                                .rankModifierVisibilityState("paralysis", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("triggers")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("targets_hit")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("clouds_created")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("toxic_cloud", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.VILLAGE)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("push");

        if (!ability.canPlayerUse(player)) {
            stack.set(RADataComponent.TOGGLED, false);
            return;
        }

        var crouching = player.isCrouching();
        var wasCrouching = stack.getOrDefault(RADataComponent.TOGGLED, false);

        if (crouching && !wasCrouching) {
            var chance = Math.max(0D, Math.min(1D, ability.getStatData("chance").getValue()));

            if (chance > 0D && player.getRandom().nextDouble() <= chance)
                activateAbility(player, stack);
        }

        stack.set(RADataComponent.TOGGLED, crouching);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        stack.set(RADataComponent.TOGGLED, false);
    }

    public void activateFromCloudSynergy(Player player, ItemStack stack) {
        if (player.level().isClientSide())
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("push");

        if (!ability.canPlayerUse(player))
            return;

        activateAbility(player, stack);
    }

    private void activateAbility(Player player, ItemStack stack) {
        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("push");

        if (!ability.canPlayerUse(player))
            return;

        var distance = Math.max(0D, ability.getStatData("distance").getValue());

        if (distance <= 0D)
            return;

        var level = player.level();
        var affectedTargets = new HashSet<java.util.UUID>();
        var cloudsCreated = 0;
        var paralyzedTargets = 0;

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSoundEvents.FART, SoundSource.PLAYERS, 1F,
                0.9F + player.getRandom().nextFloat() * 0.2F);

        var maxDistanceSq = distance * distance;
        var strength = Math.max(0.2D, distance * 0.35D);

        for (var target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(distance), entity -> entity.isAlive() && entity != player)) {
            if (target.distanceToSqr(player) > maxDistanceSq)
                continue;

            KnockbackHelper.apply(target, strength, new Vec3(player.getX() - target.getX(), player.getY() - target.getY(), player.getZ() - target.getZ()));
            affectedTargets.add(target.getUUID());

            if (target instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }

        if (ability.isRankModifierUnlocked("toxic_cloud")) {
            var spawnCloud = true;

            if (ability.isRankModifierUnlocked("paralysis")) {
                var cloudChance = Math.max(0D, Math.min(1D, ability.getStatData("cloud_spawn_chance").getValue()));
                spawnCloud = player.getRandom().nextDouble() <= cloudChance;
            }

            if (spawnCloud) {
                spawnToxicCloud(player, Math.max(1.5F, (float) Math.min(6D, distance * 0.75D)));
                cloudsCreated = 1;
            }
        }

        if (ability.isRankModifierUnlocked("paralysis")) {
            var radius = Math.max(0D, ability.getStatData("paralysis_radius").getValue());
            var durationTicks = secondsToTicks(ability.getStatData("paralysis_duration").getValue());

            if (radius > 0D && durationTicks > 0) {
                var maxRadiusSq = radius * radius;

                for (var target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), entity -> entity.isAlive() && entity != player)) {
                    if (target.distanceToSqr(player) > maxRadiusSq)
                        continue;

                    if (target.addEffect(new MobEffectInstance(RelicsMobEffects.PARALYSIS, durationTicks, 0, false, true)))
                        paralyzedTargets++;

                    affectedTargets.add(target.getUUID());

                    if (target instanceof Mob mob) {
                        mob.setTarget(null);
                        mob.getNavigation().stop();
                    }
                }
            }
        }

        relicData.getLevelingData().addExperience("push", "activation", 1D);
        ability.getStatisticData().getMetricData("triggers").addValue(1D);

        if (!affectedTargets.isEmpty())
            ability.getStatisticData().getMetricData("targets_hit").addValue(affectedTargets.size());

        if (cloudsCreated > 0) {
            relicData.getLevelingData().addExperience("push", "cloud_created", cloudsCreated);
            ability.getStatisticData().getMetricData("clouds_created").addValue(cloudsCreated);
        }

        if (paralyzedTargets > 0)
            relicData.getLevelingData().addExperience("push", "paralyzed_target", paralyzedTargets);
    }

    private static void spawnToxicCloud(Player player, float radius) {
        var level = player.level();
        var cloud = new WhoopeeCloudEntity(RAEntities.WHOOPEE_CLOUD.get(), level);

        cloud.setPos(player.getX(), player.getY(), player.getZ());
        cloud.configure(player, Math.max(0.5F, radius), 120);

        level.addFreshEntity(cloud);
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(0, (int) Math.round(Math.max(0D, seconds) * 20D));
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingDamagePost(LivingDamageEvent.Post event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getNewDamage() <= 0F)
                return;

            var chance = 0D;
            WhoopeeCushionItem selectedRelic = null;
            ItemStack selectedStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.WHOOPEE_CUSHION.value())) {
                if (!(stack.getItem() instanceof WhoopeeCushionItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("push");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("retaliation"))
                    continue;

                var value = Math.max(0D, Math.min(1D, ability.getStatData("retaliation_chance").getValue()));

                if (value <= chance)
                    continue;

                chance = value;
                selectedRelic = relic;
                selectedStack = stack;
            }

            if (selectedRelic == null || chance <= 0D || player.getRandom().nextDouble() > chance)
                return;

            selectedRelic.activateAbility(player, selectedStack);
        }
    }
}
