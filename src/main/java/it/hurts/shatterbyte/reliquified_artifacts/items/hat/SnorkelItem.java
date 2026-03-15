package it.hurts.shatterbyte.reliquified_artifacts.items.hat;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.init.RADataComponent;
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
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SnorkelItem extends WearableRelicItem {
    private static final Map<UUID, Integer> LAST_UNDERWATER_AIR = new HashMap<>();

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("snorkeling")
                                .rankModifier(1, "vision")
                                .rankModifier(3, "reserve")
                                .rankModifier(5, "resistance")
                                .stat(AbilityStatTemplate.builder("water_depth")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1143D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("reserve_duration")
                                        .initialValue(5D, 10D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1429D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("drowning_resistance")
                                        .initialValue(0.05D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("air_spent").build())
                                        .source(ExperienceSourceTemplate.builder("reserve_unique")
                                                .rankModifierVisibilityState("reserve", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("safe_underwater_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("reserve_breathing_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("reserve", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("drowning_damage_reduced")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("resistance", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.AQUATIC)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        var entity = slotContext.entity();

        if (!(entity instanceof Player player) || player.level().isClientSide())
            return;

        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("snorkeling");
        var playerId = player.getUUID();
        var underwater = player.isEyeInFluid(FluidTags.WATER);
        var previousAir = LAST_UNDERWATER_AIR.get(playerId);

        if (!ability.canPlayerUse(player)) {
            LAST_UNDERWATER_AIR.remove(playerId);
            return;
        }

        var maxWaterDepth = Math.max(0, (int) MathUtils.round(ability.getStatData("water_depth").getValue(), 0));
        var isSafeHeight = isSafeBreathingHeight(player, maxWaterDepth);

        if (isSafeHeight) {
            setReserveTriggered(stack, false);

            if (underwater && player.tickCount % 20 == 0)
                ability.getStatisticData().getMetricData("safe_underwater_duration").addValue(1D);

            if (underwater && player.getAirSupply() < player.getMaxAirSupply())
                player.setAirSupply(Math.min(player.getAirSupply() + 4, player.getMaxAirSupply()));

            if (underwater) {
                var spentBubbles = getSpentBubbles(previousAir, player.getAirSupply(), player.getMaxAirSupply());

                if (spentBubbles > 0)
                    relicData.getLevelingData().addExperience("snorkeling", "air_spent", spentBubbles);

                LAST_UNDERWATER_AIR.put(playerId, player.getAirSupply());
            } else {
                LAST_UNDERWATER_AIR.remove(playerId);
            }

            return;
        }

        if (!ability.isRankModifierUnlocked("reserve") || isReserveTriggered(stack)) {
            if (underwater) {
                var spentBubbles = getSpentBubbles(previousAir, player.getAirSupply(), player.getMaxAirSupply());

                if (spentBubbles > 0)
                    relicData.getLevelingData().addExperience("snorkeling", "air_spent", spentBubbles);

                LAST_UNDERWATER_AIR.put(playerId, player.getAirSupply());
            } else {
                LAST_UNDERWATER_AIR.remove(playerId);
            }

            return;
        }

        var durationTicks = getReserveDurationTicks(player, stack);

        if (durationTicks > 0) {
            var beforeEffect = player.getEffect(MobEffects.WATER_BREATHING);
            var hadWaterBreathing = beforeEffect != null;
            var beforeDuration = beforeEffect != null ? beforeEffect.getDuration() : 0;

            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, durationTicks, 0, false, false));

            var afterEffect = player.getEffect(MobEffects.WATER_BREATHING);
            var afterDuration = afterEffect != null ? afterEffect.getDuration() : 0;
            var gainedTicks = Math.max(0, afterDuration - beforeDuration);

            if (gainedTicks > 0)
                ability.getStatisticData().getMetricData("reserve_breathing_duration").addValue(gainedTicks / 20D);

            if (!hadWaterBreathing && afterDuration > 0)
                relicData.getLevelingData().addExperience("snorkeling", "reserve_unique", 1D);
        }

        setReserveTriggered(stack, true);

        if (underwater) {
            var spentBubbles = getSpentBubbles(previousAir, player.getAirSupply(), player.getMaxAirSupply());

            if (spentBubbles > 0)
                relicData.getLevelingData().addExperience("snorkeling", "air_spent", spentBubbles);

            LAST_UNDERWATER_AIR.put(playerId, player.getAirSupply());
        } else {
            LAST_UNDERWATER_AIR.remove(playerId);
        }
    }
    private boolean isSafeBreathingHeight(Player player, int maxWaterDepth) {
        if (!player.isEyeInFluid(FluidTags.WATER))
            return true;

        var level = player.level();
        var eyePos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());

        for (var depth = 1; depth <= maxWaterDepth + 1; depth++) {
            var pos = eyePos.above(depth);
            var blockState = level.getBlockState(pos);

            if (blockState.isAir())
                return true;

            if (!blockState.is(Blocks.WATER))
                return false;
        }

        return false;
    }

    private int getReserveDurationTicks(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("snorkeling");

        if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("reserve"))
            return 0;

        var seconds = Math.max(0D, ability.getStatData("reserve_duration").getValue());

        return Math.max(0, (int) Math.round(seconds * 20D));
    }

    private static int getAirBubbleCount(int airSupply, int maxAirSupply) {
        if (maxAirSupply <= 0)
            return 0;

        var airPerBubble = Math.max(1, maxAirSupply / 10);

        return Math.max(0, airSupply / airPerBubble);
    }

    private static int getSpentBubbles(Integer previousAir, int currentAir, int maxAirSupply) {
        if (previousAir == null)
            return 0;

        var previousBubbles = getAirBubbleCount(previousAir, maxAirSupply);
        var currentBubbles = getAirBubbleCount(currentAir, maxAirSupply);

        return Math.max(0, previousBubbles - currentBubbles);
    }
    private boolean isReserveTriggered(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.SNORKEL_RESERVE_TRIGGERED.get(), false);
    }

    private void setReserveTriggered(ItemStack stack, boolean triggered) {
        stack.set(RADataComponent.SNORKEL_RESERVE_TRIGGERED.get(), triggered);
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || !event.getSource().is(DamageTypes.DROWN))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.SNORKEL.value());

            if (!(stack.getItem() instanceof SnorkelItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("snorkeling");

            if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("resistance"))
                return;

            var reduction = Math.max(0D, Math.min(1D, ability.getStatData("drowning_resistance").getValue()));

            if (reduction <= 0D)
                return;

            var incomingDamage = event.getAmount();
            var reducedDamage = (float) Math.max(0D, incomingDamage * (1D - reduction));

            event.setAmount(reducedDamage);

            var preventedDamage = Math.max(0F, incomingDamage - reducedDamage);

            if (preventedDamage > 0F)
                ability.getStatisticData().getMetricData("drowning_damage_reduced").addValue(preventedDamage);
        }
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onRenderFog(ViewportEvent.RenderFog event) {
            if (event.getType() != FogType.WATER || !(event.getCamera().getEntity() instanceof Player player))
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.SNORKEL.value());

            if (!(stack.getItem() instanceof SnorkelItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("snorkeling");

            if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("vision"))
                return;

            event.setNearPlaneDistance(-8F);
            event.setFarPlaneDistance(Math.max(event.getFarPlaneDistance(), 512F));
            event.setCanceled(true);
        }
    }
}