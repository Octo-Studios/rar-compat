package it.hurts.octostudios.rarcompat.items.charm;

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

public class ObsidianSkullItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("lava")
                                .rankModifier(1, "lava_launch")
                                .rankModifier(3, "heat_surge")
                                .rankModifier(5, "obsidian_skin")
                                .stat(AbilityStatTemplate.builder("duration")
                                        .thresholdValue(1D, Double.MAX_VALUE)
                                        .initialValue(8D, 16D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("low_reserve_speed_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.12D, 0.3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("jump_boost")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(0.25D, 0.55D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())
                                .stat(AbilityStatTemplate.builder("damage_reduction")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.08D, 0.25D)
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

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("lava");

        if (!ability.canPlayerUse(player))
            return;

        var maxLavaTicks = getMaxLavaTicks(ability.getStatData("duration").getValue());

        if (!stack.has(DataComponentRegistry.OBSIDIAN_SKULL_LAVA_TICKS.get()))
            setLavaTicks(stack, maxLavaTicks);

        var lavaTicks = Math.max(0, Math.min(maxLavaTicks, getLavaTicks(stack)));

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
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.OBSIDIAN_SKULL_LAVA_TICKS.get(), 0));
    }

    private void setLavaTicks(ItemStack stack, int ticks) {
        stack.set(DataComponentRegistry.OBSIDIAN_SKULL_LAVA_TICKS.get(), Math.max(0, ticks));
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

    @EventBusSubscriber(modid = RARCompat.MODID)
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

                if (reduction > 0D)
                    event.setAmount((float) Math.max(0D, event.getAmount() * (1D - reduction)));
            }
        }

        @SubscribeEvent
        public static void onLivingFall(LivingFallEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getDistance() <= 0F)
                return;

            var stack = EntityUtils.findEquippedCurio(player, ModItems.OBSIDIAN_SKULL.value());

            if (!(stack.getItem() instanceof ObsidianSkullItem relic))
                return;

            var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("lava");

            if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("lava_launch") || relic.getLavaTicks(stack) <= 0)
                return;

            if (isLandingInLava(player))
                event.setCanceled(true);
        }
    }
}
