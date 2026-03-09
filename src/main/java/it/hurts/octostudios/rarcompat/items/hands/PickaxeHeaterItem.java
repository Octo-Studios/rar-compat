package it.hurts.octostudios.rarcompat.items.hands;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.tags.BlockTags;
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
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.12D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("ignite_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("stack_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.02D, 0.08D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("fortune_chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("fortune_max_casts")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        setSmeltPityStacks(stack, 0);
    }

    @Override
    public int getFortuneLevel(SlotContext slotContext, LootContext lootContext, ItemStack stack) {
        var baseFortune = super.getFortuneLevel(slotContext, lootContext, stack);

        if (!(slotContext.entity() instanceof Player player) || lootContext == null)
            return baseFortune;

        var blockState = lootContext.getParamOrNull(LootContextParams.BLOCK_STATE);

        if (blockState == null || !blockState.is(Tags.Blocks.ORES))
            return baseFortune;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("heating");

        if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("molten_luck"))
            return baseFortune;

        var chance = Math.max(0D, Math.min(1D, ability.getStatData("fortune_chance").getValue()));
        var maxCasts = Math.max(0, (int) MathUtils.round(ability.getStatData("fortune_max_casts").getValue(), 0));

        if (chance <= 0D || maxCasts <= 0)
            return baseFortune;

        var procs = MathUtils.multicast(player.getRandom(), chance, maxCasts);

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

            var ability = relic.getRelicData(player, relicStack).getAbilitiesData().getAbilityData("heating");

            if (!ability.canPlayerUse(player)) {
                relic.setSmeltPityStacks(relicStack, 0);
                return;
            }

            var chance = Math.max(0D, Math.min(1D, ability.getStatData("smelt_chance").getValue()));

            if (ability.isRankModifierUnlocked("overheat"))
                chance = Math.max(0D, Math.min(1D, chance + relic.getSmeltPityStacks(relicStack) * Math.max(0D, ability.getStatData("stack_bonus").getValue())));
            else
                relic.setSmeltPityStacks(relicStack, 0);

            var success = player.getRandom().nextDouble() <= chance;

            if (!success) {
                if (ability.isRankModifierUnlocked("overheat"))
                    relic.setSmeltPityStacks(relicStack, relic.getSmeltPityStacks(relicStack) + 1);

                return;
            }

            if (ability.isRankModifierUnlocked("overheat"))
                relic.setSmeltPityStacks(relicStack, 0);

            for (var dropEntity : event.getDrops()) {
                var smelted = relic.getSmeltingResult(event, dropEntity.getItem());

                if (smelted.isEmpty())
                    continue;

                dropEntity.setItem(smelted);
            }
        }

        @SubscribeEvent
        public static void onLivingIncomingDamageDealing(LivingIncomingDamageEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getAmount() <= 0F)
                return;

            if (!(player.getMainHandItem().getItem() instanceof PickaxeItem))
                return;

            var igniteDuration = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.PICKAXE_HEATER.value())) {
                if (!(stack.getItem() instanceof PickaxeHeaterItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("heating");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("blazing_strike"))
                    continue;

                igniteDuration = Math.max(igniteDuration, Math.max(0D, ability.getStatData("ignite_duration").getValue()));
            }

            if (igniteDuration > 0D)
                event.getEntity().igniteForSeconds((float) igniteDuration);
        }
    }
}
