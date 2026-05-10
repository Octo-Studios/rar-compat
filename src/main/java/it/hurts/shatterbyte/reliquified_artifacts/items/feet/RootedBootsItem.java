package it.hurts.shatterbyte.reliquified_artifacts.items.feet;

import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootEntries;
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

public class RootedBootsItem extends RAWearableRelicItem {
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
                                        .initialValue(1D, 2D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 5.003D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("saturation_restore")
                                        .initialValue(1D, 2D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 3.001D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("cooldown")
                                        .initialValue(25D, 30D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 5.01D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("restoration_delay")
                                        .initialValue(15D, 20D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 5.02D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("healing")
                                        .initialValue(1D, 2D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 5.003D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("fertilizer_interval")
                                        .initialValue(15D, 20D)
                                        .targetValue(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.96D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("grass_consumed").build())
                                        .source(ExperienceSourceTemplate.builder("fertilizer_application")
                                                .rankModifierVisibilityState("fertilizer", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("grass_consumed")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("healing_done")
                                                .formatValue(value -> String.valueOf(Math.max(0D, MathUtils.round(value, 2))))
                                                .rankModifierVisibilityState("healing", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("fertilizer_applications")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("fertilizer", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.OVERWORLD)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("devouring");

        if (!ability.canPlayerUse(player)) {
            stack.set(RADataComponent.ROOTED_BOOTS_COOLDOWN_TICKS.get(), 0);
            stack.set(RADataComponent.ROOTED_BOOTS_BONEMEAL_TICKS.get(), 0);
            return;
        }

        var cooldownTicks = Math.max(0, stack.getOrDefault(RADataComponent.ROOTED_BOOTS_COOLDOWN_TICKS.get(), 0));

        if (cooldownTicks > 0) {
            cooldownTicks--;
            stack.set(RADataComponent.ROOTED_BOOTS_COOLDOWN_TICKS.get(), cooldownTicks);
        }

        if (ability.getRankModifierData("fertilizer").isEnabled()) {
            var bonemealTicks = Math.max(0, stack.getOrDefault(RADataComponent.ROOTED_BOOTS_BONEMEAL_TICKS.get(), 0));

            if (bonemealTicks > 0) {
                stack.set(RADataComponent.ROOTED_BOOTS_BONEMEAL_TICKS.get(), bonemealTicks - 1);
            } else {
                var intervalTicks = Math.max(1, (int) Math.round(Math.max(0D, ability.getStatData("fertilizer_interval").getValue()) * 20D));

                if (applyBonemealAtFeet(player)) {
                    relicData.getLevelingData().addExperience("devouring", "fertilizer_application", 1D);
                    ability.getStatisticData().getMetricData("fertilizer_applications").addValue(1D);
                }

                stack.set(RADataComponent.ROOTED_BOOTS_BONEMEAL_TICKS.get(), intervalTicks);
            }
        } else {
            stack.set(RADataComponent.ROOTED_BOOTS_BONEMEAL_TICKS.get(), 0);
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

        relicData.getLevelingData().addExperience("devouring", "grass_consumed", 1D);
        ability.getStatisticData().getMetricData("grass_consumed").addValue(1D);

        foodData.setFoodLevel(targetFood);
        foodData.setSaturation(targetSaturation);

        if (ability.getRankModifierData("healing").isEnabled()) {
            var healAmount = Math.max(0D, ability.getStatData("healing").getValue());

            if (healAmount > 0D) {
                var beforeHealth = player.getHealth();

                player.heal((float) healAmount);

                var healed = Math.max(0D, player.getHealth() - beforeHealth);

                if (healed > 0D)
                    ability.getStatisticData().getMetricData("healing_done").addValue(healed);
            }
        }

        if (ability.getRankModifierData("restoration").isEnabled()) {
            var delayTicks = Math.max(0L, Math.round(Math.max(0D, ability.getStatData("restoration_delay").getValue()) * 20D));

            if (delayTicks > 0L) {
                var restoreTick = level.getGameTime() + delayTicks;

                PENDING_GRASS_RESTORE.computeIfAbsent(level.dimension(), key -> new HashMap<>())
                        .put(targetPos.immutable(), restoreTick);
            }
        }

        var newCooldown = Math.max(0, (int) Math.round(Math.max(0D, ability.getStatData("cooldown").getValue() * 20D)));

        stack.set(RADataComponent.ROOTED_BOOTS_COOLDOWN_TICKS.get(), newCooldown);
    }

    private boolean applyBonemealAtFeet(Player player) {
        var level = player.level();
        var targetPos = findBonemealTargetPos(player);

        if (targetPos == null)
            return false;

        if (!BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), level, targetPos, player))
            return false;

        level.levelEvent(1505, targetPos, 15);
        return true;
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

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
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

