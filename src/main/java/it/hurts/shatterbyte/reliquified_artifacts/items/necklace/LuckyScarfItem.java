package it.hurts.shatterbyte.reliquified_artifacts.items.necklace;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.init.RADataComponent;
import it.hurts.shatterbyte.reliquified_artifacts.items.base.RAWearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.AbilityMetricTemplate;
import it.hurts.sskirillss.relics.api.relics.AbilityStatisticTemplate;
import it.hurts.sskirillss.relics.api.relics.IRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourceTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.ExperienceSourcesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.api.relics.synergies.SynergyTemplate;
import it.hurts.sskirillss.relics.api.relics.synergies.conditions.AbilityConditionTemplate;
import it.hurts.sskirillss.relics.api.relics.synergies.conditions.RelicConditionTemplate;
import it.hurts.sskirillss.relics.api.relics.synergies.stats.SynergyStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsRelicContainers;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import top.theillusivec4.curios.api.SlotContext;

public class LuckyScarfItem extends RAWearableRelicItem {

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("fortune")
                                .rankModifier(1, "pity")
                                .rankModifier(3, "chain")
                                .rankModifier(5, "vein")
                                .stat(AbilityStatTemplate.builder("chance")
                                        .initialValue(0.05D, 0.15D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1143D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("max_casts")
                                        .initialValue(1D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.2571D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("pity_per_fail")
                                        .initialValue(0.01D, 0.05D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0571D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("chain_max_casts_per_proc")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0667D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("vein_efficiency_per_block")
                                        .initialValue(0.01D, 0.05D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("chain_max_bonus_casts")
                                        .initialValue(1D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1143D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("bonus_fortune").build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("bonus_fortune_procs")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .build())
                                .build())
                        .synergy(SynergyTemplate.builder("chest_mastery")
                                .stat(SynergyStatTemplate.builder("amplifier")
                                        .thresholdValue(0D, 49D)
                                        .formatValue(value -> (int) MathUtils.round(value + 1, 0))
                                        .build())
                                .condition(RelicConditionTemplate.builder(() -> (IRelicItem) ModItems.LUCKY_SCARF.value())
                                        .container(RelicsRelicContainers.CURIOS.get())
                                        .condition(AbilityConditionTemplate.builder("fortune").build())
                                        .build())
                                .condition(RelicConditionTemplate.builder(() -> (IRelicItem) ModItems.SUPERSTITIOUS_HAT.value())
                                        .container(RelicsRelicContainers.CURIOS.get())
                                        .condition(AbilityConditionTemplate.builder("looting").build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.MINESHAFT, LootEntries.CAVE)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var abilities = this.getRelicData(player, stack).getAbilitiesData();
        var ability = abilities.getAbilityData("fortune");

        if (!ability.canPlayerUse(player))
            return;

        var synergy = abilities.getSynergyData("chest_mastery");

        if (!synergy.isUnlocked() || !synergy.isEnabled())
            return;

        var amplifier = Math.max(0, (int) MathUtils.round(synergy.getStatData("amplifier").getValue(), 0));

        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 40, amplifier, false, false));
    }

    @Override
    public int getFortuneLevel(SlotContext slotContext, LootContext lootContext, ItemStack stack) {
        var baseFortune = super.getFortuneLevel(slotContext, lootContext, stack);

        if (!(slotContext.entity() instanceof Player player) || lootContext == null)
            return baseFortune;

        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("fortune");

        if (!ability.canPlayerUse(player)) {
            setPityChanceBonus(stack, 0D);
            setNextMaxCastsBonus(stack, 0);
            resetFortuneRollState(stack);

            return baseFortune;
        }

        if (!isBlockDropContext(lootContext)) {
            resetFortuneRollState(stack);
            return baseFortune;
        }

        var blockPos = getContextBlockPos(lootContext);

        if (blockPos == null) {
            resetFortuneRollState(stack);
            return baseFortune;
        }

        if (blockPos.asLong() == getLastBlockPos(stack))
            return baseFortune + getLastFortuneBonus(stack);

        var chance = Math.max(0D, Math.min(1D, ability.getStatData("chance").getValue()));
        var maxCasts = Math.max(0, (int) MathUtils.round(ability.getStatData("max_casts").getValue(), 0));
        var chainMaxBonusCasts = Math.max(0, (int) MathUtils.round(ability.getStatData("chain_max_bonus_casts").getValue(), 0));

        if (chance <= 0D || maxCasts <= 0) {
            setLastBlockPos(stack, blockPos.asLong());
            setLastFortuneBonus(stack, 0);
            setPendingFortuneBonus(stack, 0);
            return baseFortune;
        }

        if (ability.getRankModifierData("pity").isUnlocked()) {
            chance = Math.max(0D, Math.min(1D, chance + getPityChanceBonus(stack)));
        } else {
            setPityChanceBonus(stack, 0D);
        }

        if (ability.getRankModifierData("chain").isUnlocked()) {
            maxCasts += Math.min(getNextMaxCastsBonus(stack), chainMaxBonusCasts);
        } else {
            setNextMaxCastsBonus(stack, 0);
        }

        if (maxCasts <= 0) {
            setLastBlockPos(stack, blockPos.asLong());
            setLastFortuneBonus(stack, 0);
            setPendingFortuneBonus(stack, 0);
            return baseFortune;
        }

        if (ability.getRankModifierData("vein").isUnlocked()) {
            var nearbySameBlocks = countNearbySameBlocks(slotContext, lootContext);

            if (nearbySameBlocks > 0) {
                var efficiencyPerBlock = Math.max(0D, ability.getStatData("vein_efficiency_per_block").getValue());

                if (efficiencyPerBlock > 0D)
                    chance = Math.max(0D, Math.min(1D, chance * (1D + efficiencyPerBlock * nearbySameBlocks)));
            }
        }

        var procs = MathUtils.multicast(player.getRandom(), chance, maxCasts);

        if (ability.getRankModifierData("pity").isUnlocked()) {
            if (procs > 0) {
                setPityChanceBonus(stack, 0D);
            } else {
                var pityPerFail = Math.max(0D, Math.min(1D, ability.getStatData("pity_per_fail").getValue()));

                setPityChanceBonus(stack, Math.min(1D, getPityChanceBonus(stack) + pityPerFail));
            }
        }

        if (ability.getRankModifierData("chain").isUnlocked()) {
            var chainPerProc = Math.max(0, (int) MathUtils.round(ability.getStatData("chain_max_casts_per_proc").getValue(), 0));
            var nextBonus = procs > 0 && chainPerProc > 0 ? procs * chainPerProc : 0;

            setNextMaxCastsBonus(stack, Math.min(nextBonus, chainMaxBonusCasts));
        }

        setLastBlockPos(stack, blockPos.asLong());
        setLastFortuneBonus(stack, procs);
        setPendingFortuneBonus(stack, procs);

        if (procs <= 0)
            return baseFortune;

        return baseFortune + procs;
    }

    private static boolean isBlockDropContext(LootContext lootContext) {
        var blockState = lootContext.getParamOrNull(LootContextParams.BLOCK_STATE);

        return blockState != null;
    }

    private static BlockPos getContextBlockPos(LootContext lootContext) {
        var origin = lootContext.getParamOrNull(LootContextParams.ORIGIN);

        return origin == null ? null : BlockPos.containing(origin);
    }

    private int countNearbySameBlocks(SlotContext slotContext, LootContext lootContext) {
        var blockState = lootContext.getParamOrNull(LootContextParams.BLOCK_STATE);
        var origin = lootContext.getParamOrNull(LootContextParams.ORIGIN);

        if (blockState == null || origin == null)
            return 0;

        var level = slotContext.entity().level();
        var originPos = BlockPos.containing(origin);
        var nearby = 0;

        for (var direction : Direction.values()) {
            if (level.getBlockState(originPos.relative(direction)).is(blockState.getBlock()))
                nearby++;
        }

        return nearby;
    }

    private double getPityChanceBonus(ItemStack stack) {
        return Math.max(0D, stack.getOrDefault(RADataComponent.LUCKY_SCARF_PITY_CHANCE_BONUS.get(), 0D));
    }

    private void setPityChanceBonus(ItemStack stack, double value) {
        stack.set(RADataComponent.LUCKY_SCARF_PITY_CHANCE_BONUS.get(), Math.max(0D, value));
    }

    private int getNextMaxCastsBonus(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(RADataComponent.LUCKY_SCARF_NEXT_MAX_CASTS_BONUS.get(), 0));
    }

    private void setNextMaxCastsBonus(ItemStack stack, int value) {
        stack.set(RADataComponent.LUCKY_SCARF_NEXT_MAX_CASTS_BONUS.get(), Math.max(0, value));
    }

    private long getLastBlockPos(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.LUCKY_SCARF_LAST_BLOCK_POS.get(), Long.MIN_VALUE);
    }

    private void setLastBlockPos(ItemStack stack, long value) {
        stack.set(RADataComponent.LUCKY_SCARF_LAST_BLOCK_POS.get(), value);
    }

    private int getLastFortuneBonus(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(RADataComponent.LUCKY_SCARF_LAST_FORTUNE_BONUS.get(), 0));
    }

