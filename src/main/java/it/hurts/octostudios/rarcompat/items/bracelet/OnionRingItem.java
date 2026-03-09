package it.hurts.octostudios.rarcompat.items.bracelet;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import top.theillusivec4.curios.api.SlotContext;

public class OnionRingItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("hunger_mining")
                                .rankModifier(1, "food_rush")
                                .rankModifier(3, "sustenance")
                                .rankModifier(5, "perfect_focus")
                                .stat(AbilityStatTemplate.builder("speed_per_hunger")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.01D, 0.03D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("food_boost")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.12D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("food_boost_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("hunger_restore_chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.05D, 0.2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("hunger_mining");

        if (!ability.canPlayerUse(player)) {
            setFoodBoostTicks(stack, 0);
            return;
        }

        var ticks = getFoodBoostTicks(stack);

        if (ticks > 0)
            setFoodBoostTicks(stack, ticks - 1);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem())
            return;

        setFoodBoostTicks(stack, 0);
    }

    private int getFoodBoostTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.ONION_RING_FOOD_BOOST_TICKS.get(), 0));
    }

    private void setFoodBoostTicks(ItemStack stack, int ticks) {
        stack.set(DataComponentRegistry.ONION_RING_FOOD_BOOST_TICKS.get(), Math.max(0, ticks));
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(0, (int) Math.round(Math.max(0D, seconds) * 20D));
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
            var player = event.getEntity();

            if (player.level().isClientSide() || event.getNewSpeed() <= 0F)
                return;

            var hunger = Math.max(0, player.getFoodData().getFoodLevel());
            var speedPerHunger = 0D;
            var foodRushBonus = 0D;
            var instantBreak = false;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.ONION_RING.value())) {
                if (!(stack.getItem() instanceof OnionRingItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("hunger_mining");

                if (!ability.canPlayerUse(player)) {
                    relic.setFoodBoostTicks(stack, 0);
                    continue;
                }

                speedPerHunger = Math.max(speedPerHunger, Math.max(0D, ability.getStatData("speed_per_hunger").getValue()));

                if (ability.isRankModifierUnlocked("food_rush") && relic.getFoodBoostTicks(stack) > 0)
                    foodRushBonus = Math.max(foodRushBonus, Math.max(0D, ability.getStatData("food_boost").getValue()));

                if (ability.isRankModifierUnlocked("perfect_focus") && hunger >= 20)
                    instantBreak = true;
            }

            var multiplier = 1D + speedPerHunger * hunger + foodRushBonus;

            if (multiplier > 1D)
                event.setNewSpeed((float) Math.max(0D, event.getNewSpeed() * multiplier));

            if (!instantBreak)
                return;

            var pos = event.getPosition().orElse(null);

            if (pos == null)
                return;

            if (event.getState().getDestroySpeed(player.level(), pos) < 0F)
                return;

            event.setNewSpeed(Float.MAX_VALUE);
        }

        @SubscribeEvent
        public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide())
                return;

            if (event.getItem().getUseAnimation() != UseAnim.EAT)
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.ONION_RING.value())) {
                if (!(stack.getItem() instanceof OnionRingItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("hunger_mining");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("food_rush")) {
                    relic.setFoodBoostTicks(stack, 0);
                    continue;
                }

                var durationTicks = secondsToTicks(ability.getStatData("food_boost_duration").getValue());

                if (durationTicks <= 0)
                    continue;

                relic.setFoodBoostTicks(stack, Math.max(relic.getFoodBoostTicks(stack), durationTicks));
            }
        }

        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            var player = event.getPlayer();

            if (player.level().isClientSide() || event.isCanceled())
                return;

            var chance = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.ONION_RING.value())) {
                if (!(stack.getItem() instanceof OnionRingItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("hunger_mining");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("sustenance"))
                    continue;

                var value = Math.max(0D, Math.min(1D, ability.getStatData("hunger_restore_chance").getValue()));

                chance = Math.max(chance, value);
            }

            if (chance > 0D && player.getRandom().nextDouble() <= chance)
                player.getFoodData().eat(1, 0F);
        }
    }
}