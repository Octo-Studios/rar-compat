package it.hurts.octostudios.rarcompat.items.hat;

import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;

public class SuperstitiousHatItem extends WearableRelicItem {

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("looting")
                                .rankModifier(1, "first_kill")
                                .rankModifier(3, "beast_hunter")
                                .rankModifier(5, "streak")
                                .stat(AbilityStatTemplate.builder("chance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("first_kill_window")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(8D, 16D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("first_kill_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("animal_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.25D, 0.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("streak_window")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(4D, 10D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("streak_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.08D, 0.2D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("streak_max_effects")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(2D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("max_casts")
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
    public int getLootingLevel(SlotContext slotContext, @Nullable LootContext lootContext, ItemStack stack) {
        var baseLooting = super.getLootingLevel(slotContext, lootContext, stack);

        if (!(slotContext.entity() instanceof Player player) || lootContext == null)
            return baseLooting;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("looting");

        if (!ability.canPlayerUse(player)) {
            resetRuntimeState(stack);
            return baseLooting;
        }

        var target = lootContext.getParamOrNull(LootContextParams.THIS_ENTITY);

        if (!(target instanceof Mob mob))
            return baseLooting;

        var targetUuid = mob.getStringUUID();

        if (targetUuid.equals(getLastTargetUuid(stack)))
            return baseLooting + getLastLootingBonus(stack);

        var baseChance = Math.max(0D, Math.min(1D, ability.getStatData("chance").getValue()));
        var effectiveChance = baseChance;
        var maxCasts = Math.max(1, (int) MathUtils.round(ability.getStatData("max_casts").getValue(), 0));

        var gameTime = player.level().getGameTime();
        var lastKillTick = getLastKillTick(stack);
        var ticksSinceLastKill = lastKillTick >= 0L ? gameTime - lastKillTick : Long.MAX_VALUE;

        if (ability.isRankModifierUnlocked("first_kill")) {
            var firstWindowTicks = secondsToTicks(ability.getStatData("first_kill_window").getValue());
            var firstKillBonus = Math.max(0D, ability.getStatData("first_kill_bonus").getValue());

            if (firstWindowTicks <= 0L || ticksSinceLastKill > firstWindowTicks)
                effectiveChance = Math.max(0D, Math.min(1D, effectiveChance + baseChance * firstKillBonus));
        }

        if (ability.isRankModifierUnlocked("beast_hunter") && mob instanceof Animal) {
            var animalBonus = Math.max(0D, ability.getStatData("animal_bonus").getValue());

            effectiveChance = Math.max(0D, Math.min(1D, effectiveChance + baseChance * animalBonus));
        }

        int lootingBonus;

        if (ability.isRankModifierUnlocked("streak")) {
            var streakWindowTicks = secondsToTicks(ability.getStatData("streak_window").getValue());
            var streakBonus = Math.max(0D, ability.getStatData("streak_bonus").getValue());
            var previousStreak = getKillStreak(stack);
            var streak = lastKillTick >= 0L && ticksSinceLastKill <= streakWindowTicks ? previousStreak + 1 : 1;

            if (streak > 1)
                effectiveChance = Math.max(0D, Math.min(1D, effectiveChance + baseChance * streakBonus * (streak - 1)));

            var streakCap = Math.max(1, (int) MathUtils.round(ability.getStatData("streak_max_effects").getValue(), 0));

            maxCasts = Math.min(maxCasts, streakCap);
            lootingBonus = MathUtils.multicast(player.getRandom(), effectiveChance, maxCasts);
            setKillStreak(stack, streak);
        } else {
            lootingBonus = MathUtils.multicast(player.getRandom(), effectiveChance, maxCasts);
            setKillStreak(stack, 0);
        }

        setLastKillTick(stack, gameTime);
        setLastTargetUuid(stack, targetUuid);
        setLastLootingBonus(stack, lootingBonus);

        if (lootingBonus <= 0)
            return baseLooting;

        return baseLooting + lootingBonus;
    }

    private long getLastKillTick(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.SUPERSTITIOUS_HAT_LAST_KILL_TICK.get(), -1L);
    }

    private void setLastKillTick(ItemStack stack, long tick) {
        stack.set(DataComponentRegistry.SUPERSTITIOUS_HAT_LAST_KILL_TICK.get(), Math.max(-1L, tick));
    }

    private int getKillStreak(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.SUPERSTITIOUS_HAT_STREAK_COUNT.get(), 0));
    }

    private void setKillStreak(ItemStack stack, int streak) {
        stack.set(DataComponentRegistry.SUPERSTITIOUS_HAT_STREAK_COUNT.get(), Math.max(0, streak));
    }

    private String getLastTargetUuid(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.SUPERSTITIOUS_HAT_LAST_TARGET_UUID.get(), "");
    }

    private void setLastTargetUuid(ItemStack stack, String uuid) {
        stack.set(DataComponentRegistry.SUPERSTITIOUS_HAT_LAST_TARGET_UUID.get(), uuid == null ? "" : uuid);
    }

    private int getLastLootingBonus(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.SUPERSTITIOUS_HAT_LAST_LOOTING_BONUS.get(), 0));
    }

    private void setLastLootingBonus(ItemStack stack, int bonus) {
        stack.set(DataComponentRegistry.SUPERSTITIOUS_HAT_LAST_LOOTING_BONUS.get(), Math.max(0, bonus));
    }

    private void resetRuntimeState(ItemStack stack) {
        setLastKillTick(stack, -1L);
        setKillStreak(stack, 0);
        setLastTargetUuid(stack, "");
        setLastLootingBonus(stack, 0);
    }

    private static long secondsToTicks(double seconds) {
        return Math.max(0L, Math.round(Math.max(0D, seconds) * 20D));
    }
}

