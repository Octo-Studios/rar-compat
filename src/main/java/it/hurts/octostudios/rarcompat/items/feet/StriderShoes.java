package it.hurts.octostudios.rarcompat.items.feet;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.events.common.FluidCollisionEvent;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

public class StriderShoes extends WearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("lava_stride")
                                .rankModifier(1, "lava_jump")
                                .rankModifier(3, "free_stride")
                                .rankModifier(5, "fire_immunity")
                                .stat(AbilityStatTemplate.builder("speed_penalty")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.2D, 0.45D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("recovery_time")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1.5D, 4.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("jump_bonus")
                                        .thresholdValue(0D, 1D)
                                        .initialValue(0.1D, 0.35D)
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

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("lava_stride");

        if (!ability.canPlayerUse(player)) {
            resetStrideState(player, stack);
            return;
        }

        var freeStride = ability.isRankModifierUnlocked("free_stride");

        if (!isStrideActive(player, freeStride)) {
            resetStrideState(player, stack);
            return;
        }

        var maxRecoveryTicks = Math.max(1, (int) Math.round(Math.max(0D, ability.getStatData("recovery_time").getValue()) * 20D));
        var recoveryTicks = Math.min(maxRecoveryTicks, getRecoveryTicks(stack) + 1);
        setRecoveryTicks(stack, recoveryTicks);

        var penalty = Math.max(0D, Math.min(1D, ability.getStatData("speed_penalty").getValue()));
        var progress = Math.max(0D, Math.min(1D, (double) recoveryTicks / (double) maxRecoveryTicks));
        var currentPenalty = Math.max(0D, penalty * (1D - progress));

        if (currentPenalty > 0D)
            EntityUtils.resetAttribute(player, stack, Attributes.MOVEMENT_SPEED, (float) -currentPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        else
            EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem() || !(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        resetStrideState(player, stack);
    }

    private void resetStrideState(Player player, ItemStack stack) {
        setRecoveryTicks(stack, 0);
        EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private int getRecoveryTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.STRIDER_SHOES_RECOVERY_TICKS.get(), 0));
    }

    private void setRecoveryTicks(ItemStack stack, int ticks) {
        stack.set(DataComponentRegistry.STRIDER_SHOES_RECOVERY_TICKS.get(), Math.max(0, ticks));
    }

    private static boolean isStrideActive(Player player, boolean freeStride) {
        if (player.isSpectator() || player.getAbilities().flying || player.isFallFlying())
            return false;

        if (!freeStride && !player.isShiftKeyDown())
            return false;

        if (player.isInFluidType())
            return false;

        var surfaceY = getLavaSurfaceY(player);

        if (Double.isNaN(surfaceY))
            return false;

        var minY = player.getBoundingBox().minY;

        return minY >= surfaceY - 1.25D && minY <= surfaceY + 0.5D;
    }

    private static double getLavaSurfaceY(Player player) {
        var level = player.level();
        var probe = BlockPos.containing(player.getX(), player.getBoundingBox().minY - 0.05D, player.getZ());

        if (level.getFluidState(probe).is(FluidTags.LAVA))
            return probe.getY() + 1D;

        var below = probe.below();

        if (level.getFluidState(below).is(FluidTags.LAVA))
            return below.getY() + 1D;

        return Double.NaN;
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onFluidCollision(FluidCollisionEvent event) {
            LivingEntity entity = event.getEntity();

            if (!(entity instanceof Player player) || player.level().isClientSide())
                return;

            if (!event.getFluid().is(FluidTags.LAVA) || entity.isInFluidType())
                return;

            for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.STRIDER_SHOES.value())) {
                if (!(stack.getItem() instanceof StriderShoes relic))
                    continue;

                var ability = relic.getRelicData(entity, stack).getAbilitiesData().getAbilityData("lava_stride");

                if (!ability.canPlayerUse(entity))
                    continue;

                var freeStride = ability.isRankModifierUnlocked("free_stride");

                if (!freeStride && !entity.isShiftKeyDown())
                    continue;

                if (!isStrideActive(player, freeStride))
                    continue;

                event.setCanceled(true);
                return;
            }
        }

        @SubscribeEvent
        public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide())
                return;

            var jumpBonus = 0D;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.STRIDER_SHOES.value())) {
                if (!(stack.getItem() instanceof StriderShoes relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("lava_stride");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("lava_jump"))
                    continue;

                if (!isStrideActive(player, ability.isRankModifierUnlocked("free_stride")))
                    continue;

                var value = Math.max(0D, Math.min(1D, ability.getStatData("jump_bonus").getValue()));
                jumpBonus = Math.max(jumpBonus, value);
            }

            if (jumpBonus <= 0D)
                return;

            var motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, motion.y * (1D + jumpBonus), motion.z);
        }

        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            if (!event.getSource().is(DamageTypeTags.IS_FIRE))
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.STRIDER_SHOES.value())) {
                if (!(stack.getItem() instanceof StriderShoes relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("lava_stride");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("fire_immunity"))
                    continue;

                if (!isStrideActive(player, ability.isRankModifierUnlocked("free_stride")))
                    continue;

                event.setAmount(0F);
                event.setCanceled(true);
                return;
            }
        }
    }
}