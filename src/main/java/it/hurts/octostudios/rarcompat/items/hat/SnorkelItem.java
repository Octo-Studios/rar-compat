package it.hurts.octostudios.rarcompat.items.hat;

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

public class SnorkelItem extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("snorkeling")
                                .rankModifier(1, "vision")
                                .rankModifier(3, "reserve")
                                .rankModifier(5, "resistance")
                                .stat(AbilityStatTemplate.builder("water_depth")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 3D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("reserve_duration")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(2D, 6D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("drowning_resistance")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.2D, 0.4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        var entity = slotContext.entity();

        if (!(entity instanceof Player player) || player.level().isClientSide())
            return;

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("snorkeling");

        if (!ability.canPlayerUse(player))
            return;

        var maxWaterDepth = Math.max(0, (int) MathUtils.round(ability.getStatData("water_depth").getValue(), 0));
        var isSafeHeight = isSafeBreathingHeight(player, maxWaterDepth);

        if (isSafeHeight) {
            setReserveTriggered(stack, false);

            if (player.isEyeInFluid(FluidTags.WATER) && player.getAirSupply() < player.getMaxAirSupply())
                player.setAirSupply(Math.min(player.getAirSupply() + 4, player.getMaxAirSupply()));

            return;
        }

        if (!ability.isRankModifierUnlocked("reserve") || isReserveTriggered(stack))
            return;

        var durationTicks = getReserveDurationTicks(player, stack);

        if (durationTicks > 0)
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, durationTicks, 0, false, false));

        setReserveTriggered(stack, true);
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

    private boolean isReserveTriggered(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.SNORKEL_RESERVE_TRIGGERED.get(), false);
    }

    private void setReserveTriggered(ItemStack stack, boolean triggered) {
        stack.set(DataComponentRegistry.SNORKEL_RESERVE_TRIGGERED.get(), triggered);
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
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

            event.setAmount((float) Math.max(0D, event.getAmount() * (1D - reduction)));
        }
    }

    @EventBusSubscriber(modid = RARCompat.MODID, value = Dist.CLIENT)
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

