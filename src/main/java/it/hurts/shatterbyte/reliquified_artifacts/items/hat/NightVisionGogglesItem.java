package it.hurts.shatterbyte.reliquified_artifacts.items.hat;

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
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NightVisionGogglesItem extends RAWearableRelicItem {
    private static final Map<UUID, Integer> DARK_MOVEMENT_TICKS = new HashMap<>();

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("vision")
                                .modes("enabled", "disabled")
                                .rankModifier(1, "clarity")
                                .rankModifier(3, "evasion")
                                .rankModifier(5, "ambush")
                                .stat(AbilityStatTemplate.builder("amount")
                                        .initialValue(0.05D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0857D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("miss_chance")
                                        .initialValue(0.05D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("damage_bonus")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1755D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("vision").build())
                                        .source(ExperienceSourceTemplate.builder("evasion_miss")
                                                .rankModifierVisibilityState("evasion", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("darkness_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("evasion_misses")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("evasion", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("evasion_damage_avoided")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("evasion", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("ambush_bonus_damage")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("ambush", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .leveling(LevelingTemplate.builder()
                        .initialCost(100)
                        .step(100)
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.CAVE, LootEntries.MINESHAFT)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player))
            return;

        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("vision");
        var enabled = ability.getMode().equals("enabled");

        if (enabled) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 10, 0, false, false));
        } else if (isNightVision(player.getActiveEffects())) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }

        if (player.level().isClientSide())
            return;

        var playerId = player.getUUID();

        if (!ability.canPlayerUse(player) || !enabled) {
            DARK_MOVEMENT_TICKS.remove(playerId);
            return;
        }

        var darknessFactor = getDarknessFactor(player);

        if (darknessFactor <= 0D)
            return;

        if (player.tickCount % 20 == 0)
            ability.getStatisticData().getMetricData("darkness_duration").addValue(1D);

        if (!isMoving(player))
            return;

        var movementTicks = DARK_MOVEMENT_TICKS.getOrDefault(playerId, 0) + 1;

        while (movementTicks >= 100) {
            relicData.getLevelingData().addExperience("vision", "vision", 1D);
            movementTicks -= 100;
        }

        DARK_MOVEMENT_TICKS.put(playerId, movementTicks);
    }

    public boolean isNightVision(Collection<MobEffectInstance> activeEffects) {
        return !activeEffects.isEmpty() && activeEffects.stream().anyMatch(mobEffectInstance -> mobEffectInstance.is(MobEffects.NIGHT_VISION)
                && mobEffectInstance.getDuration() <= 10);
    }

    private static double getDarknessFactor(LivingEntity entity) {
        var level = entity.getCommandSenderWorld();
        var pos = entity.blockPosition();

        var blockBrightness = level.getBrightness(LightLayer.BLOCK, pos);
        var skyBrightness = level.getBrightness(LightLayer.SKY, pos);
        var maxBrightness = Math.max(1, level.getMaxLightLevel());
        var skyDarken = level.getSkyDarken();
        var maxSkyDarken = 11D;

        var normalizedBlock = Math.max(0D, Math.min(1D, blockBrightness / (double) maxBrightness));
        var normalizedSky = Math.max(0D, Math.min(1D, skyBrightness / (double) maxBrightness));
        var skyFactor = Math.max(0D, Math.min(1D, 1D - (skyDarken / maxSkyDarken)));

        // Any strong light source (sky or block) suppresses darkness effects.
        var ambientBrightness = Math.max(normalizedBlock, normalizedSky * skyFactor);
        var rawDarkness = Math.max(0D, Math.min(1D, 1D - ambientBrightness));

        // Effects start only in genuinely dark places.
        var activationThreshold = 0.5D;

        if (rawDarkness <= activationThreshold)
            return 0D;

        return Math.max(0D, Math.min(1D, (rawDarkness - activationThreshold) / (1D - activationThreshold)));
    }

    private static boolean isMoving(Player player) {
        var motion = player.getDeltaMovement();

        return motion.x() * motion.x() + motion.z() * motion.z() > 1.0E-4D;
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || !(event.getSource().getEntity() instanceof LivingEntity))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.NIGHT_VISION_GOGGLES.value());

            if (!(stack.getItem() instanceof NightVisionGogglesItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("vision");

            if (!ability.canPlayerUse(player) || !ability.getMode().equals("enabled") || !ability.getRankModifierData("evasion").isUnlocked())
                return;

            var baseChance = Math.max(0D, Math.min(1D, ability.getStatData("miss_chance").getValue()));

            if (baseChance <= 0D)
                return;

            var chance = baseChance * getDarknessFactor(player);

            if (chance > 0D && player.getRandom().nextDouble() <= chance) {
                event.setCanceled(true);
                ability.getStatisticData().getMetricData("evasion_misses").addValue(1D);
                ability.getStatisticData().getMetricData("evasion_damage_avoided").addValue(event.getAmount());
                relic.getRelicData(player, stack).getLevelingData().addExperience("vision", "evasion_miss", 1D);
            }
        }

        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide() || event.getEntity() == player)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.NIGHT_VISION_GOGGLES.value());

            if (!(stack.getItem() instanceof NightVisionGogglesItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("vision");

            if (!ability.canPlayerUse(player) || !ability.getMode().equals("enabled") || !ability.getRankModifierData("ambush").isUnlocked())
                return;

            var baseBonus = Math.max(0D, ability.getStatData("damage_bonus").getValue());

            if (baseBonus <= 0D)
                return;

            var bonus = baseBonus * getDarknessFactor(player);

            if (bonus > 0D) {
                var baseDamage = event.getAmount();
                var boostedDamage = (float) (baseDamage * (1D + bonus));
                var additionalDamage = Math.max(0F, boostedDamage - baseDamage);

                event.setAmount(boostedDamage);

                if (additionalDamage > 0F)
                    ability.getStatisticData().getMetricData("ambush_bonus_damage").addValue(additionalDamage);
            }
        }
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onRenderFog(ViewportEvent.RenderFog event) {
            if (!(event.getCamera().getEntity() instanceof Player player))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.NIGHT_VISION_GOGGLES.value());

            if (!(stack.getItem() instanceof NightVisionGogglesItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("vision");

            if (!ability.canPlayerUse(player) || !ability.getMode().equals("enabled") || !ability.getRankModifierData("clarity").isUnlocked())
                return;

            event.setNearPlaneDistance(-8F);
            event.setFarPlaneDistance(Math.max(event.getFarPlaneDistance(), 512F));
            event.setCanceled(true);
        }
    }
}

