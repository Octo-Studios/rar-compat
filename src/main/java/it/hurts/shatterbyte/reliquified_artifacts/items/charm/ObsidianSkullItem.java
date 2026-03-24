package it.hurts.shatterbyte.reliquified_artifacts.items.charm;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
import artifacts.registry.ModItems;
import it.hurts.shatterbyte.reliquified_artifacts.ReliquifiedArtifacts;
import it.hurts.shatterbyte.reliquified_artifacts.init.RADataComponent;
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
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class ObsidianSkullItem extends RAWearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("lava")
                                .rankModifier(1, "lava_launch")
                                .rankModifier(3, "heat_surge")
                                .rankModifier(5, "obsidian_skin")
                                .stat(AbilityStatTemplate.builder("duration")
                                        .initialValue(5D, 15D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0857D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("low_reserve_speed_bonus")
                                        .initialValue(0.15D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0857D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("damage_reduction")
                                        .initialValue(0.1D, 0.25D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0571D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("lava_second").build())
                                        .source(ExperienceSourceTemplate.builder("safe_fall")
                                                .rankModifierVisibilityState("lava_launch", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("lava_duration")
                                                .formatValue(value -> MathUtils.formatTime(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("safe_falls")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("lava_launch", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("heat_surge_procs")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("heat_surge", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("damage_reduced")
                                                .formatValue(value -> String.valueOf(MathUtils.round(value, 2)))
                                                .rankModifierVisibilityState("obsidian_skin", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.THE_NETHER, LootEntries.NETHER_LIKE)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("lava");

        if (!ability.canPlayerUse(player)) {
            setHeatSurgeActive(stack, false);
            return;
        }

        var maxLavaTicks = getMaxLavaTicks(ability.getStatData("duration").getValue());

        if (!stack.has(RADataComponent.OBSIDIAN_SKULL_LAVA_TICKS.get()))
            setLavaTicks(stack, maxLavaTicks);

        var lavaTicks = Math.max(0, Math.min(maxLavaTicks, getLavaTicks(stack)));
        var protectedInLava = player.isInLava() && lavaTicks > 0;

        if (protectedInLava && player.tickCount % 20 == 0) {
            relicData.getLevelingData().addExperience("lava", "lava_second", 1D);
            ability.getStatisticData().getMetricData("lava_duration").addValue(1D);
        }

        var lowReserveActive = ability.isRankModifierUnlocked("heat_surge")
                && protectedInLava
                && lavaTicks <= Math.max(1, maxLavaTicks / 4);

        if (lowReserveActive && !isHeatSurgeActive(stack))
            ability.getStatisticData().getMetricData("heat_surge_procs").addValue(1D);

        setHeatSurgeActive(stack, lowReserveActive);

        if (player.isInLava()) {
            if (lavaTicks > 0) {
                lavaTicks--;
                player.clearFire();
            }
        } else if (lavaTicks < maxLavaTicks) {
            lavaTicks++;
        }

        setLavaTicks(stack, lavaTicks);
    }

    private int getMaxLavaTicks(double seconds) {
        return Math.max(1, (int) Math.round(Math.max(1D, seconds) * 20D));
    }

    private int getLavaTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(RADataComponent.OBSIDIAN_SKULL_LAVA_TICKS.get(), 0));
    }

    private void setLavaTicks(ItemStack stack, int ticks) {
        stack.set(RADataComponent.OBSIDIAN_SKULL_LAVA_TICKS.get(), Math.max(0, ticks));
    }

    private boolean isHeatSurgeActive(ItemStack stack) {
        return stack.getOrDefault(RADataComponent.OBSIDIAN_SKULL_HEAT_SURGE_ACTIVE.get(), false);
    }

    private void setHeatSurgeActive(ItemStack stack, boolean active) {
        stack.set(RADataComponent.OBSIDIAN_SKULL_HEAT_SURGE_ACTIVE.get(), active);
    }

    public static double getHeatSurgeLavaSpeedBonus(Player player) {
        if (player == null || !player.isInLava())
            return 0D;

        var stack = EntityUtils.findEquippedCurio(player, ModItems.OBSIDIAN_SKULL.value());

        if (!(stack.getItem() instanceof ObsidianSkullItem relic))
            return 0D;

        var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("lava");

        if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("heat_surge"))
            return 0D;

        var maxLavaTicks = relic.getMaxLavaTicks(ability.getStatData("duration").getValue());
        var lavaTicks = Math.max(0, Math.min(maxLavaTicks, relic.getLavaTicks(stack)));
        var isLowReserve = lavaTicks > 0 && lavaTicks <= Math.max(1, maxLavaTicks / 4);

        if (!isLowReserve)
            return 0D;

        return Math.max(0D, ability.getStatData("low_reserve_speed_bonus").getValue());
    }

    private static boolean isLandingInLava(Player player) {
        if (player.isInLava())
            return true;

        var level = player.level();
        var box = player.getBoundingBox();
        var y = Mth.floor(box.minY - 0.05D);
        var minX = Mth.floor(box.minX + 1.0E-4D);
        var maxX = Mth.floor(box.maxX - 1.0E-4D);
        var minZ = Mth.floor(box.minZ + 1.0E-4D);
        var maxZ = Mth.floor(box.maxZ - 1.0E-4D);

        for (var x = minX; x <= maxX; x++) {
            for (var z = minZ; z <= maxZ; z++) {
                if (level.getFluidState(new BlockPos(x, y, z)).is(FluidTags.LAVA))
                    return true;
            }
        }

        var feetPos = player.blockPosition();

        return level.getFluidState(feetPos).is(FluidTags.LAVA) || level.getFluidState(feetPos.below()).is(FluidTags.LAVA);
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || !player.isInLava())
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.OBSIDIAN_SKULL.value());

            if (!(stack.getItem() instanceof ObsidianSkullItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("lava");

            if (!ability.canPlayerUse(player))
                return;

            if (relic.getLavaTicks(stack) <= 0)
                return;

            if (event.getSource().is(DamageTypes.LAVA) || event.getSource().is(DamageTypes.IN_FIRE) || event.getSource().is(DamageTypes.ON_FIRE)) {
                event.setCanceled(true);

                return;
            }

            if (ability.isRankModifierUnlocked("obsidian_skin")) {
                var reduction = Math.max(0D, Math.min(1D, ability.getStatData("damage_reduction").getValue()));

                if (reduction > 0D) {
                    var baseDamage = event.getAmount();
                    var reducedDamage = (float) Math.max(0D, baseDamage * (1D - reduction));
                    var blockedDamage = Math.max(0F, baseDamage - reducedDamage);

                    event.setAmount(reducedDamage);

                    if (blockedDamage > 0F)
                        ability.getStatisticData().getMetricData("damage_reduced").addValue(blockedDamage);
                }
            }
        }

        @SubscribeEvent
        public static void onLivingFall(LivingFallEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getDistance() <= 0F)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.OBSIDIAN_SKULL.value());

            if (!(stack.getItem() instanceof ObsidianSkullItem relic))
                return;

            var relicData = relic.getRelicData(player, stack);
            var ability = relicData.getAbilitiesData().getAbilityData("lava");

            if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("lava_launch") || relic.getLavaTicks(stack) <= 0)
                return;

            if (!isLandingInLava(player))
                return;

            event.setCanceled(true);
            relicData.getLevelingData().addExperience("lava", "safe_fall", 1D);
            ability.getStatisticData().getMetricData("safe_falls").addValue(1D);
        }
    }
}
