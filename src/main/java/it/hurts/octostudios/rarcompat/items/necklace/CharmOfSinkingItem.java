package it.hurts.octostudios.rarcompat.items.necklace;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
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
import it.hurts.sskirillss.relics.init.RelicsMobEffects;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import top.theillusivec4.curios.api.SlotContext;

public class CharmOfSinkingItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("sinking")
                                .rankModifier(1, "fluid_collision")
                                .rankModifier(3, "resistance")
                                .rankModifier(5, "immortality")
                                .stat(AbilityStatTemplate.builder("air_restore_speed")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0667D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat(AbilityStatTemplate.builder("resistance_per_block")
                                        .initialValue(0.01D, 0.025D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> MathUtils.round(value * 100D, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("immortality_delay")
                                        .initialValue(10D, 15D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.0143D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("air_bubble").build())
                                        .source(ExperienceSourceTemplate.builder("blocked_damage")
                                                .rankModifierVisibilityState("resistance", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("air_bubbles_restored")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("damage_reduced")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("resistance", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("immortality_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("immortality", VisibilityState.OBFUSCATED)
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
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("sinking");

        if (!ability.canPlayerUse(player)) {
            resetImmobilityState(player, stack);
            setAirRestoreProgress(stack, 0D);
            return;
        }

        var onBottom = isStandingOnBottomUnderWater(player);

        if (onBottom && player.getAirSupply() < player.getMaxAirSupply()) {
            var maxAir = player.getMaxAirSupply();
            var beforeAir = player.getAirSupply();
            var beforeBubbles = getAirBubbleCount(beforeAir, maxAir);

            var airAfterDrainCompensation = Math.min(maxAir, beforeAir + 1);
            var airRestorePerSecond = Math.max(0D, ability.getStatData("air_restore_speed").getValue());
            var progress = getAirRestoreProgress(stack) + airRestorePerSecond / 20D;
            var extraAir = (int) Math.floor(progress);

            if (extraAir > 0)
                progress -= extraAir;

            var afterAir = Math.min(maxAir, airAfterDrainCompensation + Math.max(0, extraAir));
            var afterBubbles = getAirBubbleCount(afterAir, maxAir);
            var gainedBubbles = Math.max(0, afterBubbles - beforeBubbles);

            player.setAirSupply(afterAir);

            if (gainedBubbles > 0) {
                ability.getStatisticData().getMetricData("air_bubbles_restored").addValue(gainedBubbles);
                this.getRelicData(player, stack).getLevelingData().addExperience("sinking", "air_bubble", gainedBubbles);
            }

            setAirRestoreProgress(stack, progress);
        } else {
            setAirRestoreProgress(stack, 0D);
        }

        if (ability.isRankModifierUnlocked("fluid_collision") && player.isInWaterOrBubble())
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 10, 0, false, false));

        if (!ability.isRankModifierUnlocked("immortality")) {
            resetImmobilityState(player, stack);
            return;
        }

        if (!onBottom || player.getDeltaMovement().lengthSqr() > 1.0E-4D) {
            resetImmobilityState(player, stack);
            return;
        }

        addStationaryTicks(stack, 1);

        if (getStationaryTicks(stack) < getImmobilityDelayTicks(player, stack))
            return;

        player.addEffect(new MobEffectInstance(RelicsMobEffects.IMMORTALITY, 10, 0, false, false));
        setImmortalityActive(stack, true);

        if (player.tickCount % 20 == 0)
            ability.getStatisticData().getMetricData("immortality_duration").addValue(1D);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem() || !(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        resetImmobilityState(player, stack);
    }

    private void resetImmobilityState(Player player, ItemStack stack) {
        if (isImmortalityActive(stack))
            player.removeEffect(RelicsMobEffects.IMMORTALITY);

        setImmortalityActive(stack, false);
        setStationaryTicks(stack, 0);
    }

    private static boolean isStandingOnBottomUnderWater(Player player) {
        return player.isInWaterOrBubble() && player.isEyeInFluid(FluidTags.WATER) && player.onGround();
    }

    private static int countContinuousWaterAboveHead(Player player) {
        if (!player.isInWaterOrBubble() || !player.isEyeInFluid(FluidTags.WATER))
            return 0;

        var level = player.level();
        var start = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()).above();
        var depth = 0;

        for (var i = 0; i < 64 && start.getY() + i < level.getMaxBuildHeight(); i++) {
            var pos = start.above(i);

            if (!level.getFluidState(pos).is(FluidTags.WATER))
                break;

            depth++;
        }

        return depth;
    }

    private int getImmobilityDelayTicks(Player player, ItemStack stack) {
        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("sinking");
        var seconds = Math.max(0D, ability.getStatData("immortality_delay").getValue());

        return Math.max(1, (int) Math.round(seconds * 20D));
    }

    private static int getAirBubbleCount(int airSupply, int maxAirSupply) {
        if (maxAirSupply <= 0)
            return 0;

        var airPerBubble = Math.max(1, maxAirSupply / 10);
        var maxBubbles = Math.max(1, (int) Math.ceil(maxAirSupply / (double) airPerBubble));
        var visibleBubbles = (int) Math.ceil(Math.max(0, airSupply) / (double) airPerBubble);

        return Math.max(0, Math.min(maxBubbles, visibleBubbles));
    }

    private int getStationaryTicks(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.CHARM_OF_SINKING_STATIONARY_TICKS.get(), 0);
    }

    private void setStationaryTicks(ItemStack stack, int ticks) {
        stack.set(DataComponentRegistry.CHARM_OF_SINKING_STATIONARY_TICKS.get(), Math.max(0, ticks));
    }

    private void addStationaryTicks(ItemStack stack, int ticks) {
        setStationaryTicks(stack, getStationaryTicks(stack) + ticks);
    }

    private boolean isImmortalityActive(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.CHARM_OF_SINKING_IMMORTALITY_ACTIVE.get(), false);
    }

    private void setImmortalityActive(ItemStack stack, boolean active) {
        stack.set(DataComponentRegistry.CHARM_OF_SINKING_IMMORTALITY_ACTIVE.get(), active);
    }

    private double getAirRestoreProgress(ItemStack stack) {
        return Math.max(0D, stack.getOrDefault(DataComponentRegistry.CHARM_OF_SINKING_AIR_RESTORE_PROGRESS.get(), 0D));
    }

    private void setAirRestoreProgress(ItemStack stack, double progress) {
        stack.set(DataComponentRegistry.CHARM_OF_SINKING_AIR_RESTORE_PROGRESS.get(), Math.max(0D, progress));
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            var player = event.getEntity();

            if (player.level().isClientSide())
                return;

            resetImmobilityOnAction(player);
        }

        @SubscribeEvent
        public static void onAttackEntity(AttackEntityEvent event) {
            var player = event.getEntity();

            if (player.level().isClientSide())
                return;

            resetImmobilityOnAction(player);
        }

        @SubscribeEvent
        public static void onBlockBreakAttempt(PlayerEvent.BreakSpeed event) {
            var player = event.getEntity();

            if (player.level().isClientSide())
                return;

            resetImmobilityOnAction(player);
        }

        private static void resetImmobilityOnAction(Player player) {
            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.CHARM_OF_SINKING.value())) {
                if (stack.getItem() instanceof CharmOfSinkingItem relic)
                    relic.resetImmobilityState(player, stack);
            }
        }

        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F || !player.isInWaterOrBubble())
                return;

            var waterDepth = countContinuousWaterAboveHead(player);

            if (waterDepth <= 0)
                return;

            var reduction = 0D;
            CharmOfSinkingItem bestRelic = null;
            ItemStack bestStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.CHARM_OF_SINKING.value())) {
                if (!(stack.getItem() instanceof CharmOfSinkingItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("sinking");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("resistance"))
                    continue;

                var perBlock = Math.max(0D, Math.min(1D, ability.getStatData("resistance_per_block").getValue()));
                var value = Math.min(1D, perBlock * waterDepth);

                if (value > reduction) {
                    reduction = value;
                    bestRelic = relic;
                    bestStack = stack;
                }
            }

            if (reduction <= 0D)
                return;

            var baseDamage = event.getAmount();
            var reducedDamage = (float) Math.max(0D, baseDamage * (1D - reduction));
            var blockedDamage = Math.max(0F, baseDamage - reducedDamage);

            event.setAmount(reducedDamage);

            if (bestRelic != null && blockedDamage > 0F) {
                var relicData = bestRelic.getRelicData(player, bestStack);
                var ability = relicData.getAbilitiesData().getAbilityData("sinking");

                relicData.getLevelingData().addExperience("sinking", "blocked_damage", blockedDamage);
                ability.getStatisticData().getMetricData("damage_reduced").addValue(blockedDamage);
            }
        }
    }
}
