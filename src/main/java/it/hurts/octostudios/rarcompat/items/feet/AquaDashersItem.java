package it.hurts.octostudios.rarcompat.items.feet;

import artifacts.registry.ModItems;
import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.events.common.FluidCollisionEvent;
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
import net.neoforged.neoforge.event.tick.LevelTickEvent;
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
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("surface_movement").build())
                                        .source(ExperienceSourceTemplate.builder("water_jump_boost")
                                                .rankModifierVisibilityState("water_jump", VisibilityState.OBFUSCATED)
                                                .build())
                                        .source(ExperienceSourceTemplate.builder("projectile_phase_ignore")
                                                .rankModifierVisibilityState("projectile_phase", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("surface_time")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("water_jump_boosts")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("water_jump", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("projectiles_ignored")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("projectile_phase", VisibilityState.OBFUSCATED)
                                                .build())
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
        var freeDash = ability.isRankModifierUnlocked("free_dash");
        var jumpBonus = ability.isRankModifierUnlocked("water_jump") ? Math.max(0D, Math.min(1D, ability.getStatData("jump_bonus").getValue())) : 0D;

        if (!ability.canPlayerUse(player)) {
            resetDashState(player, stack);
            return;
        }

        var active = isDashActive(player, freeDash);
        var onSurface = isOnWaterSurface(player);
        var currentRecoveryTicks = getRecoveryTicks(stack);
        var keepRecovery = currentRecoveryTicks > 0 && (!player.onGround() || onSurface);
        var keepSafeFall = jumpBonus > 0D
                && keepRecovery
                && (!player.onGround() || player.fallDistance > 0F);
        var applyJumpBonus = jumpBonus > 0D && onSurface;

        if (!active) {
            EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            if (applyJumpBonus)
                EntityUtils.resetAttribute(player, stack, Attributes.JUMP_STRENGTH, (float) jumpBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            else
                EntityUtils.removeAttribute(player, stack, Attributes.JUMP_STRENGTH, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            if (keepSafeFall)
                EntityUtils.resetAttribute(player, stack, Attributes.SAFE_FALL_DISTANCE, (float) jumpBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            else
                EntityUtils.removeAttribute(player, stack, Attributes.SAFE_FALL_DISTANCE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            if (!keepRecovery)
                setRecoveryTicks(stack, 0);

            return;
        }

        var maxRecoveryTicks = Math.max(1, (int) Math.round(Math.max(0D, ability.getStatData("recovery_time").getValue()) * 20D));
        var recoveryTicks = Math.min(maxRecoveryTicks, currentRecoveryTicks + 1);
        setRecoveryTicks(stack, recoveryTicks);

        var penalty = Math.max(0D, Math.min(1D, ability.getStatData("speed_penalty").getValue()));
        var progress = Math.max(0D, Math.min(1D, (double) recoveryTicks / (double) maxRecoveryTicks));
        var currentPenalty = Math.max(0D, penalty * (1D - progress));

        if (currentPenalty > 0D)
            EntityUtils.resetAttribute(player, stack, Attributes.MOVEMENT_SPEED, (float) -currentPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        else
            EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        if (jumpBonus > 0D) {
            EntityUtils.resetAttribute(player, stack, Attributes.JUMP_STRENGTH, (float) jumpBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            EntityUtils.resetAttribute(player, stack, Attributes.SAFE_FALL_DISTANCE, (float) jumpBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else {
            EntityUtils.removeAttribute(player, stack, Attributes.JUMP_STRENGTH, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            EntityUtils.removeAttribute(player, stack, Attributes.SAFE_FALL_DISTANCE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
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
        EntityUtils.removeAttribute(player, stack, Attributes.JUMP_STRENGTH, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        EntityUtils.removeAttribute(player, stack, Attributes.SAFE_FALL_DISTANCE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
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

        return isOnWaterSurface(player);
    }

    private static boolean isOnWaterSurface(Player player) {
        var level = player.level();
        var box = player.getBoundingBox();
        var minX = Mth.floor(box.minX + 1.0E-4D);
        var maxX = Mth.floor(box.maxX - 1.0E-4D);
        var minZ = Mth.floor(box.minZ + 1.0E-4D);
        var maxZ = Mth.floor(box.maxZ - 1.0E-4D);
        var startY = Mth.floor(box.minY - 0.05D);
        var pos = new BlockPos.MutableBlockPos();

        for (var x = minX; x <= maxX; x++) {
            for (var z = minZ; z <= maxZ; z++) {
                for (var depth = 0; depth <= 5; depth++) {
                    pos.set(x, startY - depth, z);

                    var state = level.getBlockState(pos);

                    if (state.blocksMotion())
                        break;

                    if (state.getFluidState().is(FluidTags.WATER))
                        return true;
                }
            }
        }

        return false;
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLevelTickPost(LevelTickEvent.Post event) {
            var level = event.getLevel();

            if (level.isClientSide())
                return;

            for (var player : level.players()) {
                if (!player.isAlive() || player.isSpectator())
                    continue;

                AquaDashersItem activeRelic = null;
                ItemStack activeStack = ItemStack.EMPTY;

                for (var stack : EntityUtils.findEquippedCurios(player, ModItems.AQUA_DASHERS.value())) {
                    if (!(stack.getItem() instanceof AquaDashersItem relic))
                        continue;

                    var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("water_dash");

                    if (!ability.canPlayerUse(player))
                        continue;

                    if (!isDashActive(player, ability.isRankModifierUnlocked("free_dash")))
                        continue;

                    activeRelic = relic;
                    activeStack = stack;
                    break;
                }

                if (activeRelic == null)
                    continue;

                var relicData = activeRelic.getRelicData(player, activeStack);
                var ability = relicData.getAbilitiesData().getAbilityData("water_dash");

                if (player.tickCount % 20 == 0)
                    ability.getStatisticData().getMetricData("surface_time").addValue(1D);

                if (player.getKnownMovement().multiply(1D, 0D, 1D).length() <= 1.0E-4D)
                    continue;

                var data = player.getPersistentData();
                var movingTicks = Math.max(0, data.getInt("rarcompat_aqua_dashers_surface_moving_ticks")) + 1;

                if (movingTicks >= 100) {
                    var experience = movingTicks / 100;

                    relicData.getLevelingData().addExperience("water_dash", "surface_movement", experience);
                    movingTicks %= 100;
                }

                data.putInt("rarcompat_aqua_dashers_surface_moving_ticks", movingTicks);
            }
        }

        @SubscribeEvent
        public static void onFluidCollision(FluidCollisionEvent event) {
            var entity = event.getEntity();

            if (!(entity instanceof Player player) || player.isShiftKeyDown())
                return;

            if (!event.getFluid().is(FluidTags.WATER))
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

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.AQUA_DASHERS.value())) {
                if (!(stack.getItem() instanceof AquaDashersItem relic))
                    continue;

                var relicData = relic.getRelicData(player, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("water_dash");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("water_jump"))
                    continue;

                if (!isOnWaterSurface(player))
                    continue;

                var jumpBonus = Math.max(0D, Math.min(1D, ability.getStatData("jump_bonus").getValue()));

                if (jumpBonus <= 0D)
                    continue;

                relicData.getLevelingData().addExperience("water_dash", "water_jump_boost", 1D);
                ability.getStatisticData().getMetricData("water_jump_boosts").addValue(1D);

                return;
            }
        }

        @SubscribeEvent
        public static void onProjectileImpact(ProjectileImpactEvent event) {
            if (!(event.getRayTraceResult() instanceof EntityHitResult hitResult) || !(hitResult.getEntity() instanceof Player player))
                return;

            var projectile = event.getProjectile();

            if (!isEnemyProjectile(projectile, player))
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.AQUA_DASHERS.value())) {
                if (!(stack.getItem() instanceof AquaDashersItem relic))
                    continue;

                var relicData = relic.getRelicData(player, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("water_dash");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("projectile_phase"))
                    continue;

                if (!isDashActive(player, ability.isRankModifierUnlocked("free_dash")))
                    continue;

                event.setCanceled(true);

                relicData.getLevelingData().addExperience("water_dash", "projectile_phase_ignore", 1D);
                ability.getStatisticData().getMetricData("projectiles_ignored").addValue(1D);
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