    private void setLastFortuneBonus(ItemStack stack, int value) {
        stack.set(RADataComponent.LUCKY_SCARF_LAST_FORTUNE_BONUS.get(), Math.max(0, value));
    }

    private int getPendingFortuneBonus(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(RADataComponent.LUCKY_SCARF_PENDING_FORTUNE_BONUS.get(), 0));
    }

    private void setPendingFortuneBonus(ItemStack stack, int value) {
        stack.set(RADataComponent.LUCKY_SCARF_PENDING_FORTUNE_BONUS.get(), Math.max(0, value));
    }

    private void resetFortuneRollState(ItemStack stack) {
        setLastBlockPos(stack, Long.MIN_VALUE);
        setLastFortuneBonus(stack, 0);
        setPendingFortuneBonus(stack, 0);
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onBlockDrops(BlockDropsEvent event) {
            if (!(event.getBreaker() instanceof Player player) || player.level().isClientSide() || event.isCanceled() )
                return;

            var blockPos = event.getPos().asLong();

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.LUCKY_SCARF.value())) {
                if (!(stack.getItem() instanceof LuckyScarfItem relic))
                    continue;

                var pendingBonus = relic.getPendingFortuneBonus(stack);

                if (pendingBonus <= 0 || relic.getLastBlockPos(stack) != blockPos)
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("fortune");

                if (!ability.canPlayerUse(player) || event.getDrops().isEmpty()) {
                    relic.resetFortuneRollState(stack);
                    continue;
                }

                relic.getRelicData(player, stack).getLevelingData().addExperience("fortune", "bonus_fortune", pendingBonus);
                ability.getStatisticData().getMetricData("bonus_fortune_procs").addValue(pendingBonus);
                relic.resetFortuneRollState(stack);
            }
        }
    }
}

