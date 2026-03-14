package it.hurts.octostudios.rarcompat.items.hands;

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
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import top.theillusivec4.curios.api.SlotContext;

public class PickaxeHeaterItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("heating")
                                .rankModifier(1, "blazing_strike")
                                .rankModifier(3, "overheat")
                                .rankModifier(5, "molten_luck")
                                .stat(AbilityStatTemplate.builder("smelt_chance")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0531D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("ignite_duration")
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.1429D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("stack_bonus")
                                        .initialValue(0.01D, 0.05D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("fortune_chance")
                                        .initialValue(0.05D, 0.1D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0429D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("fortune_max_casts")
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0286D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("smelted_block").build())
                                        .source(ExperienceSourceTemplate.builder("blazing_ignite")
                                                .rankModifierVisibilityState("blazing_strike", VisibilityState.OBFUSCATED)
                                                .build())
                                        .source(ExperienceSourceTemplate.builder("molten_luck_level")
                                                .rankModifierVisibilityState("molten_luck", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("smelted_blocks")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("blazing_ignites")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("blazing_strike", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("molten_luck_levels")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("molten_luck", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.THE_NETHER, LootEntries.MINESHAFT, LootEntries.NETHER_LIKE, LootEntries.CAVE)
                        .build())
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        setSmeltPityStacks(stack, 0);
        clearPendingSmeltRoll(stack);
    }

    @Override
    public int getFortuneLevel(SlotContext slotContext, LootContext lootContext, ItemStack stack) {
        var baseFortune = super.getFortuneLevel(slotContext, lootContext, stack);

        if (!(slotContext.entity() instanceof Player player) || lootContext == null)
            return baseFortune;

        var blockState = lootContext.getParamOrNull(LootContextParams.BLOCK_STATE);
        var tool = lootContext.getParamOrNull(LootContextParams.TOOL);

        if (blockState == null || !blockState.is(Tags.Blocks.ORES) || tool == null || !(tool.getItem() instanceof PickaxeItem))
            return baseFortune;

        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("heating");

        if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("molten_luck")) {
            clearPendingSmeltRoll(stack);
            return baseFortune;
        }

        var pendingSmeltResult = getPendingSmeltResult(stack);
        var pendingFortuneBonus = getPendingFortuneBonus(stack);

        if (pendingSmeltResult >= 0 && pendingFortuneBonus >= 0)
            return pendingSmeltResult == 1 ? baseFortune + pendingFortuneBonus : baseFortune;

        var smeltChance = Math.max(0D, Math.min(1D, ability.getStatData("smelt_chance").getValue()));

        if (ability.isRankModifierUnlocked("overheat"))
            smeltChance = Math.max(0D, Math.min(1D, smeltChance + getSmeltPityStacks(stack) * Math.max(0D, ability.getStatData("stack_bonus").getValue())));
        else
            setSmeltPityStacks(stack, 0);

        var smeltSuccess = player.getRandom().nextDouble() <= smeltChance;

        if (!smeltSuccess) {
            if (ability.isRankModifierUnlocked("overheat"))
                setSmeltPityStacks(stack, getSmeltPityStacks(stack) + 1);

            setPendingSmeltResult(stack, 0);
            setPendingFortuneBonus(stack, 0);
            return baseFortune;
        }

        if (ability.isRankModifierUnlocked("overheat"))
            setSmeltPityStacks(stack, 0);

        var chance = Math.max(0D, Math.min(1D, ability.getStatData("fortune_chance").getValue()));
        var maxCasts = Math.max(0, (int) MathUtils.round(ability.getStatData("fortune_max_casts").getValue(), 0));
        var procs = chance <= 0D || maxCasts <= 0 ? 0 : MathUtils.multicast(player.getRandom(), chance, maxCasts);

        setPendingSmeltResult(stack, 1);
        setPendingFortuneBonus(stack, Math.max(0, procs));

        if (procs > 0) {
            relicData.getLevelingData().addExperience("heating", "molten_luck_level", procs);
            ability.getStatisticData().getMetricData("molten_luck_levels").addValue(procs);
        }

        if (procs <= 0)
            return baseFortune;

        return baseFortune + procs;
    }

    private int getSmeltPityStacks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.PICKAXE_HEATER_SMELT_PITY_STACKS.get(), 0));
    }

    private void setSmeltPityStacks(ItemStack stack, int value) {
        stack.set(DataComponentRegistry.PICKAXE_HEATER_SMELT_PITY_STACKS.get(), Math.max(0, value));
    }

    private int getPendingSmeltResult(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.PICKAXE_HEATER_PENDING_SMELT_RESULT.get(), -1);
    }

    private void setPendingSmeltResult(ItemStack stack, int value) {
        stack.set(DataComponentRegistry.PICKAXE_HEATER_PENDING_SMELT_RESULT.get(), value);
    }

    private int getPendingFortuneBonus(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.PICKAXE_HEATER_PENDING_FORTUNE_BONUS.get(), -1);
    }

    private void setPendingFortuneBonus(ItemStack stack, int value) {
        stack.set(DataComponentRegistry.PICKAXE_HEATER_PENDING_FORTUNE_BONUS.get(), value);
    }

    private void clearPendingSmeltRoll(ItemStack stack) {
        setPendingSmeltResult(stack, -1);
        setPendingFortuneBonus(stack, -1);
    }

    private ItemStack getSmeltingResult(BlockDropsEvent event, ItemStack dropStack) {
        if (dropStack.isEmpty())
            return ItemStack.EMPTY;

        var input = dropStack.copy();

        input.setCount(1);

        var recipe = event.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), event.getLevel());

        if (recipe.isEmpty())
            return ItemStack.EMPTY;

        var result = recipe.get().value().assemble(new SingleRecipeInput(input), event.getLevel().registryAccess());

        if (result.isEmpty())
            return ItemStack.EMPTY;

        var output = result.copy();

        output.setCount(Math.max(1, output.getCount()) * dropStack.getCount());

        return output;
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onBlockDrops(BlockDropsEvent event) {
            if (!(event.getBreaker() instanceof Player player) || player.level().isClientSide() || event.isCanceled())
                return;

            if (!event.getState().is(Tags.Blocks.ORES) || !(event.getTool().getItem() instanceof PickaxeItem))
                return;

            var relicStack = EntityUtils.findEquippedCurio(player, ModItems.PICKAXE_HEATER.value());

            if (!(relicStack.getItem() instanceof PickaxeHeaterItem relic))
                return;

            var relicData = relic.getRelicData(player, relicStack);
            var ability = relicData.getAbilitiesData().getAbilityData("heating");

            if (!ability.canPlayerUse(player)) {
                relic.setSmeltPityStacks(relicStack, 0);
                relic.clearPendingSmeltRoll(relicStack);
                return;
            }

            var pendingSmeltResult = relic.getPendingSmeltResult(relicStack);
            boolean success;

            if (pendingSmeltResult >= 0) {
                success = pendingSmeltResult == 1;
                relic.clearPendingSmeltRoll(relicStack);
            } else {
                var chance = Math.max(0D, Math.min(1D, ability.getStatData("smelt_chance").getValue()));

                if (ability.isRankModifierUnlocked("overheat"))
                    chance = Math.max(0D, Math.min(1D, chance + relic.getSmeltPityStacks(relicStack) * Math.max(0D, ability.getStatData("stack_bonus").getValue())));
                else
                    relic.setSmeltPityStacks(relicStack, 0);

                success = player.getRandom().nextDouble() <= chance;

                if (!success) {
                    if (ability.isRankModifierUnlocked("overheat"))
                        relic.setSmeltPityStacks(relicStack, relic.getSmeltPityStacks(relicStack) + 1);

                    return;
                }

                if (ability.isRankModifierUnlocked("overheat"))
                    relic.setSmeltPityStacks(relicStack, 0);
            }

            if (!success)
                return;

            var smeltedBlock = false;

            for (var dropEntity : event.getDrops()) {
                var smelted = relic.getSmeltingResult(event, dropEntity.getItem());

                if (smelted.isEmpty())
                    continue;

                dropEntity.setItem(smelted);
                smeltedBlock = true;
            }

            if (!smeltedBlock)
                return;

            relicData.getLevelingData().addExperience("heating", "smelted_block", 1D);
            ability.getStatisticData().getMetricData("smelted_blocks").addValue(1D);
        }

        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            if (!(player.getMainHandItem().getItem() instanceof PickaxeItem))
                return;

            if (player.getAttackStrengthScale(0.5F) < 1F)
                return;

            var igniteDuration = 0D;
            PickaxeHeaterItem igniteRelic = null;
            ItemStack igniteStack = ItemStack.EMPTY;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.PICKAXE_HEATER.value())) {
                if (!(stack.getItem() instanceof PickaxeHeaterItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("heating");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("blazing_strike"))
                    continue;

                var localDuration = Math.max(0D, ability.getStatData("ignite_duration").getValue());

                if (igniteRelic == null || localDuration > igniteDuration) {
                    igniteDuration = localDuration;
                    igniteRelic = relic;
                    igniteStack = stack;
                }
            }

            if (igniteDuration <= 0D || igniteRelic == null)
                return;

            var target = event.getEntity();
            var wasOnFire = target.isOnFire();

            target.igniteForSeconds((float) igniteDuration);

            if (wasOnFire || !target.isOnFire())
                return;

            var relicData = igniteRelic.getRelicData(player, igniteStack);
            var ability = relicData.getAbilitiesData().getAbilityData("heating");

            relicData.getLevelingData().addExperience("heating", "blazing_ignite", 1D);
            ability.getStatisticData().getMetricData("blazing_ignites").addValue(1D);
        }
    }
}
