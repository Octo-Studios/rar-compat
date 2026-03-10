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
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import top.theillusivec4.curios.api.SlotContext;

public class AquaDashersItem extends WearableRelicItem {

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("water_dash")
                                .rankModifier(1, "water_jump")
                                .rankModifier(3, "free_dash")
                                .rankModifier(5, "projectile_phase")
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

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("water_dash");

        if (!ability.canPlayerUse(player)) {
            resetDashState(player, stack);
            return;
        }

        if (!isDashActive(player, ability.isRankModifierUnlocked("free_dash"))) {
            resetDashState(player, stack);
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

        resetDashState(player, stack);
    }

    private void resetDashState(Player player, ItemStack stack) {
        setRecoveryTicks(stack, 0);
        EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private int getRecoveryTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponentRegistry.AQUA_DASHERS_RECOVERY_TICKS.get(), 0));
    }

    private void setRecoveryTicks(ItemStack stack, int ticks) {
        stack.set(DataComponentRegistry.AQUA_DASHERS_RECOVERY_TICKS.get(), Math.max(0, ticks));
    }

    private static boolean isRunning(Player player) {
        return player.isSprinting()
                && !player.isFallFlying()
                && player.getKnownMovement().multiply(1D, 0D, 1D).length() > 1.0E-4D;
    }

    private static boolean isDashActive(Player player, boolean freeDash) {
        if (player.isSpectator() || player.getAbilities().flying || player.isFallFlying())
            return false;

        if (!freeDash && !isRunning(player))
            return false;

        if (player.isInFluidType())
            return false;

        var surfaceY = getWaterSurfaceY(player);

        if (Double.isNaN(surfaceY))
            return false;

        var minY = player.getBoundingBox().minY;

        return minY >= surfaceY - 1.25D && minY <= surfaceY + 0.5D;
    }

    private static double getWaterSurfaceY(Player player) {
        var level = player.level();
        var probe = BlockPos.containing(player.getX(), player.getBoundingBox().minY - 0.05D, player.getZ());

        if (level.getFluidState(probe).is(FluidTags.WATER))
            return probe.getY() + 1D;

        var below = probe.below();

        if (level.getFluidState(below).is(FluidTags.WATER))
            return below.getY() + 1D;

        return Double.NaN;
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onFluidCollision(FluidCollisionEvent event) {
            var entity = event.getEntity();

            if (!(entity instanceof Player player) || player.level().isClientSide())
                return;

            if (!event.getFluid().is(FluidTags.WATER) || entity.isInFluidType())
                return;

            for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.AQUA_DASHERS.value())) {
                if (!(stack.getItem() instanceof AquaDashersItem relic))
                    continue;

                var ability = relic.getRelicData(entity, stack).getAbilitiesData().getAbilityData("water_dash");

                if (!ability.canPlayerUse(entity))
                    continue;

                if (!isDashActive(player, ability.isRankModifierUnlocked("free_dash")))
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

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.AQUA_DASHERS.value())) {
                if (!(stack.getItem() instanceof AquaDashersItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("water_dash");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("water_jump"))
                    continue;

                if (!isDashActive(player, ability.isRankModifierUnlocked("free_dash")))
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
        public static void onProjectileImpact(ProjectileImpactEvent event) {
            if (!(event.getRayTraceResult() instanceof EntityHitResult hitResult) || !(hitResult.getEntity() instanceof Player player) || player.level().isClientSide())
                return;

            var projectile = event.getProjectile();

            if (!isEnemyProjectile(projectile, player))
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.AQUA_DASHERS.value())) {
                if (!(stack.getItem() instanceof AquaDashersItem relic))
                    continue;

                var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("water_dash");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("projectile_phase"))
                    continue;

                if (!isDashActive(player, ability.isRankModifierUnlocked("free_dash")))
                    continue;

                event.setCanceled(true);
                return;
            }
        }

        private static boolean isEnemyProjectile(Projectile projectile, Player player) {
            var owner = projectile.getOwner();

            if (owner == null)
                return true;

            if (owner == player)
                return false;

            if (owner instanceof LivingEntity living && living.isAlliedTo(player))
                return false;

            return true;
        }
    }
}
