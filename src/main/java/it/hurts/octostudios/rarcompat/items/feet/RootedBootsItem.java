package it.hurts.octostudios.rarcompat.items.feet;

import it.hurts.octostudios.rarcompat.RARCompat;
import it.hurts.octostudios.rarcompat.init.DataComponentRegistry;
import it.hurts.octostudios.rarcompat.items.WearableRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import top.theillusivec4.curios.api.SlotContext;

import java.util.HashMap;
import java.util.Map;

public class RootedBootsItem extends WearableRelicItem {
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> PENDING_GRASS_RESTORE = new HashMap<>();

    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("devouring")
                                .rankModifier(1, "restoration")
                                .rankModifier(3, "healing")
                                .rankModifier(5, "fertilizer")
                                .stat(AbilityStatTemplate.builder("hunger_restore")
                                        .thresholdValue(0D, 20D)
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("saturation_restore")
                                        .thresholdValue(0D, 20D)
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("cooldown")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(12D, 5D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.06D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("restoration_delay")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(6D, 20D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("healing")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(1D, 4D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("fertilizer_interval")
                                        .thresholdValue(0D, Double.MAX_VALUE)
                                        .initialValue(20D, 8D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), -0.06D)
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

        var ability = this.getRelicData(player, stack).getAbilitiesData().getAbilityData("devouring");

        if (!ability.canPlayerUse(player)) {
            stack.set(DataComponentRegistry.ROOTED_BOOTS_COOLDOWN_TICKS.get(), 0);
            stack.set(DataComponentRegistry.ROOTED_BOOTS_BONEMEAL_TICKS.get(), 0);
            return;
        }

        var cooldownTicks = Math.max(0, stack.getOrDefault(DataComponentRegistry.ROOTED_BOOTS_COOLDOWN_TICKS.get(), 0));

        if (cooldownTicks > 0) {
            cooldownTicks--;
            stack.set(DataComponentRegistry.ROOTED_BOOTS_COOLDOWN_TICKS.get(), cooldownTicks);
        }

        if (ability.isRankModifierUnlocked("fertilizer")) {
            var bonemealTicks = Math.max(0, stack.getOrDefault(DataComponentRegistry.ROOTED_BOOTS_BONEMEAL_TICKS.get(), 0));

            if (bonemealTicks > 0) {
                stack.set(DataComponentRegistry.ROOTED_BOOTS_BONEMEAL_TICKS.get(), bonemealTicks - 1);
            } else {
                var intervalTicks = Math.max(1, (int) Math.round(Math.max(0D, ability.getStatData("fertilizer_interval").getValue()) * 20D));

                applyBonemealAtFeet(player);
                stack.set(DataComponentRegistry.ROOTED_BOOTS_BONEMEAL_TICKS.get(), intervalTicks);
            }
        } else {
            stack.set(DataComponentRegistry.ROOTED_BOOTS_BONEMEAL_TICKS.get(), 0);
        }

        if (cooldownTicks > 0)
            return;

        var level = player.level();
        var targetPos = player.blockPosition().below();

        if (!level.getBlockState(targetPos).is(Blocks.GRASS_BLOCK))
            return;

        var hungerRestore = Math.max(0, (int) Math.round(Math.max(0D, ability.getStatData("hunger_restore").getValue())));
        var saturationRestore = (float) Math.max(0D, ability.getStatData("saturation_restore").getValue());
        var foodData = player.getFoodData();

        var currentFood = foodData.getFoodLevel();
        var currentSaturation = foodData.getSaturationLevel();
        var targetFood = Math.min(20, currentFood + hungerRestore);
        var targetSaturation = Math.min((float) targetFood, currentSaturation + saturationRestore);

        if (targetFood <= currentFood && targetSaturation <= currentSaturation + 1.0E-4F)
            return;

        level.setBlock(targetPos, Blocks.DIRT.defaultBlockState(), 3);

        foodData.setFoodLevel(targetFood);
        foodData.setSaturation(targetSaturation);

        if (ability.isRankModifierUnlocked("healing")) {
            var healAmount = Math.max(0D, ability.getStatData("healing").getValue());

            if (healAmount > 0D)
                player.heal((float) healAmount);
        }

        if (ability.isRankModifierUnlocked("restoration")) {
            var delayTicks = Math.max(0L, Math.round(Math.max(0D, ability.getStatData("restoration_delay").getValue()) * 20D));

            if (delayTicks > 0L) {
                var restoreTick = level.getGameTime() + delayTicks;

                PENDING_GRASS_RESTORE.computeIfAbsent(level.dimension(), key -> new HashMap<>())
                        .put(targetPos.immutable(), restoreTick);
            }
        }

        var newCooldown = Math.max(0, (int) Math.round(Math.max(0D, ability.getStatData("cooldown").getValue() * 20D)));

        stack.set(DataComponentRegistry.ROOTED_BOOTS_COOLDOWN_TICKS.get(), newCooldown);
    }

    private void applyBonemealAtFeet(Player player) {
        var level = player.level();
        var targetPos = findBonemealTargetPos(player);

        if (targetPos == null)
            return;

        if (!BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), level, targetPos, player))
            return;

        level.levelEvent(1505, targetPos, 15);
    }

    private BlockPos findBonemealTargetPos(Player player) {
        var level = player.level();
        var basePos = player.blockPosition();
        var minY = Mth.floor(player.getBoundingBox().minY);
        var maxY = Mth.floor(player.getBoundingBox().maxY) + 1;

        for (var y = minY; y <= maxY; y++) {
            var candidate = new BlockPos(basePos.getX(), y, basePos.getZ());
            var state = level.getBlockState(candidate);

            if (state.getBlock() instanceof BonemealableBlock bonemealable
                    && bonemealable.isValidBonemealTarget(level, candidate, state))
                return candidate;
        }

        return null;
    }

    @EventBusSubscriber(modid = RARCompat.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLevelTickPost(LevelTickEvent.Post event) {
            if (!(event.getLevel() instanceof ServerLevel level))
                return;

            var queued = PENDING_GRASS_RESTORE.get(level.dimension());

            if (queued == null || queued.isEmpty())
                return;

            var now = level.getGameTime();
            var iterator = queued.entrySet().iterator();

            while (iterator.hasNext()) {
                var entry = iterator.next();

                if (entry.getValue() > now)
                    continue;

                iterator.remove();

                var pos = entry.getKey();

                if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(Blocks.DIRT))
                    continue;

                level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }

            if (queued.isEmpty())
                PENDING_GRASS_RESTORE.remove(level.dimension());
        }
    }
}
