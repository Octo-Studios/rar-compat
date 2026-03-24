package it.hurts.shatterbyte.reliquified_artifacts.items.feet;

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
import it.hurts.sskirillss.relics.init.RelicsMobEffects;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import top.theillusivec4.curios.api.SlotContext;

public class RunningShoesItem extends RAWearableRelicItem {
    @Override
    public RelicTemplate constructDefaultRelicTemplate() {
        return RelicTemplate.builder()
                .abilities(AbilitiesTemplate.builder()
                        .ability(AbilityTemplate.builder("runner")
                                .rankModifier(1, "step_up")
                                .rankModifier(3, "jump_boost")
                                .rankModifier(5, "immortality")
                                .stat(AbilityStatTemplate.builder("max_speed_bonus")
                                        .initialValue(0.08D, 0.35D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0531D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .stat(AbilityStatTemplate.builder("max_step_bonus")
                                        .initialValue(0.15D, 0.45D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.08D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat(AbilityStatTemplate.builder("jump_charge_bonus")
                                        .initialValue(0.05D, 0.1D)
                                        .upgradeModifier(RelicsScalingModels.MULTIPLICATIVE_BASE.get(), 0.0429D)
                                        .formatValue(value -> (int) MathUtils.round(value * 100D, 0))
                                        .build())
                                .experienceSources(ExperienceSourcesTemplate.builder()
                                        .source(ExperienceSourceTemplate.builder("running_time").build())
                                        .source(ExperienceSourceTemplate.builder("running_jump")
                                                .rankModifierVisibilityState("jump_boost", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .statistic(AbilityStatisticTemplate.builder()
                                        .metric(AbilityMetricTemplate.builder("running_time")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("running_jumps")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("jump_boost", VisibilityState.OBFUSCATED)
                                                .build())
                                        .metric(AbilityMetricTemplate.builder("immortality_time")
                                                .formatValue(value -> String.valueOf(Math.max(0, (int) MathUtils.round(value, 0))))
                                                .rankModifierVisibilityState("immortality", VisibilityState.OBFUSCATED)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .loot(LootTemplate.builder()
                        .entry(LootEntries.PLAINS, LootEntries.VILLAGE)
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide())
            return;

        var relicData = this.getRelicData(player, stack);
        var ability = relicData.getAbilitiesData().getAbilityData("runner");

        if (!ability.canPlayerUse(player)) {
            setCharge(stack, 0D);
            EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            EntityUtils.removeAttribute(player, stack, Attributes.STEP_HEIGHT, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            return;
        }

        var charge = getCharge(stack);
        var running = isRunning(player);

        if (running)
            charge = Math.min(1D, charge + 0.025D);
        else
            charge = Math.max(0D, charge - 0.025D);

        setCharge(stack, charge);

        if (running) {
            if (player.tickCount % 20 == 0)
                ability.getStatisticData().getMetricData("running_time").addValue(1D);

            var data = player.getPersistentData();
            var runningTicks = Math.max(0, data.getInt("reliquified_artifacts_running_shoes_running_ticks")) + 1;

            if (runningTicks >= 100) {
                var experience = runningTicks / 100;

                relicData.getLevelingData().addExperience("runner", "running_time", experience);
                runningTicks %= 100;
            }

            data.putInt("reliquified_artifacts_running_shoes_running_ticks", runningTicks);
        }

        var speedBonus = Math.max(0D, ability.getStatData("max_speed_bonus").getValue()) * charge;

        if (speedBonus <= 0D)
            EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        else
            EntityUtils.resetAttribute(player, stack, Attributes.MOVEMENT_SPEED, (float) speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        if (ability.isRankModifierUnlocked("step_up")) {
            var stepBonus = Math.max(0D, ability.getStatData("max_step_bonus").getValue()) * charge;

            if (stepBonus <= 0D)
                EntityUtils.removeAttribute(player, stack, Attributes.STEP_HEIGHT, AttributeModifier.Operation.ADD_VALUE);
            else
                EntityUtils.resetAttribute(player, stack, Attributes.STEP_HEIGHT, (float) stepBonus, AttributeModifier.Operation.ADD_VALUE);
        } else {
            EntityUtils.removeAttribute(player, stack, Attributes.STEP_HEIGHT, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        if (ability.isRankModifierUnlocked("immortality") && running && charge >= 0.999D) {
            player.addEffect(new MobEffectInstance(RelicsMobEffects.IMMORTALITY, 10, 0, false, false));

            if (player.tickCount % 20 == 0)
                ability.getStatisticData().getMetricData("immortality_time").addValue(1D);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);

        if (stack.getItem() == newStack.getItem() || !(slotContext.entity() instanceof Player player))
            return;

        setCharge(stack, 0D);
        EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        EntityUtils.removeAttribute(player, stack, Attributes.STEP_HEIGHT, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private boolean isRunning(Player player) {
        return player.isSprinting() && !player.isFallFlying();
    }

    private double getCharge(ItemStack stack) {
        return Math.max(0D, Math.min(1D, stack.getOrDefault(RADataComponent.RUNNING_SHOES_CHARGE.get(), 0D)));
    }

    private void setCharge(ItemStack stack, double value) {
        stack.set(RADataComponent.RUNNING_SHOES_CHARGE.get(), Math.max(0D, Math.min(1D, value)));
    }

    @EventBusSubscriber(modid = ReliquifiedArtifacts.MODID)
    public static class CommonEvents {
        @SubscribeEvent
        public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide())
                return;

            for (var stack : EntityUtils.findEquippedCurios(player, ModItems.RUNNING_SHOES.value())) {
                if (!(stack.getItem() instanceof RunningShoesItem relic))
                    continue;

                var relicData = relic.getRelicData(player, stack);
                var ability = relicData.getAbilitiesData().getAbilityData("runner");

                if (!ability.canPlayerUse(player) || !ability.isRankModifierUnlocked("jump_boost") || !relic.isRunning(player))
                    continue;

                var bonus = Math.max(0D, Math.min(1D, ability.getStatData("jump_charge_bonus").getValue()));

                if (bonus <= 0D)
                    continue;

                relic.setCharge(stack, Math.min(1D, relic.getCharge(stack) + bonus));
                relicData.getLevelingData().addExperience("runner", "running_jump", 1D);
                ability.getStatisticData().getMetricData("running_jumps").addValue(1D);
            }
        }
    }
}
