package it.hurts.shatterbyte.reliquified_artifacts.items.charm;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
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
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class WarpDriveItem extends RAWearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("warp")
                                .rankModifier(1, "protection")
                                .rankModifier(3, "disorientation")
                                .rankModifier(5, "weightless")
                                .stat(AbilityStatTemplate.builder("cooldown")
                                        .initialValue(3.5D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.0257D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("blind_radius")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("blind_duration")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("pearl_use").build())
                                        .source(ExperienceSourceTemplate.builder("disorient_target")
                                                .rankModifierVisibilityState("disorientation", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("pearl_uses")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("protection_damage_avoided")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("protection", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("disoriented_targets")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("disorientation", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.THE_END, LootEntries.END_LIKE)
                        .build())
                .build();
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(0, (int) Math.round(Math.max(0D, seconds) * 20D));
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
        public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            var player = event.getEntity();
            var stack = event.getItemStack();

            if (!stack.is(Items.ENDER_PEARL))
                return;

            var relicStack = EntityUtils.findEquippedCurio(player, ModItems.WARP_DRIVE.value());

            if (!(relicStack.getItem() instanceof WarpDriveItem relic))
                return;

            var relicData = relic.getRelicData(player, relicStack);
            var ability = relicData.getAbilitiesData().getAbilityData("warp");

            if (!ability.canPlayerUse(player))
                return;

            if (player.getCooldowns().isOnCooldown(Items.ENDER_PEARL))
                return;

            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);

            if (player.level().isClientSide())
                return;

            var level = player.level();
            var pearl = new ThrownEnderpearl(level, player);
            var visualStack = stack.copy();

            visualStack.setCount(1);
            pearl.setItem(visualStack);

            if (ability.getRankModifierData("weightless").isEnabled())
                pearl.setNoGravity(true);

            pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0F, 1.5F, 1F);
            level.addFreshEntity(pearl);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_PEARL_THROW, SoundSource.NEUTRAL, 0.5F,
                    0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

            var cooldownTicks = secondsToTicks(ability.getStatData("cooldown").getValue());

            if (cooldownTicks > 0)
                player.getCooldowns().addCooldown(Items.ENDER_PEARL, cooldownTicks);

            player.awardStat(Stats.ITEM_USED.get(Items.ENDER_PEARL));
            relicData.getLevelingData().addExperience("warp", "pearl_use", 1D);
            ability.getStatisticData().getMetricData("pearl_uses").addValue(1D);
        }

        @SubscribeEvent
        public static void onEnderPearlTeleport(EntityTeleportEvent.EnderPearl event) {
            var player = event.getPlayer();

            if (player.level().isClientSide())
                return;

            var relicStack = EntityUtils.findEquippedCurio(player, ModItems.WARP_DRIVE.value());

            if (!(relicStack.getItem() instanceof WarpDriveItem relic))
                return;

            var relicData = relic.getRelicData(player, relicStack);
            var ability = relicData.getAbilitiesData().getAbilityData("warp");

            if (!ability.canPlayerUse(player))
                return;

            if (ability.getRankModifierData("protection").isEnabled()) {
                var pearlDamage = Math.max(0F, event.getAttackDamage());

                if (pearlDamage > 0F) {
                    event.setAttackDamage(0F);
                    ability.getStatisticData().getMetricData("protection_damage_avoided").addValue(pearlDamage);
                }
            }

            if (!ability.getRankModifierData("disorientation").isEnabled())
                return;

            var radius = Math.max(0D, ability.getStatData("blind_radius").getValue());
            var durationTicks = secondsToTicks(ability.getStatData("blind_duration").getValue());
            var affectedTargets = applyBlindnessAndForget(player, event.getTarget(), radius, durationTicks);

            if (affectedTargets > 0) {
                relicData.getLevelingData().addExperience("warp", "disorient_target", affectedTargets);
                ability.getStatisticData().getMetricData("disoriented_targets").addValue(affectedTargets);
            }
        }
    }
}
