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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class VampiricGloveItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("vampire")
                                .rankModifier(1, "streak")
                                .rankModifier(3, "execution_heal")
                                .rankModifier(5, "overheal_absorption")
                                .stat(AbilityStatTemplate.builder("lifesteal")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.08D, 0.24D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("streak_heal_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.03D, 0.09D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("streak_window")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1.5D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("max_streak_bonuses")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(2D, 6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("kill_heal")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.05D, 0.18D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("overheal_absorption_cap")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 10D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("vampire");

        if (!ability.canPlayerUse(player)) {
            clearState(stack);
            removeAbsorptionCap(player, stack);
            return;
        }

        if (!ability.isRankModifierUnlocked("overheal_absorption")) {
            removeAbsorptionCap(player, stack);
            return;
        }

        var cap = Math.max(0D, ability.getStatData("overheal_absorption_cap").getValue());

        if (cap <= 0D)
            removeAbsorptionCap(player, stack);
        else
            setAbsorptionCap(player, stack, cap);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (!(slotContext.entity() instanceof Player player) || stack.getItem() == newStack.getItem())
            return;

        clearState(stack);
        removeAbsorptionCap(player, stack);
    }

    private int getStreakCount(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.VAMPIRIC_GLOVE_STREAK_COUNT.get(), 0));
    }

    private void setStreakCount(ItemStack stack, int value) {
        stack.set(DataComponentRegistry.VAMPIRIC_GLOVE_STREAK_COUNT.get(), Math.max(0, value));
    }

    private long getLastAttackTick(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.VAMPIRIC_GLOVE_LAST_ATTACK_TICK.get(), -1L);
    }

    private void setLastAttackTick(ItemStack stack, long value) {
        stack.set(DataComponentRegistry.VAMPIRIC_GLOVE_LAST_ATTACK_TICK.get(), Math.max(-1L, value));
    }

    private void clearState(ItemStack stack) {
        setStreakCount(stack, 0);
        setLastAttackTick(stack, -1L);
    }

    private void setAbsorptionCap(Player player, ItemStack stack, double cap) {
        if (player.getAttribute(Attributes.MAX_ABSORPTION) != null)
            EntityUtils.resetAttribute(player, stack, Attributes.MAX_ABSORPTION, (float) Math.max(0D, cap), AttributeModifier.Operation.ADD_VALUE);
    }

    private void removeAbsorptionCap(Player player, ItemStack stack) {
        if (player.getAttribute(Attributes.MAX_ABSORPTION) != null)
            EntityUtils.removeAttribute(player, stack, Attributes.MAX_ABSORPTION, AttributeModifier.Operation.ADD_VALUE);
    }

    private static long secondsToTicks(double seconds) {
        return Math.max(0L, Math.round(Math.max(0D, seconds) * 20D));
    }

    private static void healWithOverflow(Player player, double amount, double absorptionCap) {
        if (amount <= 0D)
            return;

        if (absorptionCap <= 0D || player.getAttribute(Attributes.MAX_ABSORPTION) == null) {
            player.heal((float) amount);
            return;
        }

        var health = player.getHealth();
        var maxHealth = player.getMaxHealth();
        var missing = Math.max(0D, maxHealth - health);
        var directHeal = Math.min(amount, missing);

        if (directHeal > 0D)
            player.heal((float) directHeal);

        var overflow = amount - directHeal;

        if (overflow <= 0D)
            return;

        var currentAbsorption = Math.max(0D, player.getAbsorptionAmount());

        if (currentAbsorption >= absorptionCap)
            return;

        player.setAbsorptionAmount((float) Math.min(absorptionCap, currentAbsorption + overflow));
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingDamagePost(LivingDamageEvent.Post event) {
            if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player || player.level().isClientSide() || event.getEntity() == player || event.getNewDamage() <= 0F)
                return;

            var gameTime = player.level().getGameTime();
            var killed = !event.getEntity().isAlive() || event.getEntity().getHealth() <= 0F;
            var bestHealing = 0D;
            var bestOverhealCap = 0D;
            var bestUsesOverheal = false;
            ItemStack bestStack = ItemStack.EMPTY;
            VampiricGloveItem bestRelic = null;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.VAMPIRIC_GLOVE.value())) {
                if (!(stack.getItem() instanceof VampiricGloveItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("vampire");

                if (!ability.canPlayerUse(player)) {
                    relic.clearState(stack);
                    relic.removeAbsorptionCap(player, stack);
                    continue;
                }

                if (ability.isRankModifierUnlocked("overheal_absorption")) {
                    var cap = Math.max(0D, ability.getStatData("overheal_absorption_cap").getValue());

                    if (cap > 0D)
                        relic.setAbsorptionCap(player, stack, cap);
                    else
                        relic.removeAbsorptionCap(player, stack);
                } else {
                    relic.removeAbsorptionCap(player, stack);
                }

                var streak = 0;

                if (ability.isRankModifierUnlocked("streak")) {
                    var windowTicks = secondsToTicks(ability.getStatData("streak_window").getValue());
                    var maxStacks = Math.max(0, (int) MathUtils.round(ability.getStatData("max_streak_bonuses").getValue(), 0));

                    if (maxStacks > 0) {
                        var lastTick = relic.getLastAttackTick(stack);
                        var previousStreak = relic.getStreakCount(stack);

                        streak = lastTick >= 0L && gameTime - lastTick <= windowTicks ? Math.min(maxStacks, previousStreak + 1) : 0;
                    }

                    relic.setStreakCount(stack, streak);
                    relic.setLastAttackTick(stack, gameTime);
                } else {
                    relic.clearState(stack);
                }

                var healingPercent = Math.max(0D, ability.getStatData("lifesteal").getValue());

                if (ability.isRankModifierUnlocked("streak") && streak > 0) {
                    var streakBonus = Math.max(0D, ability.getStatData("streak_heal_bonus").getValue());

                    healingPercent += streakBonus * streak;
                }

                var healing = Math.max(0D, event.getNewDamage()) * healingPercent;

                if (killed && ability.isRankModifierUnlocked("execution_heal")) {
                    var killHeal = Math.max(0D, Math.min(1D, ability.getStatData("kill_heal").getValue()));

                    healing += player.getMaxHealth() * killHeal;
                }

                if (healing > bestHealing || (healing == bestHealing && ability.isRankModifierUnlocked("overheal_absorption") && !bestUsesOverheal)) {
                    bestHealing = healing;
                    bestUsesOverheal = ability.isRankModifierUnlocked("overheal_absorption");
                    bestOverhealCap = bestUsesOverheal ? Math.max(0D, ability.getStatData("overheal_absorption_cap").getValue()) : 0D;
                    bestStack = stack;
                    bestRelic = relic;
                }
            }

            if (bestHealing <= 0D)
                return;

            if (!bestUsesOverheal || bestOverhealCap <= 0D) {
                player.heal((float) bestHealing);
                return;
            }

            if (bestRelic != null && !bestStack.isEmpty())
                bestRelic.setAbsorptionCap(player, bestStack, bestOverhealCap);

            healWithOverflow(player, bestHealing, bestOverhealCap);
        }
    }
}