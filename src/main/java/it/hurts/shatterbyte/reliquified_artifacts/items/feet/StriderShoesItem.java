package it.hurts.shatterbyte.reliquified_artifacts.items.feet;

import it.hurts.sskirillss.relics.api.events.utility.FluidCollisionEvent;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import top.theillusivec4.curios.api.SlotContext;

public class StriderShoesItem extends RAWearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("lava_stride")
                                .rankModifier(1, "lava_jump")
                                .rankModifier(3, "free_stride")
                                .rankModifier(5, "fire_immunity")
                                .stat(AbilityStatTemplate.builder("speed_penalty")
                                        .initialValue(1D, 0.75D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.0286D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("recovery_time")
                                        .initialValue(10D, 7.5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.0258D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("jump_bonus")
                                        .initialValue(0.15D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0531D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("surface_movement").build())
                                        .source(ExperienceSourceTemplate.builder("lava_jump_boost")
                                                .rankModifierVisibilityState("lava_jump", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("surface_time")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("lava_jump_boosts")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("lava_jump", VisibilityState.OBFUSCATED)
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

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("lava_stride");
        var freeStride = ability.isRankModifierUnlocked("free_stride");
        var jumpBonus = ability.isRankModifierUnlocked("lava_jump") ? Math.max(0D, Math.min(1D, ability.getStatData("jump_bonus").getValue())) : 0D;

        if (!ability.canPlayerUse(player)) {
            resetStrideState(player, stack);
            return;
        }

        if (player.isInLava()) {
            resetStrideState(player, stack);
            return;
        }

        var active = isStrideActive(player, freeStride);
        var onSurface = isOnLavaSurface(player);
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

        resetStrideState(player, stack);
    }

    private void resetStrideState(Player player, ItemStack stack) {
        setRecoveryTicks(stack, 0);
        EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        EntityUtils.removeAttribute(player, stack, Attributes.JUMP_STRENGTH, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        EntityUtils.removeAttribute(player, stack, Attributes.SAFE_FALL_DISTANCE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private int getRecoveryTicks(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(RADataComponent.STRIDER_SHOES_RECOVERY_TICKS.get(), 0));
    }

    private void setRecoveryTicks(ItemStack stack, int ticks) {
        stack.set(RADataComponent.STRIDER_SHOES_RECOVERY_TICKS.get(), Math.max(0, ticks));
    }

    private static boolean isStrideActive(Player player, boolean freeStride) {
        if (player.isSpectator() || player.getAbilities().flying || player.isFallFlying() || player.isInLava())
            return false;

        if (!freeStride && !player.isShiftKeyDown())
            return false;

        return isOnLavaSurface(player);
    }

    private static boolean isOnLavaSurface(Player player) {
        if (player.isInLava())
            return false;

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

                    if (state.getFluidState().is(FluidTags.LAVA))
                        return true;
                }
            }
        }

        return false;
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLevelTickPost(LevelTickEvent.Post event) {
            var level = event.getLevel();

            if (level.isClientSide())
                return;

            for (var player : level.players()) {
                if (!player.isAlive() || player.isSpectator())
                    continue;

                StriderShoesItem activeRelic = null;
                ItemStack activeStack = ItemStack.EMPTY;

                for (var stack : EntityUtils.findEquippedCurios(player, ModItems.STRIDER_SHOES.value())) {
                    if (!(stack.getItem() instanceof StriderShoesItem relic))
                        continue;

                    var ability = relic.getRelicData(player, stack).getAbilitiesData().getAbilityData("lava_stride");

                    if (!ability.canPlayerUse(player))
                        continue;

                    if (!isStrideActive(player, ability.isRankModifierUnlocked("free_stride")))
                        continue;

                    activeRelic = relic;
                    activeStack = stack;
                    break;
                }

                if (activeRelic == null)
                    continue;

                var relicData = activeRelic.getRelicData(player, activeStack);
                var ability = relicData.getAbilitiesData().getAbilityData("lava_stride");

                if (player.tickCount % 20 == 0)
                    ability.getStatisticData().getMetricData("surface_time").addValue(1D);

                if (player.getKnownMovement().multiply(1D, 0D, 1D).length() <= 1.0E-4D)
                    continue;

                var data = player.getPersistentData();
                var movingTicks = Math.max(0, data.getInt("reliquified_artifacts_strider_shoes_surface_moving_ticks")) + 1;

                if (movingTicks >= 100) {
                    var experience = movingTicks / 100;

                    relicData.getLevelingData().addExperience("lava_stride", "surface_movement", experience);
                    movingTicks %= 100;
                }

                data.putInt("reliquified_artifacts_strider_shoes_surface_moving_ticks", movingTicks);
            }
        }

        @SubscribeEvent
        public static void onFluidCollision(FluidCollisionEvent event) {
            LivingEntity entity = event.getEntity();

            if (!(entity instanceof Player player) || player.isInLava())
                return;

            if (!event.getFluid().is(FluidTags.LAVA))
                return;

            for (var stack : EntityUtils.findEquippedCurios(entity, ModItems.STRIDER_SHOES.value())) {
                if (!(stack.getItem() instanceof StriderShoesItem relic))
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

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.STRIDER_SHOES.value())) {
                if (!(stack.getItem() instanceof StriderShoesItem relic))
                    continue;

                var relicData = relic.getRelicData(player, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("lava_stride");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("lava_jump"))
                    continue;

                if (!isOnLavaSurface(player))
                    continue;

                var jumpBonus = Math.max(0D, Math.min(1D, ability.getStatData("jump_bonus").getValue()));

                if (jumpBonus <= 0D)
                    continue;

                relicData.getLevelingData().addExperience("lava_stride", "lava_jump_boost", 1D);
                ability.getStatisticData().getMetricData("lava_jump_boosts").addValue(1D);
                return;
            }
        }

        @SubscribeEvent
        public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide() || event.getAmount() <= 0F)
                return;

            if (!event.getSource().is(DamageTypeTags.IS_FIRE))
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.STRIDER_SHOES.value())) {
                if (!(stack.getItem() instanceof StriderShoesItem relic))
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
